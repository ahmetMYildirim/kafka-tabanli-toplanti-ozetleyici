package controller;

import cache.dataStore;
import entity.*;
import repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.dto.ApiResponse;
import model.dto.MeetingDetailDTO;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import model.event.ProcessedActionItem;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MeetingController - Toplantı verilerine erişim sağlayan REST API kontrolcüsü
 *
 * Bu kontrolcü, Discord ve Zoom platformlarından toplanan ve AI servisi
 * tarafından işlenen toplantı verilerine erişim sağlar.
 *
 * Sunulan Özellikler:
 * - Toplantı özetlerini listeleme ve filtreleme (platform bazlı)
 * - Toplantı transkriptlerine erişim (speech-to-text sonuçları)
 * - AI tarafından belirlenen görevleri görüntüleme (action items)
 * - Platform bazlı filtreleme (Discord/Zoom/Teams)
 * - Pagination desteği (limit parametresi ile)
 *
 * Veri Kaynağı (HYBRID):
 * - MySQL database (persistent storage)
 * - In-memory cache (dataStore) - Kafka real-time events
 * - Önce MySQL'den çeker, sonra cache ile birleştirir
 *
 * Güvenlik:
 * - JWT token ile korunmaktadır
 * - JwtAuthFilter otomatik devreye girer
 * - Rate limiting uygulanır (RateLimitFilter)
 *
 * @author Ahmet
 * @version 1.0
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/meetings")
@Tag(name = "Meeting_Management", description = "Discord and Zoom meeting data access endpoints")
public class MeetingController {

    private final dataStore dataStore;
    private final MeetingRepository meetingRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final MeetingSummaryRepository summaryRepository;
    private final TaskRepository taskRepository;

