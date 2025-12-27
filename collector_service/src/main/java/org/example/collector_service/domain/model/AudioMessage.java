package org.example.collector_service.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * AudioMessage - Ses mesajı entity sınıfı
 * 
 * Discord ve Zoom'dan toplanan ses kayıtlarını ve transkriptlerini saklar.
 * Her ses kaydı, ilgili sesli oturuma (VoiceSession) bağlanır.
 * 
 * Özellikler:
 * - Ses dosyası URL'i (audio_storage klasöründe)
 * - AI tarafından oluşturulan transkript metni
 * - Katılımcı bilgisi
 * 
 * Veritabanı: audio_messages tablosu
 * İlişki: VoiceSession'a many-to-one (voiceSessionId)
 * 
 * @author Ahmet
 * @version 1.0
 */
@Entity
@Table(name = "audio_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioMessage {
    
    /** Otomatik artan primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Platform adı (DISCORD veya ZOOM) */
    private String platform;
    
    /** Kanal ID'si */
    private String channelId;
    
    /** Konuşan kullanıcının adı */
    private String author;
    
    /** Ses dosyasının URL'si (local storage path) */
    private String audioUrl;
    
    /** AI tarafından transkript edilmiş metin */
    @Column(columnDefinition = "TEXT")
    private String transcription;
    
    /** Ses kaydının zaman damgası */
    private LocalDateTime timestamp;
    
    /** İlgili sesli oturumun ID'si (foreign key mantığı) */
    private String voiceSessionId;

    /** Mesajın toplantı IDsi **/
    private String meetingId;

}

