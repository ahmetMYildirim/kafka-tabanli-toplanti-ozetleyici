package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingMediaEvent {
    private String eventId;
    private String meetingId;
    private String fileKey;
    private String storagePath;
    private String mimeType;
    private Long fileSize;
    private String platform;
    private String meetingTitle;
    private String hostName;
    private String uploadedBy;
    private String originalFileName;
    private String checksum;
    private Integer participantCount;
    private LocalDateTime meetingStartTime;
    private LocalDateTime meetingEndTime;
    private Instant timestamp;
    private EventType eventType;
    private ProcessingStatus status;

    public enum EventType {
        MEDIA_UPLOADED,
        MEDIA_PROCESSING,
        MEDIA_PROCESSED,
        MEDIA_FAILED,
        TRANSCRIPTION_STARTED,
        TRANSCRIPTION_COMPLETED,
        SUMMARY_GENERATED
    }

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public enum Platform {
        ZOOM,
        TEAMS,
        GOOGLE_MEET,
        DISCORD,
        WEBEX,
        OTHER
    }
}

