package org.example.collector_service.service;

import org.example.collector_service.domain.model.AudioMessage;
import org.example.collector_service.exception.MediaAssetNotFoundException;
import org.example.collector_service.outbox.OutboxEventPublisher;
import org.example.collector_service.repository.AudioMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AudioMessageService - Ses mesajı yönetim servisi
 * 
 * Discord ve Zoom'dan gelen ses kayıtlarını ve transkriptlerini yönetir.
 * Ses dosyalarını kaydeder ve outbox event'leri oluşturur.
 * 
 * Sorumluluklar:
 * - Ses kaydı persistance (kalıcı hale getirme)
 * - Transkript güncelleme
 * - Outbox event yayınlama
 * 
 * İş Akışı:
 * 1. Discord/Zoom bot'tan ses kaydı alınır
 * 2. AudioMessage entity veritabanına kaydedilir
 * 3. OutboxEventPublisher ile Kafka'ya event gönderilir
 * 4. AI servisi event'i alır ve transkripsiyon yapar
 * 
 * @author Ahmet
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AudioMessageService {
    
    private final AudioMessageRepository audioMessageRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    /**
     * Ses mesajını işler ve veritabanına kaydeder, ardından AudioMessageCreated event'i oluşturur.
     * 
     * Bu metod transactional olarak çalışır; hem veritabanı kaydı hem de outbox event
     * aynı transaction içinde gerçekleşir. Bu sayede tutarlılık garantisi sağlanır.
     * 
     * @param audioMessage Kaydedilecek ses mesajı (platform, channelId, author, audioUrl gibi bilgileri içerir)
     * @throws org.springframework.dao.DataAccessException Veritabanı hatası durumunda
     */
    @Transactional
    public void processAndSaveAudioMessage(AudioMessage audioMessage) {
        AudioMessage saved = audioMessageRepository.save(audioMessage);
        
        outboxEventPublisher.publishCreated(
                saved,
                saved.getId().toString(),
                "AudioMessage"
        );
        
        log.debug("Audio message saved: id={}, author={}", saved.getId(), saved.getAuthor());
    }

    /**
     * Ses mesajının transkriptini günceller ve AudioMessageUpdated event'i oluşturur.
     * 
     * AI servisi tarafından transkripsiyon tamamlandığında bu metod çağrılır.
     * Mevcut ses mesajı bulunur, transcription alanı güncellenir ve 
     * güncelleme event'i Kafka'ya gönderilir.
     * 
     * @param audioMessage Güncellenecek ses mesajı (id, transcription ve audioUrl alanları zorunlu)
     * @throws MediaAssetNotFoundException Belirtilen ID ile ses mesajı bulunamazsa
     * @throws org.springframework.dao.DataAccessException Veritabanı hatası durumunda
     */
    @Transactional
    public void updateAudioMessage(AudioMessage audioMessage) {
        AudioMessage existing = audioMessageRepository.findById(audioMessage.getId())
                .orElseThrow(() -> new MediaAssetNotFoundException("Audio message not found with id: " + audioMessage.getId()));

        existing.setTranscription(audioMessage.getTranscription());
        existing.setAudioUrl(audioMessage.getAudioUrl());

        AudioMessage updated = audioMessageRepository.save(existing);

        outboxEventPublisher.publishUpdated(
                updated,
                updated.getId().toString(),
                "AudioMessage"
        );

        log.debug("Audio message updated: id={}", updated.getId());
    }
}
