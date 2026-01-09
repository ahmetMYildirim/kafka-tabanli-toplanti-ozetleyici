package org.example.ai_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AudioMessage - collector_service ile PAYLASILAN entity
 *
 * Bu entity, collector_service'deki audio_messages tablosuna karsilik gelir.
 * AI Service bu tabloya transkripsiyon sonuclarini yazar.
 *
 * Tablo: audio_messages (collector_service tarafindan olusturulmus)
 */
@Entity
@Table(name = "audio_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Platform adi (DISCORD, ZOOM, GOOGLE_MEET, TEAMS) */
    private String platform;

    /** Kanal ID'si */
    private String channelId;

    /** Konusan kullanicinin adi */
    private String author;

    /** Ses dosyasinin URL'si (local storage path) */
    private String audioUrl;

    /** Ses dosyasinin fiziksel yolu */
    private String audioFilePath;

    /** Ses dosyasinin suresi (saniye) */
    private Double durationSeconds;

    /** Dosya boyutu (byte) */
    private Long fileSizeBytes;

    /** MIME tipi */
    private String mimeType;

    /** Transkripsiyon durumu */
    private String transcriptionStatus;

    /** Islenme zamani */
    private LocalDateTime processedAt;

    /**
     * AI tarafindan transkript edilmis metin
     * BU ALANI AI_SERVICE GUNCELLER
     */
    @Column(columnDefinition = "TEXT")
    private String transcription;

    /** Ses kaydinin zaman damgasi */
    private LocalDateTime timestamp;

    /** Ilgili sesli oturumun ID'si */
    private String voiceSessionId;

    /** Mesajin toplanti ID'si */
    private Long meetingId;
}
