package org.example.collector_service.repository;

import org.example.collector_service.domain.model.OutBoxEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OutBoxEventRepository - Outbox event veri erişim katmanı
 * 
 * Transactional Outbox Pattern için event kayıtlarını yönetir.
 * İşlenmemiş event'leri sorgulama, işaretleme ve temizleme işlemleri sağlar.
 * 
 * Kullanım Senaryoları:
 * - Kafka'ya gönderilmemiş event'leri bulma
 * - Event'leri işlenmiş olarak işaretleme
 * - Eski işlenmiş event'leri temizleme
 * 
 * @author Ahmet
 * @version 1.0
 */
@Repository
public interface OutBoxEventRepository extends JpaRepository<OutBoxEvent, Long> {

    /**
     * Tüm işlenmemiş event'leri getirir.
     *
     * @return İşlenmemiş event listesi
     */
    @Query("SELECT e FROM OutBoxEvent e WHERE e.processed = false")
    List<OutBoxEvent> findUnprocessed();

    /**
     * İşlenmemiş event'lerden en eski 100 tanesini getirir.
     * Kafka relay işlemi için batch olarak kullanılır.
     *
     * @param pageRequest Sayfalama bilgisi
     * @return Maksimum 100 işlenmemiş event
     */
    @Query("SELECT e FROM OutBoxEvent e WHERE e.processed = false ORDER BY e.createdAt ASC")
    List<OutBoxEvent> find100Unprocessed(PageRequest pageRequest);

    /**
     * Belirli bir event tipine göre işlenmemiş event'leri getirir.
     *
     * @param eventType Event tipi (Created, Updated, Started, Ended vb.)
     * @return Filtrelenmiş işlenmemiş event listesi
     */
    @Query("SELECT e FROM OutBoxEvent e WHERE e.processed = false AND e.eventType = :eventType")
    List<OutBoxEvent> findUnprocessedByEventType(@Param("eventType") String eventType);

    /**
     * Aggregate tipi ve kimliğine göre event'leri getirir.
     * Belirli bir entity'nin tüm event geçmişini görüntülemek için kullanılır.
     *
     * @param aggregateType Aggregate tipi (Meeting, Message, VoiceSession vb.)
     * @param aggregateId   Aggregate kimliği
     * @return İlgili event listesi
     */
    List<OutBoxEvent> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);

    /**
     * Verilen ID listesindeki event'leri işlenmiş olarak işaretler.
     * Kafka'ya başarıyla gönderilen event'ler için kullanılır.
     *
     * @param ids İşaretlenecek event ID'leri
     * @return Güncellenen kayıt sayısı
     */
    @Modifying
    @Query("UPDATE OutBoxEvent e SET e.processed = true WHERE e.id IN :ids")
    int markAsProcessed(@Param("ids") List<Long> ids);

    /**
     * Belirli bir tarihten önce işlenmiş event'leri siler.
     * Veritabanı temizliği için periyodik olarak çalıştırılır.
     *
     * @param beforeDate Bu tarihten önceki event'ler silinir
     * @return Silinen kayıt sayısı
     */
    @Modifying
    @Query("DELETE FROM OutBoxEvent e WHERE e.processed = true AND e.createdAt < :beforeDate")
    int deleteProcessedEventBefore(@Param("beforeDate") LocalDateTime beforeDate);
}