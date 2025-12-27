package controller;

import cache.dataStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * DashboardController - Ana sayfa istatistiklerini sunan REST API
 * 
 * Bu kontrolcü, UI dashboard sayfası için gerekli özet istatistikleri sağlar.
 * Toplantı sayıları, transkript sayıları ve görev istatistikleri döndürür.
 * 
 * Sunulan Veriler:
 * - Toplam toplantı sayısı
 * - Platform bazlı toplantı dağılımı (Discord/Zoom)
 * - Toplam transkript sayısı
 * - Toplam görev sayısı
 * 
 * @author Ahmet
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Ana sayfa istatistikleri ve ozet bilgiler")
public class DashboardController {
    
    private final dataStore dataStore;

    /**
     * Dashboard için özet istatistikleri döndürür.
     * 
     * Dönen veriler:
     * - totalMeetings: Toplam toplantı sayısı
     * - totalTranscriptions: Toplam transkript sayısı
     * - totalActionItems: Toplam görev sayısı
     * - discordMeetings: Discord toplantı sayısı
     * - zoomMeetings: Zoom toplantı sayısı
     * 
     * @return İstatistik verilerini içeren ApiResponse
     */
    @GetMapping("/stats")
    @Operation(summary = "Dashboard istatistiklerini getir - Toplanti ve gorev sayilari")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        log.info("Dashboard istatistikleri istegi alindi");
        
        Map<String, Object> stats = dataStore.getUserStatistics();
        
        log.debug("Dashboard istatistikleri: {}", stats);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
