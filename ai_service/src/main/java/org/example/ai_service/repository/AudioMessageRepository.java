package org.example.ai_service.repository;

import org.example.ai_service.entity.AudioMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AudioMessageRepository - collector_service ile paylasilan audio_messages tablosu
 *
 * AI Service bu repository uzerinden transkripsiyon sonuclarini gunceller.
 */
@Repository
public interface AudioMessageRepository extends JpaRepository<AudioMessageEntity, Long> {

    List<AudioMessageEntity> findByVoiceSessionId(String voiceSessionId);

    List<AudioMessageEntity> findByMeetingId(Long meetingId);

    Optional<AudioMessageEntity> findByAudioUrl(String audioUrl);

    /**
     * Transkripsiyon sonucunu gunceller
     * AI Service bu metodu kullanarak transkripsiyon yazar
     */
    @Modifying
    @Query("UPDATE AudioMessageEntity a SET a.transcription = :transcription WHERE a.id = :id")
    int updateTranscription(@Param("id") Long id, @Param("transcription") String transcription);

    /**
     * Transkripsiyon yapilmamis ses kayitlarini getirir
     */
    @Query("SELECT a FROM AudioMessageEntity a WHERE a.transcription IS NULL OR a.transcription = ''")
    List<AudioMessageEntity> findPendingTranscriptions();
}
