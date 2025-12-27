package org.example.collector_service.repository;

import org.example.collector_service.domain.model.VoiceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * VoiceSessionRepository - Sesli oturum veri erişim katmanı
 * 
 * VoiceSession entity için CRUD işlemleri ve özel sorgulamalar sağlar.
 * Aktif oturum kontrolü ve kanal bazlı filtreleme imkanı sunar.
 * 
 * @author Ahmet
 * @version 1.0
 */
@Repository
public interface VoiceSessionRepository extends JpaRepository<VoiceSession, Long> {
    
    /**
     * Platform ve kanal bazında aktif oturumu bulur.
     * Bitiş zamanı null olan kayıtlar aktif oturum olarak kabul edilir.
     *
     * @param platform  Platform adı (DISCORD veya ZOOM)
     * @param channelId Ses kanalı kimliği
     * @return Aktif oturum varsa döner
     */
    Optional<VoiceSession> findByPlatformAndChannelIdAndEndTimeIsNull(String platform, String channelId);

    /**
     * Belirli bir kanala ait tüm sesli oturumları getirir.
     *
     * @param channelId Ses kanalı kimliği
     * @return Bulunan sesli oturumların listesi
     */
    List<VoiceSession> findByChannelId(String channelId);
}
