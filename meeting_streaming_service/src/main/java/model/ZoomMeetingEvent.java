package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoomMeetingEvent {
    private String meetingId;
    private String meetingTopic;
    private String hostId;
    private String participantId;
    private String participantName;
    private EventType eventType;
    private String transcriptionChunk;
    private byte[] audioData;
    private Instant timestamp;

    public enum EventType {
        MEETING_STARTED,
        MEETING_ENDED,
        PARTICIPANT_JOINED,
        PARTICIPANT_LEFT,
        TRANSCRIPTION_CHUNK,
        AUIDO_CHUNK,
        CHAT_MESSAGE
    }
}
