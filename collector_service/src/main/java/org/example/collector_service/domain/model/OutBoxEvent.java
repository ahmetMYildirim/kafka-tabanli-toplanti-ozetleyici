package org.example.collector_service.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * OutBoxEvent - Transactional Outbox Pattern event entity sınıfı
 * 
 * Transactional outbox pattern implementasyonu için event kayıtlarını saklar.
 * Veritabanı transaction'ı ile birlikte kaydedilerek at-least-once delivery garantisi sağlar.
 * 
 * Çalışma Prensibi:
 * 1. Aggregate (Meeting, Message vb.) ile aynı transaction'da kaydedilir
 * 2. OutboxEventRelayer scheduled job ile işlenir
 * 3. Kafka'ya başarıyla gönderildikten sonra processed=true olarak işaretlenir
 * 
 * Veritabanı: outbox tablosu
 * Pattern: Transactional Outbox (Microservices Pattern)
 * 
 * @author Ahmet
 * @version 1.0
 */
@Entity
@Table(name = "outbox")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutBoxEvent {
    
    /** Otomatik artan primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** Aggregate tipi (Meeting, Message, AudioMessage vb.) */
    private String aggregateType;
    
    /** Aggregate'in ID'si */
    private String aggregateId;
    
    /** Event tipi (Created, Updated, Deleted, Started, Ended) */
    private String eventType;
    
    /** JSON formatında event payload */
    @Column(columnDefinition = "TEXT")
    private String payload;
    
    /** Event oluşturulma zamanı */
    private LocalDateTime createdAt;
    
    /** Kafka'ya gönderildi mi? */
    @Column(name = "is_processed")
    @Builder.Default
    private boolean processed = false;
}

