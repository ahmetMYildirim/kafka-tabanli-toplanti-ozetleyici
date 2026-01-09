package org.example.ai_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai_service.entity.*;
import org.example.ai_service.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Meeting Data Controller - MySQL'den toplanti verilerini getir
 * 
 * Gateway API bu endpoint'leri kullanarak UI'a veri sağlar
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MeetingDataController {

    private final MeetingRepository meetingRepository;
    private final AudioMessageRepository audioMessageRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final TaskRepository taskRepository;

    /**
     * Tüm toplantıları listele (meetings tablosundan)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMeetings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting all meetings: page={}, size={}", page, size);
        
        List<MeetingEntity> meetings = meetingRepository.findAll();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", meetings);
        response.put("total", meetings.size());
        response.put("page", page);
        response.put("size", size);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Toplantı detayını getir (external_id ile) - özet + transkripsiyon + görevler
     */
    @GetMapping("/{externalId}")
    public ResponseEntity<Map<String, Object>> getMeetingDetail(@PathVariable String externalId) {
        log.info("Getting meeting detail by externalId: {}", externalId);
        
        Optional<MeetingEntity> meetingOpt = meetingRepository.findByExternalId(externalId);
        
        if (meetingOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Meeting not found: " + externalId);
            return ResponseEntity.status(404).body(error);
        }
        
        MeetingEntity meeting = meetingOpt.get();
        Long meetingId = meeting.getId();
        
        Optional<MeetingSummaryEntity> summaryOpt = meetingSummaryRepository.findByMeetingId(meetingId);
        List<AudioMessageEntity> audioMessages = audioMessageRepository.findByMeetingId(meetingId);
        List<TranscriptionEntity> transcriptions = transcriptionRepository.findByMeetingId(meetingId);
        List<TaskEntity> tasks = taskRepository.findByMeetingId(meetingId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("meeting", meeting);
        response.put("externalId", externalId);
        response.put("summary", summaryOpt.orElse(null));
        response.put("transcription", transcriptions.isEmpty() ? null : transcriptions.get(0).getFullText());
        response.put("transcriptions", transcriptions);
        response.put("audioMessages", audioMessages);
        response.put("tasks", tasks);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Toplantı özetini getir (by numeric ID)
     */
    @GetMapping("/by-id/{meetingId}/summary")
    public ResponseEntity<Map<String, Object>> getMeetingSummary(@PathVariable Long meetingId) {
        log.info("Getting meeting summary by ID: meetingId={}", meetingId);
        
        Optional<MeetingSummaryEntity> summaryOpt = meetingSummaryRepository.findByMeetingId(meetingId);
        
        if (summaryOpt.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Summary not found for meetingId: " + meetingId);
            return ResponseEntity.status(404).body(error);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", summaryOpt.get());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Toplantı transkripsiyonunu getir (by numeric ID)
     */
    @GetMapping("/by-id/{meetingId}/transcription")
    public ResponseEntity<Map<String, Object>> getMeetingTranscription(@PathVariable Long meetingId) {
        log.info("Getting meeting transcription by ID: meetingId={}", meetingId);
        
        List<TranscriptionEntity> transcriptions = transcriptionRepository.findByMeetingId(meetingId);
        
        if (transcriptions.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Transcription not found for meetingId: " + meetingId);
            return ResponseEntity.status(404).body(error);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", transcriptions);
        response.put("transcription", transcriptions.get(0).getFullText());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Toplantı görevlerini getir (by numeric ID)
     */
    @GetMapping("/by-id/{meetingId}/tasks")
    public ResponseEntity<Map<String, Object>> getMeetingTasks(@PathVariable Long meetingId) {
        log.info("Getting meeting tasks by ID: meetingId={}", meetingId);
        
        List<TaskEntity> tasks = taskRepository.findByMeetingId(meetingId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tasks);
        response.put("count", tasks.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Tüm görevleri getir
     */
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getAllTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        
        log.info("Getting all tasks: status={}, priority={}", status, priority);
        
        List<TaskEntity> tasks;
        
        if (status != null && !status.isEmpty()) {
            try {
                TaskEntity.Status statusEnum = TaskEntity.Status.valueOf(status.toUpperCase());
                tasks = taskRepository.findByStatus(statusEnum);
            } catch (IllegalArgumentException e) {
                tasks = taskRepository.findAll();
            }
        } else if (priority != null && !priority.isEmpty()) {
            try {
                TaskEntity.Priority priorityEnum = TaskEntity.Priority.valueOf(priority.toUpperCase());
                tasks = taskRepository.findByPriority(priorityEnum);
            } catch (IllegalArgumentException e) {
                tasks = taskRepository.findAll();
            }
        } else {
            tasks = taskRepository.findAll();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tasks);
        response.put("count", tasks.size());
        
        return ResponseEntity.ok(response);
    }

    /**
     * İstatistikler
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Getting statistics");
        
        long totalMeetings = meetingSummaryRepository.count();
        long totalTasks = taskRepository.count();
        long totalAudioMessages = audioMessageRepository.count();
        
        List<TaskEntity> pendingTasks = taskRepository.findByStatus(TaskEntity.Status.PENDING);
        List<TaskEntity> completedTasks = taskRepository.findByStatus(TaskEntity.Status.COMPLETED);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMeetings", totalMeetings);
        stats.put("totalTasks", totalTasks);
        stats.put("pendingTasks", pendingTasks.size());
        stats.put("completedTasks", completedTasks.size());
        stats.put("totalAudioMessages", totalAudioMessages);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", stats);
        
        return ResponseEntity.ok(response);
    }
}

