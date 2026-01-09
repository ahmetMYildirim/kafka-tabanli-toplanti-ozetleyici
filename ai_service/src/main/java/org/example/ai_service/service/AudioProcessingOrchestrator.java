package org.example.ai_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai_service.domain.model.AudioEvent;
import org.example.ai_service.domain.model.ExtractedTask;
import org.example.ai_service.domain.model.MeetingSummary;
import org.example.ai_service.domain.model.TranscriptionResult;
import org.example.ai_service.entity.AudioMessageEntity;
import org.example.ai_service.entity.MeetingEntity;
import org.example.ai_service.entity.MeetingSummaryEntity;
import org.example.ai_service.entity.TaskEntity;
import org.example.ai_service.entity.TranscriptionEntity;
import org.example.ai_service.producer.ActionItemProducer;
import org.example.ai_service.producer.SummaryProducer;
import org.example.ai_service.producer.TranscriptionProducer;
import org.example.ai_service.repository.AudioMessageRepository;
import org.example.ai_service.repository.MeetingRepository;
import org.example.ai_service.repository.MeetingSummaryRepository;
import org.example.ai_service.repository.TaskRepository;
import org.example.ai_service.repository.TranscriptionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioProcessingOrchestrator {

    private final TranscriptionService transcriptionService;
    private final TaskExtractionService taskExtractionService;
    private final MeetingSummrayService summaryService;
    private final TranscriptionProducer transcriptionProducer;
    private final ActionItemProducer actionItemProducer;
    private final SummaryProducer summaryProducer;
    private final AudioMessageRepository audioMessageRepository;
    private final MeetingRepository meetingRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final TaskRepository taskRepository;

    @Async
    @Transactional
    public void processAudioEvent(AudioEvent audioEvent) {
        if (audioEvent == null) {
            log.warn("AudioEvent is null, skipping processing");
            return;
        }

        log.info("Starting audio processing: meetingId={}", audioEvent.getMeetingId());

        try {
            TranscriptionResult transcription = transcriptionService.transcribe(audioEvent);

            if (transcription == null) {
                log.warn("Transcription failed or returned null, skipping further processing: meetingId={}, audioUrl={}",
                        audioEvent.getMeetingId(), audioEvent.getAudioUrl());
                return;
            }

            ExtractedTask tasks = null;
            MeetingSummary summary = null;

            try {
                tasks = taskExtractionService.extractedTask(transcription);
                actionItemProducer.send(tasks);
            } catch (Exception e) {
                log.error("Failed to extract or send tasks: meetingId={}", audioEvent.getMeetingId(), e);
            }

            try {
                summary = summaryService.generateSummary(transcription);
                summaryProducer.send(summary);
            } catch (Exception e) {
                log.error("Failed to generate or send summary: meetingId={}", audioEvent.getMeetingId(), e);
            }

            try {
                transcriptionProducer.send(transcription);
            } catch (Exception e) {
                log.error("Failed to send transcription to Kafka: meetingId={}", audioEvent.getMeetingId(), e);
            }

            // Tüm AI sonuçlarını MySQL'e kaydet
            saveMeetingDataToDatabase(audioEvent, transcription, summary, tasks);

            log.info("Audio processing completed successfully: meetingId={}", audioEvent.getMeetingId());

        } catch (Exception e) {
            log.error("Audio processing failed: meetingId={}", audioEvent.getMeetingId(), e);
        }
    }

    /**
     * Transkripsiyon, özet ve görevleri Meeting/Transcription/Summary/Task tablolarına yazar.
     */
    private void saveMeetingDataToDatabase(AudioEvent audioEvent,
                                           TranscriptionResult transcription,
                                           MeetingSummary summary,
                                           ExtractedTask tasks) {
        if (audioEvent == null || transcription == null) {
            log.warn("Cannot persist meeting data: audioEvent or transcription is null");
            return;
        }

        String fullTranscription = transcription.getFullTranscription();
        if (fullTranscription == null || fullTranscription.trim().isEmpty()) {
            log.warn("Cannot persist meeting data: fullTranscription is empty for meetingId={}", audioEvent.getMeetingId());
            return;
        }

        // 1) Meeting bul/oluştur (external_id = audioEvent.meetingId)
        String externalMeetingId = audioEvent.getMeetingId() != null
                ? audioEvent.getMeetingId()
                : UUID.randomUUID().toString();

        MeetingEntity meeting = meetingRepository.findByExternalId(externalMeetingId)
                .orElseGet(() -> {
                    MeetingEntity created = MeetingEntity.builder()
                            .externalId(externalMeetingId)
                            .title(summary != null && summary.getTitle() != null && !summary.getTitle().isEmpty()
                                    ? summary.getTitle()
                                    : "Meeting - " + externalMeetingId)
                            .description(summary != null ? summary.getSummary() : null)
                            .platform(audioEvent.getPlatform() != null ? audioEvent.getPlatform() : "UNKNOWN")
                            .channelId(audioEvent.getChannelId())
                            .status("COMPLETED")
                            .actualStart(audioEvent.getTimestamp() != null ? audioEvent.getTimestamp() : LocalDateTime.now())
                            .actualEnd(LocalDateTime.now())
                            .durationSeconds(transcription.getDurationSeconds() != null
                                    ? transcription.getDurationSeconds().intValue()
                                    : null)
                            .build();
                    MeetingEntity saved = meetingRepository.save(created);
                    log.info("Meeting created in DB: id={}, externalId={}", saved.getId(), saved.getExternalId());
                    return saved;
                });

        Long meetingId = meeting.getId();

        // 2) AudioMessage güncelle ve toplantıya bağla
        Long audioMessageId = null;
        String audioUrl = audioEvent.getAudioUrl();
        if (audioUrl != null && !audioUrl.trim().isEmpty()) {
            Optional<AudioMessageEntity> existingMessage = audioMessageRepository.findByAudioUrl(audioUrl);

            AudioMessageEntity message = existingMessage.orElseGet(() -> AudioMessageEntity.builder()
                    .platform(audioEvent.getPlatform())
                    .channelId(audioEvent.getChannelId())
                    .author(audioEvent.getAuthor())
                    .audioUrl(audioUrl)
                    .timestamp(audioEvent.getTimestamp())
                    .voiceSessionId(audioEvent.getVoiceSessionId())
                    .build());

            message.setMeetingId(meetingId);
            message.setTranscription(fullTranscription);
            message.setTranscriptionStatus("COMPLETED");
            message.setProcessedAt(LocalDateTime.now());

            AudioMessageEntity savedMessage = audioMessageRepository.save(message);
            audioMessageId = savedMessage.getId();
            log.info("Audio message linked to meeting: audioMessageId={}, meetingId={}", audioMessageId, meetingId);
        }

        // 3) TranscriptionEntity oluştur
        TranscriptionEntity transcriptionEntity = TranscriptionEntity.builder()
                .audioMessageId(audioMessageId)
                .meetingId(meetingId)
                .fullText(fullTranscription)
                .language("tr")
                .confidenceScore(transcription.getConfidence())
                .wordCount(fullTranscription.split("\\s+").length)
                .processingTimeMs(null)
                .aiModel("whisper-1")
                .build();
        transcriptionEntity = transcriptionRepository.save(transcriptionEntity);
        log.info("Transcription saved to DB: id={}, meetingId={}", transcriptionEntity.getId(), meetingId);

        // 4) MeetingSummaryEntity oluştur
        if (summary != null) {
            String keyPoints = summary.getKeyPoints() != null
                    ? String.join("||", summary.getKeyPoints())
                    : null;
            String decisions = summary.getDecisions() != null
                    ? String.join("||", summary.getDecisions())
                    : null;
            String participants = summary.getParticipants() != null
                    ? String.join("||", summary.getParticipants())
                    : null;

            LocalDateTime processedTime = summary.getProcessedTime() != null
                    ? LocalDateTime.ofInstant(summary.getProcessedTime(), ZoneId.systemDefault())
                    : LocalDateTime.now();

            LocalDateTime meetingDate = summary.getMeetingDate() != null
                    ? LocalDateTime.ofInstant(summary.getMeetingDate(), ZoneId.systemDefault())
                    : processedTime;

            MeetingSummaryEntity summaryEntity = MeetingSummaryEntity.builder()
                    .meetingId(meetingId)
                    .channelId(summary.getChannelId())
                    .platform(summary.getPlatform())
                    .title(summary.getTitle())
                    .summary(summary.getSummary())
                    .keyPoints(keyPoints)
                    .decisions(decisions)
                    .participants(participants)
                    .durationMinutes(summary.getDurationMinutes())
                    .meetingDate(meetingDate)
                    .processedTime(processedTime)
                    .build();
            MeetingSummaryEntity savedSummary = meetingSummaryRepository.save(summaryEntity);
            log.info("Meeting summary saved to DB: id={}, meetingId={}", savedSummary.getId(), meetingId);
        } else {
            log.warn("Summary is null, skipping MeetingSummaryEntity persistence for meetingId={}", meetingId);
        }

        // 5) Görevleri TaskEntity olarak kaydet
        if (tasks != null && tasks.getTaskItems() != null && !tasks.getTaskItems().isEmpty()) {
            List<ExtractedTask.TaskItem> items = tasks.getTaskItems();
            for (ExtractedTask.TaskItem item : items) {
                TaskEntity.Priority priority = TaskEntity.Priority.MEDIUM;
                if (item.getPriority() != null) {
                    try {
                        priority = TaskEntity.Priority.valueOf(item.getPriority().name());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                TaskEntity.Status status = TaskEntity.Status.PENDING;
                if (item.getStatus() != null) {
                    try {
                        status = TaskEntity.Status.valueOf(item.getStatus().name());
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                LocalDateTime dueDate = null;
                if (item.getDueDate() != null) {
                    dueDate = LocalDateTime.ofInstant(item.getDueDate(), ZoneId.systemDefault());
                }

                TaskEntity taskEntity = TaskEntity.builder()
                        .taskId(item.getId() != null ? item.getId() : UUID.randomUUID().toString())
                        .meetingId(meetingId)
                        .transcriptionId(transcriptionEntity.getId())
                        .channelId(tasks.getChannelId())
                        .platform(tasks.getPlatform())
                        .title(item.getTitle() != null
                                ? (item.getTitle().length() > 150
                                ? item.getTitle().substring(0, 150)
                                : item.getTitle())
                                : "Action Item")
                        .description(item.getDescription())
                        .assignee(item.getAssignee())
                        .assignedToName(item.getAssignee())
                        .assigneeId(item.getAssigneeId())
                        .priority(priority)
                        .status(status)
                        .dueDate(dueDate)
                        .sourceText(item.getSourceText())
                        .confidenceScore(item.getConfidenceScore())
                        .assignmentReason(item.getAssignmentReason())
                        .processedTime(LocalDateTime.now())
                        .build();

                taskRepository.save(taskEntity);
            }
            log.info("Saved {} tasks to DB for meetingId={}", tasks.getTaskItems().size(), meetingId);
        } else {
            log.info("No tasks extracted for meetingId={}, skipping TaskEntity persistence", meetingId);
        }
    }
}
