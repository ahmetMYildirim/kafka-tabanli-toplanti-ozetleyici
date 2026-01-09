package org.example.ai_service.repository;

import org.example.ai_service.entity.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Meeting Repository - Ana toplanti tablosu
 */
@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {

    Optional<MeetingEntity> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}

