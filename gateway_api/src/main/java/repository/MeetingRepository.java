package repository;

import entity.MeetingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {
    
    Optional<MeetingEntity> findByExternalId(String externalId);
    
    List<MeetingEntity> findByPlatform(String platform, Pageable pageable);
    
    @Query("SELECT m FROM MeetingEntity m ORDER BY m.createdAt DESC")
    List<MeetingEntity> findLatestMeetings(Pageable pageable);
}

