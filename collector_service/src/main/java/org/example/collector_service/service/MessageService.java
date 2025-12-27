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
 * - Mesaj persistance
 * - Outbox event yayınlama (OutboxEventPublisher aracılığıyla)
 * - Transaction yönetimi
 * 
 * Pattern: Transactional Service + Outbox Pattern
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
     * Mesajı veritabanına kaydeder ve MessageCreated event'i oluşturur.
     * Transaction içinde hem message hem de outbox event atomik olarak kaydedilir.
     *
     * @param message Kaydedilecek mesaj entity
     */
    @Transactional
    public void processAndSaveMessage(Message message) {
        Message savedMessage = messageRepository.save(message);
        
        outboxEventPublisher.publishCreated(
                savedMessage,
                savedMessage.getId().toString(),
                "Message"
        );
        
        log.debug("Mesaj kaydedildi: id={}, author={}, platform={}", 
                savedMessage.getId(), savedMessage.getAuthor(), savedMessage.getPlatform());
    }
}
