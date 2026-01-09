package com.toplanti.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Transcription - Transkript modeli
 * @author Ömer
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transcription {

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

        /** Konuşmanın başlangıç zamanı (ms) */
        private Long startTimeMs;

        /** Konuşmanın bitiş zamanı (ms) */
        private Long endTimeMs;

        /** Güven skoru */
        private Double confidence;
    }
}

