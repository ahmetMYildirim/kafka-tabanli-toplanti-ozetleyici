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
 * - Ses kaydı persistance
 * - Transkript güncelleme
 * - Outbox event yayınlama
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
     * Ses mesajını kaydeder ve AudioMessageCreated event'i oluşturur.
     * 
     * @param audioMessage Kaydedilecek ses mesajı
     */
    @Transactional
    public void processAndSaveAudioMessage(AudioMessage audioMessage) {
        AudioMessage saved = audioMessageRepository.save(audioMessage);
        
        outboxEventPublisher.publishCreated(
                saved,
                saved.getId().toString(),
                "AudioMessage"
        );
        
        log.debug("Ses mesajı kaydedildi: id={}, author={}", saved.getId(), saved.getAuthor());
    }

    /**
     * Ses mesajının transkriptini günceller ve AudioMessageUpdated event'i oluşturur.
     * 
     * @param audioMessage Güncellenecek ses mesajı (transcription ve audioUrl alanları)
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

        log.debug("Ses mesajı güncellendi: id={}", updated.getId());
    }
}
