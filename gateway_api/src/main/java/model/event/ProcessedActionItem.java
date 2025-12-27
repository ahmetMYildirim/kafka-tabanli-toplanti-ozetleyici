package model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * ProcessedActionItem - AI servisi tarafından çıkarılan görev listesi modeli
 * 
 * Bu sınıf, toplantı içeriğinden AI tarafından otomatik olarak belirlenen
 * yapılacak işleri (action items) temsil eder.
 * 
 * İçerik:
 * - Toplantı kimlik bilgisi
 * - Görev listesi (atanan kişi, öncelik, tarih dahil)
 * - İşlem zamanı
 * 
 * Kullanım: Toplantı sonrası görev takibi ve atama
 * Kaynak: AI Service (Toplantı özeti analizi)
 * Hedef: API Gateway bellek deposu ve UI
 * 
 * @author Ahmet
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedActionItem {
    
    /** Toplantının benzersiz kimliği */
    private String meetingId;
    
    /** Toplantıdan çıkarılan görevlerin listesi */
    private List<String> actionItems;
    
    /** AI işlem tamamlanma zamanı */
    private Instant processedTime;

    /**
     * ActionItem - Tek bir görevi temsil eden iç sınıf
     * 
     * Her görev; yapılacak iş, atanan kişi, öncelik seviyesi,
     * teslim tarihi ve bağlam bilgilerini içerir.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        
        /** Yapılacak görev açıklaması */
        private String task;
        
        /** Görevin atandığı kişi */
        private String assign;
        
        /** Öncelik seviyesi: HIGH, MEDIUM, LOW */
        private String priority;
        
        /** Teslim tarihi */
        private String dueDate;
        
        /** Görevin toplantı içindeki bağlamı */
        private String context;
    }
}
