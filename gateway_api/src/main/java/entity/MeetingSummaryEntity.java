package entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "meeting_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "platform")
    private String platform;

    @Column(name = "title")
    private String title;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "key_points", columnDefinition = "TEXT")
    private String keyPoints;

    @Column(name = "decisions", columnDefinition = "TEXT")
    private String decisions;

    @Column(name = "participants", columnDefinition = "TEXT")
    private String participants;

    @Column(name = "duration_minutes")
    private Long durationMinutes;

    @Column(name = "meeting_date")
    private LocalDateTime meetingDate;

    @Column(name = "processed_time")
    private LocalDateTime processedTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

