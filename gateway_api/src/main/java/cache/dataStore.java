package cache;

import lombok.extern.slf4j.Slf4j;
import model.event.ProcessedActionItem;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * dataStore - Bellekte toplantı verilerini saklayan sınıf
 * 
 * Bu sınıf, Kafka'dan gelen işlenmiş toplantı verilerini geçici olarak bellekte tutar.
 * Discord ve Zoom toplantıları için özet, transkript ve görev verilerini yönetir.
 * 
 * Mimari: Event-Driven Architecture içinde In-Memory Cache rolü üstlenir.
 * Performans: ConcurrentHashMap kullanılarak thread-safe erişim sağlanır.
 * 
 * @author Ahmet
 * @version 1.0
 * @since 2025
 */
@Component
@Slf4j
public class dataStore {
    
    private final Map<String, ProcessedSummary> summaries = new ConcurrentHashMap<>();
    private final Map<String, ProcessedTranscription> transcriptions = new ConcurrentHashMap<>();
    private final Map<String, ProcessedActionItem> actionItems = new ConcurrentHashMap<>();

    /**
     * Toplantı özetini bellek deposuna kaydeder.
     * Aynı meetingId ile tekrar çağırılırsa önceki veri güncellenir.
     * 
     * @param summary AI servisi tarafından işlenmiş toplantı özeti
     */
    public void saveSummary(ProcessedSummary summary) {
        String meetingId = summary.getMeetingId();
        summaries.put(meetingId, summary);
        log.info("Meeting summary saved. Meeting ID: {}", meetingId);
    }

    /**
     * Belirtilen toplantı kimliğine ait özeti getirir.
     * 
     * @param meetingId Aranan toplantının benzersiz kimliği
     * @return Optional şeklinde toplantı özeti, bulunamazsa boş Optional
     */
    public Optional<ProcessedSummary> getSummary(String meetingId) {
        return Optional.ofNullable(summaries.get(meetingId));
    }

    /**
     * Bellekteki tüm toplantı özetlerini liste olarak döndürür.
     * 
     * @return Tüm özetlerin listesi
     */
    public List<ProcessedSummary> getAllSummaries() {
        return new ArrayList<>(summaries.values());
    }

    /**
     * Belirtilen platforma (Discord/Zoom) göre özetleri filtreler.
     * Platform karşılaştırması büyük-küçük harf duyarsızdır.
     * 
     * @param platform Filtrelenecek platform (DISCORD veya ZOOM)
     * @return Filtrelenmiş özet listesi
     */
    public List<ProcessedSummary> getSummariesByPlatform(String platform) {
        return summaries.values().stream()
                .filter(s -> s.getPlatform() != null && s.getPlatform().equalsIgnoreCase(platform))
                .collect(Collectors.toList());
    }

    /**
     * En son işlenen toplantı özetlerini tarihe göre sıralı getirir.
     * En yeni özetler listenin başında yer alır.
     * 
     * @param limit Getirilecek maksimum özet sayısı
     * @return Tarihe göre sıralı özet listesi
     */
    public List<ProcessedSummary> getLastSummaries(int limit) {
        return summaries.values().stream()
                .sorted((a, b) -> b.getProcessedTime().compareTo(a.getProcessedTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Toplantı transkriptini bellek deposuna kaydeder.
     * Transkript, ses kayıtlarından metne dönüştürülmüş toplantı içeriğidir.
     * 
     * @param transcription AI servisi tarafından oluşturulan transkript
     */
    public void saveTranscription(ProcessedTranscription transcription) {
        String meetingId = transcription.getMeetingId();
        transcriptions.put(meetingId, transcription);
        log.info("Transcription saved. Meeting ID: {}", meetingId);
    }

    /**
     * Belirtilen toplantı kimliğine ait transkripti getirir.
     * 
     * @param meetingId Aranan toplantının benzersiz kimliği
     * @return Optional şeklinde transkript, bulunamazsa boş Optional
     */
    public Optional<ProcessedTranscription> getTranscription(String meetingId) {
        return Optional.ofNullable(transcriptions.get(meetingId));
    }

    /**
     * Toplantıdan çıkarılan görevleri (action items) bellek deposuna kaydeder.
     * Görevler, AI tarafından toplantı içeriğinden otomatik çıkarılır.
     * 
     * @param actionItem AI servisi tarafından belirlenen görev listesi
     */
    public void saveActionItems(ProcessedActionItem actionItem) {
        String meetingId = actionItem.getMeetingId();
        actionItems.put(meetingId, actionItem);
        log.info("Action items list saved. Meeting ID: {}", meetingId);
    }

    /**
     * Belirtilen toplantı kimliğine ait görev listesini getirir.
     * 
     * @param meetingId Aranan toplantının benzersiz kimliği
     * @return Optional şeklinde görev listesi, bulunamazsa boş Optional
     */
    public Optional<ProcessedActionItem> getActionItem(String meetingId) {
        return Optional.ofNullable(actionItems.get(meetingId));
    }

    /**
     * Tüm toplantılara ait görev listelerini döndürür.
     * Dashboard'da genel görev takibi için kullanılır.
     * 
     * @return Tüm görev listelerinin koleksiyonu
     */
    public List<ProcessedActionItem> getAllActionItems() {
        return new ArrayList<>(actionItems.values());
    }

    /**
     * Sistem istatistiklerini hesaplar ve döndürür.
     * Dashboard'da gösterilmek üzere özet bilgiler sunar.
     * 
     * @return İstatistik bilgilerini içeren Map yapısı
     */
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        int totalActionItems = actionItems.values().stream()
                .mapToInt(item -> item.getActionItems() != null ? item.getActionItems().size() : 0)
                .sum();
        
        stats.put("totalMeetings", summaries.size());
        stats.put("totalTranscriptions", transcriptions.size());
        stats.put("totalActionItems", totalActionItems);
        stats.put("discordMeetings", getSummariesByPlatform("DISCORD").size());
        stats.put("zoomMeetings", getSummariesByPlatform("ZOOM").size());
        
        return stats;
    }

    /**
     * Belirtilen toplantıya ait tüm verileri bellekten temizler.
     * Toplantı sonlandıktan sonra bellek yönetimi için kullanılır.
     * 
     * @param meetingId Temizlenecek toplantının benzersiz kimliği
     */
    public void clearCache(String meetingId) {
        summaries.remove(meetingId);
        transcriptions.remove(meetingId);
        actionItems.remove(meetingId);
        log.info("Meeting data cleared. Meeting ID: {}", meetingId);
    }
}
