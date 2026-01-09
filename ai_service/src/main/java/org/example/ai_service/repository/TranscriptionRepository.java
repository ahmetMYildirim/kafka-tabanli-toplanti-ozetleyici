package org.example.ai_service.repository;

import org.example.ai_service.entity.TranscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Transcription Repository
 */
@Repository
public interface TranscriptionRepository extends JpaRepository<TranscriptionEntity, Long> {

    List<TranscriptionEntity> findByMeetingId(Long meetingId);

    Optional<TranscriptionEntity> findByAudioMessageId(Long audioMessageId);
}

