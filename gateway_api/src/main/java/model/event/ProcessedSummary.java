package model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * ProcessedSummary - AI servisi tarafından oluşturulan toplantı özet modeli
 * 
 * Bu sınıf, Kafka üzerinden AI servisinden gelen işlenmiş toplantı
 * özet verilerini temsil eder. Discord ve Zoom toplantıları için kullanılır.
 * 
 * İçerik:
 * - Toplantı meta bilgileri (başlık, platform, süre)
 * - AI tarafından oluşturulan özet metin
 * - Toplantının ana konuları (key points)
 * - Katılımcı listesi
 * 
 * Kaynak: AI Service (Gemini API entegrasyonu)
 * Hedef: API Gateway bellek deposu ve UI
 * 
 * @author Ahmet
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedSummary {
    
    /** Toplantının benzersiz kimliği - UUID formatında */
    private String meetingId;
    
    /** Discord kanal ID veya Zoom meeting ID */
    private String channelId;
    
    /** Toplantı platformu: DISCORD veya ZOOM */
    private String platform;
    
    /** Toplantı başlığı veya konusu */
    private String title;
    
    /** AI tarafından oluşturulan toplantı özeti - Türkçe metin */
    private String summary;
    
    /** Toplantının ana konuları ve alınan kararlar listesi */
    private List<String> keyPoints;
    
    /** Toplantıya katılan kişilerin listesi */
    private List<String> participants;
    
    /** Toplantının başlangıç zamanı - UTC formatında */
    private Instant meetingStartTime;
    
    /** Toplantının bitiş zamanı - UTC formatında */
    private Instant meetingEndTime;
    
    /** AI işlem tamamlanma zamanı - UTC formatında */
    private Instant processedTime;
}
