package org.example.collector_service.repository;

import org.example.collector_service.domain.model.AudioMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AudioMessageRepository - Ses mesajı veri erişim katmanı
 * 
 * AudioMessage entity için CRUD işlemleri ve özel sorgulamalar sağlar.
 * Sesli oturum bazında ses kayıtlarını filtreleme imkanı sunar.
 * 
 * @author Ahmet
 * @version 1.0
 */
@Repository
public interface AudioMessageRepository extends JpaRepository<AudioMessage, Long> {
    
    /**
     * Belirli bir sesli oturuma ait tüm ses mesajlarını getirir.
     *
     * @param voiceSessionId Sesli oturum kimliği
     * @return Bulunan ses mesajlarının listesi
     */
    List<AudioMessage> findByVoiceSessionId(String voiceSessionId);
}
