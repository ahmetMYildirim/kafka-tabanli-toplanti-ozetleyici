package org.example.collector_service.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * VoiceSession - Sesli oturum entity sınıfı
 * 
 * Discord ve Zoom ses kanallarındaki oturumları temsil eder.
 * Katılımcı sayısı, oturum süresi ve kanal bilgilerini saklar.
 * 
 * Kullanım:
 * - Ses kayıtlarını gruplandırma
 * - Toplantı süresi hesaplama
 * - Katılımcı takibi
 * 
 * Veritabanı: voice_sessions tablosu
 * İlişki: AudioMessage ile one-to-many (voiceSessionId)
 * 
 * @author Ahmet
 * @version 1.0
 */
@Entity
@Table(name = "voice_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceSession {
    
    /** Otomatik artan primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Platform adı (DISCORD veya ZOOM) */
    private String platform;
    
    /** Ses kanalının ID'si */
    private String channelId;
    
    /** Ses kanalının adı */
    private String channelName;
    
    /** Oturum başlangıç zamanı */
    private LocalDateTime startTime;
    
    /** Oturum bitiş zamanı */
    private LocalDateTime endTime;
    
    /** Oturumdaki katılımcı sayısı */
    private Integer participantCount;
}