    /**
     * HYBRID: MySQL + Kafka Cache
     * Tüm toplantı özetlerini listeler (önce MySQL, sonra cache ile merge)
     */
    @GetMapping
    @Operation(summary = "List all meeting summaries - HYBRID (MySQL + Kafka)")
    public ResponseEntity<ApiResponse<List<ProcessedSummary>>> getAllMeetings(
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "20") int limit) {

        log.info("[HYBRID] Meeting list request. Platform: {}, Limit: {}", platform, limit);

        
        List<ProcessedSummary> mysqlSummaries = fetchFromMySQL(platform, limit);
        log.debug("[MySQL] Found {} meetings", mysqlSummaries.size());

        
        List<ProcessedSummary> cacheSummaries;
        if (platform != null && !platform.trim().isEmpty()) {
            cacheSummaries = dataStore.getSummariesByPlatform(platform);
        } else {
            cacheSummaries = dataStore.getLastSummaries(limit);
        }
        log.debug("[Kafka Cache] Found {} meetings", cacheSummaries.size());

        
        Set<String> existingIds = mysqlSummaries.stream()
                .map(ProcessedSummary::getMeetingId)
                .collect(Collectors.toSet());

        cacheSummaries.stream()
                .filter(s -> !existingIds.contains(s.getMeetingId()))
                .forEach(mysqlSummaries::add);

        log.info("[HYBRID] Total {} meetings returned", mysqlSummaries.size());
        return ResponseEntity.ok(ApiResponse.success(mysqlSummaries));
    }

    /**
     * MySQL'den meeting summaries çeker
     */
    private List<ProcessedSummary> fetchFromMySQL(String platform, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        List<MeetingEntity> meetings;
        if (platform != null && !platform.trim().isEmpty()) {
            meetings = meetingRepository.findByPlatform(platform, pageable);
        } else {
            meetings = meetingRepository.findLatestMeetings(pageable);
        }

        return meetings.stream()
                .map(this::convertToProcessedSummary)
                .collect(Collectors.toList());
    }

    /**
     * MeetingEntity'yi ProcessedSummary'ye dönüştürür
     */
    private ProcessedSummary convertToProcessedSummary(MeetingEntity meeting) {
        MeetingSummaryEntity summary = summaryRepository.findByMeetingId(meeting.getId()).orElse(null);

        ProcessedSummary ps = new ProcessedSummary();
        ps.setMeetingId(meeting.getExternalId());
        ps.setPlatform(meeting.getPlatform());
        ps.setChannelId(meeting.getChannelId());

        if (summary != null) {
            ps.setSummary(summary.getSummary());
            ps.setTitle(summary.getTitle());
            if (summary.getKeyPoints() != null) {
                ps.setKeyPoints(Arrays.asList(summary.getKeyPoints().split(",")));
            }
            if (summary.getProcessedTime() != null) {
                ps.setProcessedTime(summary.getProcessedTime().atZone(ZoneId.systemDefault()).toInstant());
            }
        }

        return ps;
    }

    /**
     * Belirtilen toplantı kimliğine ait tüm detayları getirir.
     *
     * Özet (summary), transkript (transcription) ve görev listesini (action items)
     * tek bir istekte döndürür. Bu sayede client-side'da tek bir API çağrısı ile
     * tüm meeting detaylarına erişilir.
     *
     * MeetingDetailDTO içeriği:
     * - ProcessedSummary: AI özeti, ana konular, kararlar
     * - ProcessedTranscription: Tam transkript metni, konuşmacılar
     * - ProcessedActionItem: Görev listesi, sorumlular, öncelikler
     *
     * @param meetingId Aranan toplantının benzersiz ID'si (meeting external ID)
     * @return Toplantı detay bilgileri veya 404 Not Found
     */
    @GetMapping("/{meetingId}")
    @Operation(summary = "Get meeting details - Summary, transcription and action items included")
    public ResponseEntity<ApiResponse<MeetingDetailDTO>> getMeetingDetail(
            @PathVariable String meetingId) {

        log.info("Meeting detail request. Meeting ID: {}", meetingId);

        Optional<ProcessedSummary> summary = dataStore.getSummary(meetingId);

        if (summary.isEmpty()) {
            log.warn("Meeting not found. ID: {}", meetingId);
            return ResponseEntity.notFound().build();
        }

        Optional<ProcessedTranscription> transcription = dataStore.getTranscription(meetingId);
        Optional<ProcessedActionItem> actionItem = dataStore.getActionItem(meetingId);

        MeetingDetailDTO detail = MeetingDetailDTO.builder()
                .processedSummary(summary.orElse(null))
                .processedTranscription(transcription.orElse(null))
                .processedActionItem(actionItem.orElse(null))
                .build();

        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * Belirtilen toplantının AI tarafından oluşturulan özetini getirir.
     *
     * Özet, OpenAI ChatGPT tarafından üretilir ve şunları içerir:
     * - Ana konular (main topics)
     * - Alınan kararlar (decisions)
     * - Öne çıkan noktalar (key highlights)
     * - Toplantı süresi ve katılımcı bilgileri
     *
     * @param meetingId Aranan toplantının benzersiz ID'si
     * @return Toplantı özeti (ProcessedSummary) veya 404 Not Found
     */
    @GetMapping("/{meetingId}/summary")
    @Operation(summary = "Get meeting AI summary - Main topics and decisions")
    public ResponseEntity<ApiResponse<ProcessedSummary>> getMeetingSummary(
            @PathVariable String meetingId) {

        log.debug("Meeting summary request. ID: {}", meetingId);

        return dataStore.getSummary(meetingId)
                .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Belirtilen toplantının ses-metin dönüşümü (speech-to-text) sonucunu getirir.
     *
     * Transkript, OpenAI Whisper API ile oluşturulur ve şunları içerir:
     * - Tam metin transkripti (full text)
     * - Konuşmacı bilgileri (speaker identification)
     * - Zaman damgaları (timestamps)
     * - Güven skoru (confidence score)
     *
     * @param meetingId Aranan toplantının benzersiz ID'si
     * @return Toplantı transkripti (ProcessedTranscription) veya 404 Not Found
     */
    @GetMapping("/{meetingId}/transcription")
    @Operation(summary = "Get meeting transcription - Speech-to-text result")
    public ResponseEntity<ApiResponse<ProcessedTranscription>> getMeetingTranscription(
            @PathVariable String meetingId) {

        log.debug("Transcription request. Meeting ID: {}", meetingId);

        return dataStore.getTranscription(meetingId)
                .map(t -> ResponseEntity.ok(ApiResponse.success(t)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Belirtilen toplantıdan AI tarafından çıkarılan görevleri (action items) getirir.
     *
     * Görevler, GPT-4 ile transkriptten otomatik çıkarılır ve şunları içerir:
     * - Görev tanımı (task description)
     * - Sorumlu kişi (assignee)
     * - Öncelik seviyesi (priority: HIGH/MEDIUM/LOW)
     * - Termin tarihi (due date)
     *
     * @param meetingId Aranan toplantının benzersiz ID'si
     * @return Görev listesi (ProcessedActionItem) veya 404 Not Found
     */
    @GetMapping("/{meetingId}/action-items")
    @Operation(summary = "Get meeting action items - AI-extracted tasks")
    public ResponseEntity<ApiResponse<ProcessedActionItem>> getMeetingActionItems(
            @PathVariable String meetingId) {

        log.debug("Action items request. Meeting ID: {}", meetingId);

        return dataStore.getActionItem(meetingId)
                .map(a -> ResponseEntity.ok(ApiResponse.success(a)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * HYBRID: MySQL + Kafka Cache
     * Tüm action items'ları listeler
     */
    @GetMapping("/action-items")
    @Operation(summary = "List all action items - HYBRID (MySQL + Kafka)")
    public ResponseEntity<ApiResponse<List<ProcessedActionItem>>> getAllActionItems() {
        log.info("[HYBRID] All action items request");

        
        List<ProcessedActionItem> mysqlTasks = fetchAllTasksFromMySQL();
        log.debug("[MySQL] Found {} action items", mysqlTasks.size());

        
        List<ProcessedActionItem> cacheTasks = dataStore.getAllActionItems();
        log.debug("[Kafka Cache] Found {} action items", cacheTasks.size());

        
        Set<String> existingMeetingIds = mysqlTasks.stream()
                .map(ProcessedActionItem::getMeetingId)
                .collect(Collectors.toSet());

        cacheTasks.stream()
                .filter(t -> !existingMeetingIds.contains(t.getMeetingId()))
                .forEach(mysqlTasks::add);

        log.info("[HYBRID] Total {} action items returned", mysqlTasks.size());
        return ResponseEntity.ok(ApiResponse.success(mysqlTasks));
    }

    /**
     * MySQL'den tüm task'ları çeker ve ProcessedActionItem'a dönüştürür
     */
    private List<ProcessedActionItem> fetchAllTasksFromMySQL() {
        
        Map<Long, List<TaskEntity>> tasksByMeeting = taskRepository.findAll().stream()
                .collect(Collectors.groupingBy(TaskEntity::getMeetingId));

        return tasksByMeeting.entrySet().stream()
                .map(entry -> convertToProcessedActionItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Task listesini ProcessedActionItem'a dönüştürür
     */
    private ProcessedActionItem convertToProcessedActionItem(Long meetingId, List<TaskEntity> tasks) {
        MeetingEntity meeting = meetingRepository.findById(meetingId).orElse(null);

        ProcessedActionItem item = new ProcessedActionItem();
        if (meeting != null) {
            item.setMeetingId(meeting.getExternalId());
        }

        
        List<String> actionItems = tasks.stream()
                .map(t -> String.format("%s - %s (Assignee: %s, Priority: %s)",
                        t.getTitle(), t.getDescription(), t.getAssignedToName(), t.getPriority()))
                .collect(Collectors.toList());

        item.setActionItems(actionItems);
        if (!tasks.isEmpty() && tasks.get(0).getProcessedTime() != null) {
            item.setProcessedTime(tasks.get(0).getProcessedTime().atZone(ZoneId.systemDefault()).toInstant());
        }

        return item;
    }
}
