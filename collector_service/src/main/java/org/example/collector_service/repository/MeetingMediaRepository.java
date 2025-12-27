package org.example.collector_service.repository;


import org.example.collector_service.domain.model.MeetingMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface MeetingMediaRepository extends JpaRepository<MeetingMedia, Long> {
    List<MeetingMedia> findByMeetingId(String meetingId);
    List<MeetingMedia> findByPlatform(String platform);
}
