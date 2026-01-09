package repository;

import entity.TranscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TranscriptionRepository extends JpaRepository<TranscriptionEntity, Long> {
    
    Optional<TranscriptionEntity> findByMeetingId(Long meetingId);
    
    List<TranscriptionEntity> findAllByMeetingId(Long meetingId);
}

