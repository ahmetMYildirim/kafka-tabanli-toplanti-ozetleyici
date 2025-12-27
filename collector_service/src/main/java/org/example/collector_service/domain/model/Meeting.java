package org.example.collector_service.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Meeting - Toplantı entity sınıfı
 * 
 * Discord ve Zoom platformlarından toplanan toplantı oturumlarını temsil eder.
 * Toplantının yaşam döngüsünü, katılımcı sayısını ve platform bilgilerini saklar.
 * 
 * Veritabanı: meetings tablosu
 * Aggregate Root: Outbox event'leri için ana entity
 * 
 * @author Ahmet
 * @version 1.0
 */
@Entity
@Table(name = "meetings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meeting {
    
    /** Otomatik artan primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Platform adı (DISCORD veya ZOOM) */
    private String platform;
    
    /** Platform üzerindeki toplantı ID'si (Discord channel ID veya Zoom meeting ID) */
    private String meetingId;
    
    /** Toplantı başlığı veya konusu */
    private String title;
    
    /** Toplantı başlangıç zamanı */
    private LocalDateTime startTime;
    
    /** Toplantı bitiş zamanı */
    private LocalDateTime endTime;
    
    /** Katılımcı sayısı */
    private Integer participants;
}

