package com.toplanti.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MeetingDetail - Toplantı detay modeli
 * @author Ömer
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDetail {

    /** AI tarafından oluşturulan toplantı özeti */
    private MeetingSummary processedSummary;

    /** Ses-metin dönüşümü sonucu */
    private Transcription processedTranscription;

    /** Toplantıdan çıkarılan görev listesi */
    private ActionItem processedActionItem;
}

