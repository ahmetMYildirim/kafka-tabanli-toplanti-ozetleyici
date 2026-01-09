package org.example.collector_service.service;

import org.example.collector_service.domain.model.Message;
import org.example.collector_service.outbox.OutboxEventPublisher;
import org.example.collector_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MessageService - Mesaj yönetim servisi
 * 
 * Discord ve Zoom'dan gelen metin mesajlarını işler ve saklar.
 * Her mesaj kaydı için Outbox pattern kullanarak MessageCreated event'i oluşturur.
 * 
 * Sorumluluklar:
 * - Mesaj persistance (veritabanına kalıcı kayıt)
 * - Outbox event yayınlama (OutboxEventPublisher aracılığıyla Kafka'ya)
 * - Transaction yönetimi (@Transactional ile ACID garantisi)
 * 
 * Mimari Pattern: Transactional Service + Outbox Pattern
 * 
 * Kullanım Senaryoları:
 * - Discord channel'larındaki text mesajlar
 * - Zoom meeting chat mesajları
 * - Teams conversation mesajları
 * 
 * İş Akışı:
 * 1. Discord/Zoom bot mesaj alır
 * 2. Message entity oluşturulur
 * 3. processAndSaveMessage() çağrılır
 * 4. Transaction içinde mesaj ve event kaydedilir
 * 5. OutboxEventRelayer Kafka'ya gönderir
 * 6. AI servisi mesajı alır ve analiz eder
 * 
 * @author Ahmet
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * Mesajı işler, veritabanına kaydeder ve MessageCreated event'i oluşturur.
     * 
     * Transaction içinde hem message hem de outbox event atomik olarak kaydedilir.
     * Bu sayede tutarlılık garantisi sağlanır (ACID properties).
     * 
     * Event, Kafka üzerinden downstream servislere (AI, Gateway) iletilir.
     * AI servisi mesajdan önemli bilgiler çıkarır (action items, sentiment vb.).
     *
     * @param message Kaydedilecek mesaj entity'si (platform, channelId, author, content vs.)
     * @throws org.springframework.dao.DataAccessException Veritabanı hatası durumunda
     */
    @Transactional
    public void processAndSaveMessage(Message message) {
        Message savedMessage = messageRepository.save(message);
        
        outboxEventPublisher.publishCreated(
                savedMessage,
                savedMessage.getId().toString(),
                "Message"
        );
        
        log.debug("Message saved: id={}, author={}, platform={}", 
                savedMessage.getId(), savedMessage.getAuthor(), savedMessage.getPlatform());
    }
}
