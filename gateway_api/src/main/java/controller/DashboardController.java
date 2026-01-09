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
 * DashboardController - Ana sayfa istatistiklerini sunan REST API kontrolcüsü
 * 
 * Bu kontrolcü, UI dashboard sayfası için gerekli özet istatistikleri sağlar.
 * In-memory cache'den (dataStore) real-time istatistikler hesaplanır.
 * 
 * Sunulan Veriler:
 * - Toplam toplantı sayısı (total meetings count)
 * - Platform bazlı toplantı dağılımı (Discord/Zoom/Teams breakdown)
 * - Toplam transkript sayısı (processed transcriptions)
 * - Toplam görev sayısı (action items count)
 * - Son aktivite zamanı (last activity timestamp)
 * 
 * Veri Kaynağı:
 * - In-memory dataStore (Kafka consumer'lardan beslenir)
 * - Real-time güncel veriler
 * - Veritabanına erişim olmaz (performance için)
 * 
 * Kullanım: Dashboard widget'ları bu endpoint'i polling ile çağırır
 * 
 * @author Ahmet
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard", description = "Home page statistics and summary information")
public class DashboardController {
    
    private final dataStore dataStore;

    /**
     * Dashboard için özet istatistikleri döndürür.
     * 
     * Bu metod, dataStore'dan anlık (real-time) istatistikleri hesaplar
     * ve Map formatında döndürür. Frontend dashboard widget'ları tarafından
     * periyodik olarak (her 10-30 saniyede) çağrılır.
     * 
     * Dönen veriler (Map key'leri):
     * - totalMeetings: Toplam toplantı sayısı (Integer)
     * - totalTranscriptions: Toplam transkript sayısı (Integer)
     * - totalActionItems: Toplam görev sayısı (Integer)
     * - discordMeetings: Discord toplantı sayısı (Integer)
     * - zoomMeetings: Zoom toplantı sayısı (Integer)
     * - teamsMeetings: Teams toplantı sayısı (Integer, opsiyonel)
     * 
     * Performance: O(n) complexity, cache-based hesaplama (hızlı)
     * 
     * @return İstatistik verilerini içeren ApiResponse<Map<String, Object>>
     */
    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics - Meeting and task counts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        log.info("Dashboard statistics request received");
        
        Map<String, Object> stats = dataStore.getUserStatistics();
        
        log.debug("Dashboard statistics: {}", stats);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
