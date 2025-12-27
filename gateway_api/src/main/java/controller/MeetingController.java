package controller;

import cache.dataStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.dto.ApiResponse;
import model.dto.MeetingDetailDTO;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import model.event.ProcessedActionItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * MeetingController - Toplantı verilerine erişim sağlayan REST API
 * 
 * Bu kontrolcü, Discord ve Zoom platformlarından toplanan ve AI servisi
 * tarafından işlenen toplantı verilerine erişim sağlar.
 * 
 * Sunulan Özellikler:
 * - Toplantı özetlerini listeleme ve filtreleme
 * - Toplantı transkriptlerine erişim
 * - AI tarafından belirlenen görevleri görüntüleme
 * - Platform bazlı filtreleme (Discord/Zoom)
 * 
 * Güvenlik: JWT token ile korunmaktadır
 * 
 * @author Ahmet
 * @version 1.0
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/meetings")
@Tag(name = "Toplanti_Yonetimi", description = "Discord ve Zoom toplanti verilerine erisim endpointleri")
public class MeetingController {
    
    private final dataStore dataStore;

    /**
     * Tüm toplantı özetlerini listeler.
     * Platform parametresi verilirse filtreleme yapar.
     * 
     * Örnek Kullanım:
     * GET /api/v1/meetings?platform=DISCORD&limit=10
     * 
     * @param platform Opsiyonel platform filtresi (DISCORD/ZOOM)
     * @param limit Döndürülecek maksimum kayıt sayısı
     * @return Toplantı özetlerinin listesi
     */
    @GetMapping
    @Operation(summary = "Tum toplanti ozetlerini listele - Platform ve limit ile filtrelenebilir")
    public ResponseEntity<ApiResponse<List<ProcessedSummary>>> getAllMeetings(
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "20") int limit) {
        
        log.info("Toplanti listesi istegi alindi. Platform: {}, Limit: {}", platform, limit);
        
        List<ProcessedSummary> summaries;
        
        if (platform != null && !platform.trim().isEmpty()) {
            summaries = dataStore.getSummariesByPlatform(platform);
            log.debug("{} platformu icin {} adet toplanti bulundu", platform, summaries.size());
        } else {
            summaries = dataStore.getLastSummaries(limit);
            log.debug("Son {} adet toplanti getirildi", summaries.size());
        }
        
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    /**
     * Belirtilen toplantı kimliğine ait tüm detayları getirir.
     * Özet, transkript ve görev listesini tek istekte döndürür.
     * 
     * @param meetingId Aranan toplantının benzersiz ID'si
     * @return Toplantı detay bilgileri veya 404 Not Found
     */
    @GetMapping("/{meetingId}")
    @Operation(summary = "Toplanti detaylarini getir - Ozet, transkript ve gorevler dahil")
    public ResponseEntity<ApiResponse<MeetingDetailDTO>> getMeetingDetail(
            @PathVariable String meetingId) {
        
        log.info("Toplanti detayi istegi. Toplanti ID: {}", meetingId);
        
        Optional<ProcessedSummary> summary = dataStore.getSummary(meetingId);
        
        if (summary.isEmpty()) {
            log.warn("Toplanti bulunamadi. ID: {}", meetingId);
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
     * Özet, toplantının ana konularını ve kararları içerir.
     * 
     * @param meetingId Aranan toplantının benzersiz ID'si
     * @return Toplantı özeti veya 404 Not Found
     */
    @GetMapping("/{meetingId}/summary")
    @Operation(summary = "Toplanti AI ozetini getir - Ana konular ve kararlar")
    public ResponseEntity<ApiResponse<ProcessedSummary>> getMeetingSummary(
            @PathVariable String meetingId) {
        
        log.debug("Toplanti ozeti istegi. ID: {}", meetingId);
        
        return dataStore.getSummary(meetingId)
                .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Belirtilen toplantının ses-metin dönüşümü sonucunu getirir.
     * Transkript, toplantıdaki tüm konuşmaları metin olarak içerir.
     * 
     * @param meetingId Aranan toplantının benzersiz ID'si
     * @return Toplantı transkripti veya 404 Not Found
     */
    @GetMapping("/{meetingId}/transcription")
    @Operation(summary = "Toplanti transkriptini getir - Ses-metin donusumu sonucu")
    public ResponseEntity<ApiResponse<ProcessedTranscription>> getMeetingTranscription(
            @PathVariable String meetingId) {
        
        log.debug("Transkript istegi. Toplanti ID: {}", meetingId);
        
        return dataStore.getTranscription(meetingId)
                .map(t -> ResponseEntity.ok(ApiResponse.success(t)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Belirtilen toplantıdan AI tarafından çıkarılan görevleri getirir.
     * Görevler, toplantıda belirlenen yapılacak işleri içerir.
     * 
     * @param meetingId Aranan toplantının benzersiz ID'si
     * @return Görev listesi veya 404 Not Found
     */
    @GetMapping("/{meetingId}/action-items")
    @Operation(summary = "Toplanti gorevlerini getir - AI tarafindan cikarilan yapilacaklar")
    public ResponseEntity<ApiResponse<ProcessedActionItem>> getMeetingActionItems(
            @PathVariable String meetingId) {
        
        log.debug("Gorev listesi istegi. Toplanti ID: {}", meetingId);
        
        return dataStore.getActionItem(meetingId)
                .map(a -> ResponseEntity.ok(ApiResponse.success(a)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Tüm toplantılardaki görevlerin birleşik listesini döndürür.
     * Dashboard'da genel görev takibi için kullanılır.
     * 
     * @return Tüm görev listelerinin koleksiyonu
     */
    @GetMapping("/action-items")
    @Operation(summary = "Tum gorevleri listele - Tum toplantilardaki yapilacaklar")
    public ResponseEntity<ApiResponse<List<ProcessedActionItem>>> getAllActionItems() {
        log.info("Tum gorevler listesi istegi alindi");
        
        List<ProcessedActionItem> allItems = dataStore.getAllActionItems();
        log.debug("Toplam {} adet gorev listesi donduruldu", allItems.size());
        
        return ResponseEntity.ok(ApiResponse.success(allItems));
    }
}
