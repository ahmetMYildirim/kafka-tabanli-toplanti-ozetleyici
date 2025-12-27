package org.example.collector_service.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Message - Mesaj entity sınıfı
 * 
 * Discord ve Zoom platformlarından toplanan metin mesajlarını saklar.
 * Her mesaj, yazarı, içeriği ve zaman damgası ile birlikte kaydedilir.
 * 
 * Veritabanı: messages tablosu
 * Kullanım: Toplantı sohbet kayıtları, transkript oluşturma
 * 
 * @author Ahmet
 * @version 1.0
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    
    /** Otomatik artan primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Platform adı (DISCORD veya ZOOM) */
    private String platform;
    
    /** Mesajı yazan kullanıcının adı */
    private String author;
    
    /** Mesaj içeriği (metin) */
    @Column(columnDefinition = "TEXT")
    private String content;
    
    /** Mesajın gönderilme zamanı */
    private LocalDateTime timestamp;

    /** Mesajın toplantı IDsi **/
    private String meetingId;

    /** Mesajın geldiği kanal ID'si */
    private String channelId;

    /** Mesajın geldiği kanal adı */
    private String channelName;
}

