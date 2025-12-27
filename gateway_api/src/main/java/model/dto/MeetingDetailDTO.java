package model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.event.ProcessedActionItem;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;

/**
 * MeetingDetailDTO - Toplantı detay bilgilerini içeren DTO
 * 
 * Bu sınıf, tek bir toplantıya ait tüm işlenmiş verileri
 * bir arada sunar. /api/v1/meetings/{meetingId} endpoint'inden döner.
 * 
 * İçerik:
 * - processedSummary: AI tarafından oluşturulan özet
 * - processedTranscription: Ses-metin dönüşümü sonucu
 * - processedActionItem: Toplantıdan çıkarılan görevler
 * 
 * Not: Alanlar null olabilir (henüz işlenmemiş olabilir).
 * 
 * @author Ahmet
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDetailDTO {
    
    /** AI tarafından oluşturulan toplantı özeti */
    private ProcessedSummary processedSummary;
    
    /** Ses-metin dönüşümü sonucu */
    private ProcessedTranscription processedTranscription;
    
    /** Toplantıdan çıkarılan görev listesi */
    private ProcessedActionItem processedActionItem;
}
