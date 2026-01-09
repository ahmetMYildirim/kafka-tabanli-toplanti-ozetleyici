package org.example.ai_service.repository;

import org.example.ai_service.entity.MeetingSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingSummaryRepository extends JpaRepository<MeetingSummaryEntity, Long> {

    Optional<MeetingSummaryEntity> findByMeetingId(Long meetingId);

    List<MeetingSummaryEntity> findByChannelId(String channelId);

    List<MeetingSummaryEntity> findByPlatform(String platform);

    boolean existsByMeetingId(Long meetingId);
}
