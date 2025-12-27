package org.example.collector_service.repository;

import org.example.collector_service.domain.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The interface Meeting repository.
 */
@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    /**
     * Find by platform and meeting id optional.
     *
     * @param platform  the platform
     * @param meetingId the meeting id
     * @return the optional
     */
    Optional<Meeting> findByPlatformAndMeetingId(String platform, String meetingId);
}
