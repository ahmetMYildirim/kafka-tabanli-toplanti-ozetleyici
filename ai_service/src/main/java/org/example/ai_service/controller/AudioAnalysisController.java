package org.example.ai_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai_service.client.OpenAIClient;
import org.example.ai_service.entity.*;
import org.example.ai_service.repository.*;
import org.example.ai_service.util.AudioCompressor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/v1/audio")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AudioAnalysisController {

    private final OpenAIClient openAIClient;
    private final AudioCompressor audioCompressor;
    private final MeetingRepository meetingRepository;
    private final AudioMessageRepository audioMessageRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;

    /**
     * Ses dosyasını yükle ve analiz et
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> analyzeAudio(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "meetingId", required = false) String meetingId,
            @RequestParam(value = "meetingTitle", required = false) String meetingTitle) {

        log.info("Audio analysis request received: filename={}, size={} bytes",
                file.getOriginalFilename(), file.getSize());

        try {
            
            if (file.isEmpty() || file.getSize() == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Audio file is empty");
                return ResponseEntity.badRequest().body(error);
            }
            
            
            String tempDir = System.getProperty("java.io.tmpdir");
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path tempFile = Path.of(tempDir, fileName);
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("Audio file saved temporarily: {}", tempFile);

            
            File audioFile = tempFile.toFile();
            File processedFile = audioCompressor.compressIfNeeded(audioFile);
            
            if (!processedFile.equals(audioFile)) {
                log.info("Audio compressed: {} MB → {} MB",
                        audioCompressor.getFileSizeInMB(audioFile),
                        audioCompressor.getFileSizeInMB(processedFile));
            }

            
            String transcription = openAIClient.transcribeAudio(processedFile.getAbsolutePath());
            log.info("Transcription completed, length: {} chars", transcription.length());

            
            String summary = openAIClient.generateSummary(transcription);
            log.info("Summary generated");

            
            String tasks = openAIClient.extractTasks(transcription, List.of());
            log.info("Tasks extracted");

            
            Files.deleteIfExists(tempFile);
            if (!processedFile.equals(audioFile)) {
                processedFile.delete();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("meetingId", meetingId != null ? meetingId : UUID.randomUUID().toString());
            result.put("meetingTitle", meetingTitle != null ? meetingTitle : "Uploaded Meeting");
            result.put("transcription", transcription);
            result.put("summary", summary);
            result.put("actionItems", tasks);
            result.put("processedAt", LocalDateTime.now().toString());
            result.put("audioFileName", file.getOriginalFilename());
            result.put("audioFileSize", file.getSize());

            return ResponseEntity.ok(result);

        } catch (IOException e) {
            log.error("Error processing audio file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Dosya yolundan ses analizi yap
     */
    @PostMapping("/analyze-path")
    public ResponseEntity<Map<String, Object>> analyzeAudioFromPath(
            @RequestParam("filePath") String filePath,
            @RequestParam(value = "meetingId", required = false) String meetingId,
            @RequestParam(value = "meetingTitle", required = false) String meetingTitle) {

        if (filePath == null || filePath.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Audio file path is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        if (meetingTitle == null || meetingTitle.trim().isEmpty()) {
            meetingTitle = "Meeting " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        }
        
        log.info("Audio analysis from path: {}", filePath);

        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "File not found: " + filePath);
            return ResponseEntity.badRequest().body(error);
        }

        try {
            log.info("Starting transcription for file: {} (size: {} bytes)",
                    audioFile.getName(), audioFile.length());

            
            File processedFile = audioCompressor.compressIfNeeded(audioFile);
            
            if (!processedFile.equals(audioFile)) {
                log.info("Audio compressed: {} MB → {} MB",
                        audioCompressor.getFileSizeInMB(audioFile),
                        audioCompressor.getFileSizeInMB(processedFile));
            }

            
            String transcription = openAIClient.transcribeAudio(processedFile.getAbsolutePath());
            log.info("Transcription completed, length: {} chars", transcription.length());

            
            String summary = openAIClient.generateSummary(transcription);
            log.info("Summary generated");

            
            String tasks = openAIClient.extractTasks(transcription, List.of());
            log.info("Tasks extracted");

            String finalMeetingId = meetingId != null ? meetingId : UUID.randomUUID().toString();
            String finalMeetingTitle = meetingTitle != null ? meetingTitle : audioFile.getName();

            saveToDatabase(finalMeetingId, finalMeetingTitle, filePath, transcription, summary, tasks, audioFile.length());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("meetingId", finalMeetingId);
            result.put("meetingTitle", finalMeetingTitle);
            result.put("transcription", transcription);
            result.put("summary", summary);
            result.put("actionItems", tasks);
            result.put("processedAt", LocalDateTime.now().toString());
            result.put("audioFileName", audioFile.getName());
            result.put("audioFileSize", audioFile.length());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error processing audio file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "openai-whisper-audio-analysis");
        response.put("provider", "OpenAI Whisper + ChatGPT");
        response.put("features", "Auto-compression, 25MB limit, MySQL storage");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * MySQL'e DOGRU SIRALAMAYLA kaydetme
     * 1. meetings (ana tablo)
     * 2. audio_messages
     * 3. transcriptions
     * 4. meeting_summaries
     * 5. tasks
     */
    private void saveToDatabase(String externalMeetingId, String meetingTitle, String audioFilePath, 
                                String transcription, String summary, String tasks, long fileSize) {
        try {
            log.info("Saving to database with correct order: externalId={}", externalMeetingId);

            MeetingEntity meeting = MeetingEntity.builder()
                    .externalId(externalMeetingId)
                    .title(meetingTitle)
                    .platform("MANUAL_UPLOAD")
                    .channelId("direct-upload")
                    .status("COMPLETED")
                    .actualStart(LocalDateTime.now())
                    .actualEnd(LocalDateTime.now())
                    .build();
            meeting = meetingRepository.save(meeting);
            log.info("[1/5] Meeting saved: id={}, externalId={}", meeting.getId(), meeting.getExternalId());

            AudioMessageEntity audioMessage = AudioMessageEntity.builder()
                    .meetingId(meeting.getId())
                    .platform("MANUAL_UPLOAD")
                    .channelId("direct-upload")
                    .author("System")
                    .audioUrl(audioFilePath)
                    .audioFilePath(audioFilePath)
                    .fileSizeBytes(fileSize)
                    .mimeType("audio/wav")
                    .transcription(transcription)
                    .transcriptionStatus("COMPLETED")
                    .timestamp(LocalDateTime.now())
                    .processedAt(LocalDateTime.now())
                    .build();
            audioMessage = audioMessageRepository.save(audioMessage);
            log.info("[2/5] Audio message saved: id={}", audioMessage.getId());

            TranscriptionEntity transcriptionEntity = TranscriptionEntity.builder()
                    .audioMessageId(audioMessage.getId())
                    .meetingId(meeting.getId())
                    .fullText(transcription)
                    .language("en")
                    .wordCount(transcription.split("\\s+").length)
                    .aiModel("whisper-1")
                    .build();
            transcriptionEntity = transcriptionRepository.save(transcriptionEntity);
            log.info("[3/5] Transcription saved: id={}", transcriptionEntity.getId());

            MeetingSummaryEntity summaryEntity = MeetingSummaryEntity.builder()
                    .meetingId(meeting.getId())
                    .channelId("direct-upload")
                    .platform("MANUAL_UPLOAD")
                    .title(meetingTitle)
                    .summary(summary)
                    .processedTime(LocalDateTime.now())
                    .meetingDate(LocalDateTime.now())
                    .build();
            summaryEntity = meetingSummaryRepository.save(summaryEntity);
            log.info("[4/5] Meeting summary saved: id={}", summaryEntity.getId());

            List<TaskEntity> taskEntities = parseTasksAndSave(meeting.getId(), transcriptionEntity.getId(), tasks);
            log.info("[5/5] Saved {} tasks to database", taskEntities.size());

            log.info("All data successfully saved for meeting: {}", meeting.getExternalId());

        } catch (Exception e) {
            log.error("Error saving to database: externalId={}", externalMeetingId, e);
        }
    }

    /**
     * Task string'ini parse edip TaskEntity listesi oluştur
     */
    private List<TaskEntity> parseTasksAndSave(Long meetingId, Long transcriptionId, String tasksString) {
        List<TaskEntity> taskEntities = new ArrayList<>();
        
        try {
            Pattern pattern = Pattern.compile("(\\d+)\\.\\s*\\*\\*Task description:\\*\\*\\s*([^\\n]+)\\s*\\*\\*Responsible person:\\*\\*\\s*([^\\n]+)\\s*\\*\\*Deadline:\\*\\*\\s*([^\\n]+)", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(tasksString);
            
            while (matcher.find()) {
                String description = matcher.group(2).trim();
                String responsible = matcher.group(3).trim();
                String deadline = matcher.group(4).trim();
                
                TaskEntity task = TaskEntity.builder()
                        .taskId(UUID.randomUUID().toString())
                        .meetingId(meetingId)
                        .transcriptionId(transcriptionId)
                        .channelId("direct-upload")
                        .platform("MANUAL_UPLOAD")
                        .title(description.length() > 100 ? description.substring(0, 100) : description)
                        .description(description)
                        .assignee(responsible)
                        .assignedToName(responsible)
                        .priority(TaskEntity.Priority.MEDIUM)
                        .status(TaskEntity.Status.PENDING)
                        .processedTime(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .build();
                
                taskRepository.save(task);
                taskEntities.add(task);
            }
        } catch (Exception e) {
            log.error("Error parsing tasks", e);
        }
        
        return taskEntities;
    }
}

