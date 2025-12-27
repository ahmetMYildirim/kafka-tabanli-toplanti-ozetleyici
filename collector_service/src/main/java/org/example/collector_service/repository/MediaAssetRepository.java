package org.example.collector_service.repository;

import org.example.collector_service.domain.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    Optional<MediaAsset> findByFileKey(String fileKey);
    boolean existsByChecksum(String checksum);
}
