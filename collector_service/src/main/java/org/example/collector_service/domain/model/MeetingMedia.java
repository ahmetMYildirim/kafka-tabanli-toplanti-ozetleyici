package org.example.collector_service.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String meetingId;

    @Column(nullable = false)
    private String platform;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_asset_id", nullable = false)
    private MediaAsset mediaAsset;

    private String meetingTitle;
    private String hostname;
    private LocalDateTime meetingStartTime;
    private LocalDateTime meetingEndTime;
    private Integer participantCount;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;
    private String uploadedBy;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
