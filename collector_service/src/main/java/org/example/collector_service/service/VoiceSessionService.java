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
 * Kullanıcı giriş/çıkış işlemlerini yönetir ve session yaşam döngüsünü kontrol eder.
 * 
 * Özellikler:
 * - Kanal başına aktif session takibi (in-memory ConcurrentHashMap)
 * - Katılımcı sayısı yönetimi (increment/decrement)
 * - Oturum başlatma/sonlandırma event'leri (Outbox pattern ile)
 * 
 * İş Akışı:
 * 1. İlk kullanıcı kanala katıldığında yeni VoiceSession oluşturulur
 * 2. Sonraki kullanıcılar için sadece participantCount artırılır
 * 3. Kullanıcılar ayrıldıkça participantCount azaltılır
 * 4. Son kullanıcı ayrıldığında session sonlandırılır ve event yayınlanır
 * 
 * Thread-Safety: ConcurrentHashMap kullanarak multi-threaded ortamda güvenli çalışır.
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
     * Kullanıcının ses kanalına katılma işlemini yönetir.
     * 
     * İlk kullanıcı ise yeni VoiceSession başlatır ve VoiceSessionStarted event'i oluşturur.
     * Kanal zaten aktifse sadece katılımcı sayısını 1 artırır.
     * 
     * Session key formatı: "{platform}_{channelId}" şeklindedir (örn: "DISCORD_123456789")
     * Bu key ile aynı kanaldaki birden fazla kullanıcı aynı session'a bağlanır.
     *
     * @param platform    Platform adı (DISCORD veya ZOOM)
     * @param channelId   Kanal ID'si (Discord: snowflake ID, Zoom: meeting ID)
     * @param channelName Kanal adı (kullanıcı dostu gösterim için)
     * @param userName    Katılan kullanıcının görünen adı
     * @throws org.springframework.dao.DataAccessException Veritabanı hatası durumunda
     */
    @Transactional
    public void handleUserJoinedVoiceChannel(String platform, String channelId, String channelName, String userName) {
        String sessionKey = buildSessionKey(platform, channelId);

        if (!activeSessions.containsKey(sessionKey)) {
            startNewSession(platform, channelId, channelName, sessionKey);
            log.info("New voice session started: platform={}, channel={}, user={}", 
                    platform, channelName, userName);
        } else {
            incrementParticipantCount(sessionKey);
            log.debug("User joined voice session: channel={}, user={}", channelName, userName);
        }
    }

    /**
     * Kullanıcının ses kanalından ayrılma işlemini yönetir.
     * 
     * Son kullanıcı ise session'ı sonlandırır ve VoiceSessionEnded event'i oluşturur.
     * Kanalda hala kullanıcı varsa sadece katılımcı sayısını 1 azaltır.
     * 
     * Katılımcı sayısı 0 veya daha az olduğunda:
     * - Session endTime'ı set edilir
     * - Veritabanı güncellenir
     * - Memory'den kaldırılır (activeSessions Map'ten)
     * - VoiceSessionEnded event'i Kafka'ya gönderilir
     *
     * @param platform  Platform adı (DISCORD veya ZOOM)
     * @param channelId Kanal ID'si
     * @throws MediaAssetNotFoundException Belirtilen session ID veritabanında bulunamazsa
     * @throws org.springframework.dao.DataAccessException Veritabanı hatası durumunda
     */
    @Transactional
    public void handleUserLeftVoiceChannel(String platform, String channelId) {
        String sessionKey = buildSessionKey(platform, channelId);

        if (activeSessions.containsKey(sessionKey)) {
            Long sessionId = activeSessions.get(sessionKey);
            
            VoiceSession session = voiceSessionRepository.findById(sessionId)
                    .orElseThrow(() -> {
                        log.warn("Active session not found: key={}", sessionKey);
                        activeSessions.remove(sessionKey);
                        return new MediaAssetNotFoundException("Voice session not found with id: " + sessionId);
                    });

            int newCount = session.getParticipantCount() - 1;

            if (newCount <= 0) {
                endSession(session, sessionKey);
                log.info("Voice session ended: platform={}, channel={}",
                        session.getPlatform(), session.getChannelName());
            } else {
                session.setParticipantCount(newCount);
                voiceSessionRepository.save(session);
                log.debug("User left voice session: channel={}, remaining={}",
                        session.getChannelName(), newCount);
            }
        }
    }

    /**
     * Yeni bir sesli oturum başlatır ve VoiceSessionStarted event'i yayınlar.
     * 
     * VoiceSession entity oluşturulur, veritabanına kaydedilir ve 
     * activeSessions Map'ine eklenir. Bu sayede sonraki kullanıcı katılımlarında
     * aynı session'a bağlanabilir.
     * 
     * @param platform Platform adı (DISCORD veya ZOOM)
     * @param channelId Kanal ID'si
     * @param channelName Kanal adı
     * @param sessionKey Benzersiz session anahtarı (platform_channelId formatında)
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
     * Belirtilen session'ın katılımcı sayısını 1 artırır.
     * 
     * Mevcut session veritabanından okunur, participantCount alanı artırılır
     * ve tekrar kaydedilir.
     * 
     * @param sessionKey Güncellenecek session'ın benzersiz anahtarı
     * @throws MediaAssetNotFoundException Session ID veritabanında bulunamazsa
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
     * 
     * Session'ın endTime'ı set edilir, participantCount 0'a çekilir ve
     * activeSessions Map'inden kaldırılır. Son olarak VoiceSessionEnded 
     * event'i outbox'a yazılarak Kafka'ya gönderilir.
     * 
     * @param session Sonlandırılacak VoiceSession entity'si
     * @param sessionKey Memory'den kaldırılacak session'ın key'i
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
     * Platform ve kanal ID'sinden benzersiz (unique) session key oluşturur.
     * 
     * Format: "{platform}_{channelId}"
     * Örnek: "DISCORD_123456789" veya "ZOOM_987654321"
     * 
     * Bu key, aynı kanaldaki tüm kullanıcıların aynı VoiceSession'a
     * bağlanmasını sağlar.
     * 
     * @param platform Platform adı (DISCORD veya ZOOM)
     * @param channelId Kanal ID'si
     * @return Benzersiz session anahtarı
     */
    private String buildSessionKey(String platform, String channelId) {
        return platform + "_" + channelId;
    }
}
