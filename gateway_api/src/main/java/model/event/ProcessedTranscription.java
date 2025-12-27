package model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * ProcessedTranscription - AI servisi tarafından oluşturulan transkript modeli
 * 
 * Bu sınıf, toplantı ses kayıtlarının metne dönüştürülmüş halini temsil eder.
 * Discord ve Zoom ses kanallarından alınan ses verileri AI servisi tarafından
 * işlenerek bu formata dönüştürülür.
 * 
 * İçerik:
 * - Toplantı kimlik bilgileri
 * - Konuşma segmentleri (kim, ne zaman, ne söyledi)
 * - Tam transkript metni
 * 
 * Kaynak: AI Service (Speech-to-Text dönüşümü)
 * Hedef: API Gateway bellek deposu ve UI
 * 
 * @author Ahmet
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedTranscription {
    
    /** Toplantının benzersiz kimliği */
    private String meetingId;
    
    /** Discord kanal ID veya Zoom meeting ID */
    private String channelId;
    
    /** Konuşma segmentlerinin listesi */
    private List<String> segments;
    
    /** Tüm konuşmaların birleştirilmiş tam metni */
    private String fullTranscription;
    
    /** AI işlem tamamlanma zamanı */
    private Instant processedTime;

    /**
     * TranscriptionSegment - Tek bir konuşma segmentini temsil eden iç sınıf
     * 
     * Her segment, bir konuşmacının belirli bir zaman aralığındaki
     * konuşmasını içerir.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptionSegment {
        
        /** Konuşmacının benzersiz kimliği */
        private String speakerId;
        
        /** Konuşmacının görünen adı */
        private String speakerName;
        
        /** Konuşma metni */
        private String text;
        
        /** Konuşmanın başlangıç zamanı */
        private Instant startTime;
        
        /** Konuşmanın bitiş zamanı */
        private Instant endTime;
    }
}
