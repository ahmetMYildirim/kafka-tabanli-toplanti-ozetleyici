package org.example.collector_service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collector_service.domain.model.OutBoxEvent;
import org.example.collector_service.repository.OutBoxEventRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * OutboxEventPublisher - Merkezi Outbox event yayınlama servisi
 * 
 * Transactional Outbox Pattern için event oluşturma mantığını merkezileştirir.
 * Tüm servisler bu sınıfı kullanarak outbox event'lerini yayınlar.
 * 
 * Avantajlar:
 * - Kod tekrarını önler (DRY - Don't Repeat Yourself prensibi)
 * - Merkezi hata yönetimi
 * - Tutarlı event formatı
 * - Test edilebilirlik
 * 
 * Mimari Pattern: Transactional Outbox Pattern
 * Bu pattern, veritabanı transaction'ı ile event yayınlama işlemini
 * atomik hale getirir. Böylece data inconsistency önlenir.
 * 
 * İş Akışı:
 * 1. Service, aggregate'i veritabanına kaydeder
 * 2. Aynı transaction içinde OutboxEventPublisher çağrılır
 * 3. Event, outbox tablosuna JSON olarak yazılır
 * 4. OutboxEventRelayer bu event'leri Kafka'ya gönderir
 * 5. Başarılı gönderimden sonra processed=true olarak işaretlenir
 * 
 * Kullanım: Service'ler @Transactional metod içinde aggregate kayıtla
 * birlikte bu sınıfı çağırmalıdır.
 * 
 * @author Ahmet
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {
    
    private final OutBoxEventRepository outBoxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Aggregate için outbox event oluşturur ve veritabanına kaydeder.
     * 
     * Transaction içinde çağrılmalıdır (caller'ın @Transactional olması gerekir).
     * JSON serialization hatası durumunda OutboxPublicationException fırlatır.
     * 
     * @param aggregate     Event yayınlanacak aggregate (Meeting, Message vb.)
     * @param aggregateId   Aggregate'in unique ID'si
     * @param aggregateType Aggregate tipi (Meeting, Message vb.)
     * @param eventType     Event tipi (Created, Updated, Deleted vb.)
     * @param <T>           Aggregate'in generic tipi
     * @throws OutboxPublicationException JSON serialization hatası durumunda
     */
    public <T> void publishEvent(T aggregate, String aggregateId, String aggregateType, String eventType) {
        try {
            String payload = serializeToJson(aggregate);
            
            OutBoxEvent event = OutBoxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payload)
                    .createdAt(LocalDateTime.now())
                    .processed(false)
                    .build();
            
            outBoxEventRepository.save(event);
            
            log.debug("Outbox event published: type={}, id={}, event={}", 
                    aggregateType, aggregateId, eventType);
            
        } catch (JsonProcessingException e) {
            String errorMsg = String.format(
                    "Outbox event serialization failed: type=%s, id=%s, event=%s", 
                    aggregateType, aggregateId, eventType
            );
            log.error(errorMsg, e);
            throw new OutboxPublicationException(errorMsg, e);
        }
    }

    /**
     * Created event'i için kısayol metod.
     * 
     * @param aggregate     Event yayınlanacak aggregate
     * @param aggregateId   Aggregate ID'si
     * @param aggregateType Aggregate tipi
     * @param <T>           Aggregate generic tipi
     */
    public <T> void publishCreated(T aggregate, String aggregateId, String aggregateType) {
        publishEvent(aggregate, aggregateId, aggregateType, "Created");
    }

    /**
     * Updated event'i için kısayol metod.
     * 
     * @param aggregate     Event yayınlanacak aggregate
     * @param aggregateId   Aggregate ID'si
     * @param aggregateType Aggregate tipi
     * @param <T>           Aggregate generic tipi
     */
    public <T> void publishUpdated(T aggregate, String aggregateId, String aggregateType) {
        publishEvent(aggregate, aggregateId, aggregateType, "Updated");
    }

    /**
     * Started event'i için kısayol metod (Meeting için).
     * 
     * @param aggregate     Event yayınlanacak aggregate
     * @param aggregateId   Aggregate ID'si
     * @param aggregateType Aggregate tipi
     * @param <T>           Aggregate generic tipi
     */
    public <T> void publishStarted(T aggregate, String aggregateId, String aggregateType) {
        publishEvent(aggregate, aggregateId, aggregateType, "Started");
    }

    /**
     * Ended event'i için kısayol metod (Meeting için).
     * 
     * @param aggregate     Event yayınlanacak aggregate
     * @param aggregateId   Aggregate ID'si
     * @param aggregateType Aggregate tipi
     * @param <T>           Aggregate generic tipi
     */
    public <T> void publishEnded(T aggregate, String aggregateId, String aggregateType) {
        publishEvent(aggregate, aggregateId, aggregateType, "Ended");
    }

    /**
     * Aggregate'i JSON string'e serialize eder.
     * 
     * @param aggregate Serialize edilecek obje
     * @param <T>       Obje tipi
     * @return JSON string
     * @throws JsonProcessingException Serialization hatası
     */
    private <T> String serializeToJson(T aggregate) throws JsonProcessingException {
        return objectMapper.writeValueAsString(aggregate);
    }
}

