package org.example.collector_service.service;

import org.example.collector_service.domain.model.VoiceSession;
import org.example.collector_service.exception.MediaAssetNotFoundException;
import org.example.collector_service.outbox.OutboxEventPublisher;
import org.example.collector_service.repository.VoiceSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * VoiceSessionService - Sesli oturum yönetim servisi
 * 
 * Discord ve Zoom ses kanallarındaki aktif oturumları takip eder.
 * Kullanıcı giriş/çıkış işlemlerini yönetir ve session lifecycle'ı kontrol eder.
 * 
 * Özellikler:
 * - Kanal başına aktif session takibi (in-memory Map)
 * - Katılımcı sayısı yönetimi
 * - Oturum başlatma/sonlandırma event'leri
 * 
 * @author Ahmet
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceSessionService {
    
    private final VoiceSessionRepository voiceSessionRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    private final Map<String, Long> activeSessions = new ConcurrentHashMap<>();

    /**
     * Kullanıcı ses kanalına katıldığında çağrılır.
     * İlk kullanıcı ise yeni session başlatır, değilse katılımcı sayısını artırır.
     *
     * @param platform    Platform adı (DISCORD veya ZOOM)
     * @param channelId   Kanal ID'si
     * @param channelName Kanal adı
     * @param userName    Katılan kullanıcının adı
     */
    @Transactional
    public void handleUserJoinedVoiceChannel(String platform, String channelId, String channelName, String userName) {
        String sessionKey = buildSessionKey(platform, channelId);

        if (!activeSessions.containsKey(sessionKey)) {
            startNewSession(platform, channelId, channelName, sessionKey);
            log.info("Yeni sesli oturum başlatıldı: platform={}, channel={}, user={}", 
                    platform, channelName, userName);
        } else {
            incrementParticipantCount(sessionKey);
            log.debug("Sesli oturuma katılım: channel={}, user={}", channelName, userName);
        }
    }

    /**
     * Kullanıcı ses kanalından ayrıldığında çağrılır.
     * Son kullanıcı ise session'ı sonlandırır, değilse katılımcı sayısını azaltır.
     *
     * @param platform  Platform adı (DISCORD veya ZOOM)
     * @param channelId Kanal ID'si
     */
    @Transactional
    public void handleUserLeftVoiceChannel(String platform, String channelId) {
        String sessionKey = buildSessionKey(platform, channelId);

        if (activeSessions.containsKey(sessionKey)) {
            Long sessionId = activeSessions.get(sessionKey);
            
            VoiceSession session = voiceSessionRepository.findById(sessionId)
                    .orElseThrow(() -> {
                        log.warn("Aktif session bulunamadı: key={}", sessionKey);
                        activeSessions.remove(sessionKey);
                        return new MediaAssetNotFoundException("Voice session not found with id: " + sessionId);
                    });

            int newCount = session.getParticipantCount() - 1;

            if (newCount <= 0) {
                endSession(session, sessionKey);
                log.info("Sesli oturum sonlandı: platform={}, channel={}",
                        session.getPlatform(), session.getChannelName());
            } else {
                session.setParticipantCount(newCount);
                voiceSessionRepository.save(session);
                log.debug("Sesli oturumdan ayrılma: channel={}, kalan={}",
                        session.getChannelName(), newCount);
            }
        }
    }

    /**
     * Yeni bir sesli oturum başlatır ve VoiceSessionStarted event'i yayınlar.
     */
    private void startNewSession(String platform, String channelId, String channelName, String sessionKey) {
        VoiceSession session = VoiceSession.builder()
                .platform(platform)
                .channelId(channelId)
                .channelName(channelName)
                .startTime(LocalDateTime.now())
                .participantCount(1)
                .build();

        VoiceSession saved = voiceSessionRepository.save(session);
        activeSessions.put(sessionKey, saved.getId());

        outboxEventPublisher.publishStarted(
                saved,
                saved.getId().toString(),
                "VoiceSession"
        );
    }

    /**
     * Katılımcı sayısını 1 artırır.
     */
    private void incrementParticipantCount(String sessionKey) {
        Long sessionId = activeSessions.get(sessionKey);
        
        VoiceSession session = voiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new MediaAssetNotFoundException("Voice session not found with id: " + sessionId));

        session.setParticipantCount(session.getParticipantCount() + 1);
        voiceSessionRepository.save(session);
    }

    /**
     * Sesli oturumu sonlandırır ve VoiceSessionEnded event'i yayınlar.
     */
    private void endSession(VoiceSession session, String sessionKey) {
        session.setParticipantCount(0);
        session.setEndTime(LocalDateTime.now());
        
        VoiceSession ended = voiceSessionRepository.save(session);
        activeSessions.remove(sessionKey);

        outboxEventPublisher.publishEnded(
                ended,
                ended.getId().toString(),
                "VoiceSession"
        );
    }

    /**
     * Platform ve kanal ID'sinden unique session key oluşturur.
     */
    private String buildSessionKey(String platform, String channelId) {
        return platform + "_" + channelId;
    }
}
