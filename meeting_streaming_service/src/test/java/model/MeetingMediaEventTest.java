package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MeetingMediaEvent Unit Tests")
class MeetingMediaEventTest {

    @Test
    @DisplayName("Should create event using builder pattern")
    void shouldCreateEventUsingBuilder() {
        Instant now = Instant.now();
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        MeetingMediaEvent event = MeetingMediaEvent.builder()
                .eventId("event-123")
                .meetingId("meeting-456")
                .fileKey("zoom_abc123")
                .storagePath("/storage/zoom/file.mp4")
                .mimeType("video/mp4")
                .fileSize(1024000L)
                .platform("ZOOM")
                .meetingTitle("Team Standup")
                .hostName("John Doe")
                .uploadedBy("admin")
                .originalFileName("recording.mp4")
                .checksum("sha256hash")
                .participantCount(5)
                .meetingStartTime(startTime)
                .meetingEndTime(endTime)
                .timestamp(now)
                .eventType(MeetingMediaEvent.EventType.MEDIA_UPLOADED)
                .status(MeetingMediaEvent.ProcessingStatus.PENDING)
                .build();

        assertEquals("event-123", event.getEventId());
        assertEquals("meeting-456", event.getMeetingId());
        assertEquals("ZOOM", event.getPlatform());
        assertEquals(MeetingMediaEvent.EventType.MEDIA_UPLOADED, event.getEventType());
        assertEquals(MeetingMediaEvent.ProcessingStatus.PENDING, event.getStatus());
    }

    @Test
    @DisplayName("Should have all event types defined")
    void shouldHaveAllEventTypesDefined() {
        MeetingMediaEvent.EventType[] eventTypes = MeetingMediaEvent.EventType.values();

        assertEquals(7, eventTypes.length);
        assertNotNull(MeetingMediaEvent.EventType.valueOf("MEDIA_UPLOADED"));
        assertNotNull(MeetingMediaEvent.EventType.valueOf("MEDIA_PROCESSING"));
        assertNotNull(MeetingMediaEvent.EventType.valueOf("MEDIA_PROCESSED"));
        assertNotNull(MeetingMediaEvent.EventType.valueOf("MEDIA_FAILED"));
        assertNotNull(MeetingMediaEvent.EventType.valueOf("TRANSCRIPTION_STARTED"));
        assertNotNull(MeetingMediaEvent.EventType.valueOf("TRANSCRIPTION_COMPLETED"));
        assertNotNull(MeetingMediaEvent.EventType.valueOf("SUMMARY_GENERATED"));
    }

    @Test
    @DisplayName("Should have all processing statuses defined")
    void shouldHaveAllProcessingStatusesDefined() {
        MeetingMediaEvent.ProcessingStatus[] statuses = MeetingMediaEvent.ProcessingStatus.values();

        assertEquals(4, statuses.length);
        assertNotNull(MeetingMediaEvent.ProcessingStatus.valueOf("PENDING"));
        assertNotNull(MeetingMediaEvent.ProcessingStatus.valueOf("PROCESSING"));
        assertNotNull(MeetingMediaEvent.ProcessingStatus.valueOf("COMPLETED"));
        assertNotNull(MeetingMediaEvent.ProcessingStatus.valueOf("FAILED"));
    }

    @Test
    @DisplayName("Should have all platforms defined")
    void shouldHaveAllPlatformsDefined() {
        MeetingMediaEvent.Platform[] platforms = MeetingMediaEvent.Platform.values();

        assertEquals(6, platforms.length);
        assertNotNull(MeetingMediaEvent.Platform.valueOf("ZOOM"));
        assertNotNull(MeetingMediaEvent.Platform.valueOf("TEAMS"));
        assertNotNull(MeetingMediaEvent.Platform.valueOf("GOOGLE_MEET"));
        assertNotNull(MeetingMediaEvent.Platform.valueOf("DISCORD"));
        assertNotNull(MeetingMediaEvent.Platform.valueOf("WEBEX"));
        assertNotNull(MeetingMediaEvent.Platform.valueOf("OTHER"));
    }

    @Test
    @DisplayName("Should create event using no-args constructor")
    void shouldCreateEventUsingNoArgsConstructor() {
        MeetingMediaEvent event = new MeetingMediaEvent();

        assertNull(event.getEventId());
        assertNull(event.getMeetingId());
        assertNull(event.getEventType());
    }

    @Test
    @DisplayName("Should support setter methods")
    void shouldSupportSetterMethods() {
        MeetingMediaEvent event = new MeetingMediaEvent();

        event.setEventId("new-event");
        event.setMeetingId("new-meeting");
        event.setPlatform("TEAMS");
        event.setEventType(MeetingMediaEvent.EventType.MEDIA_PROCESSING);

        assertEquals("new-event", event.getEventId());
        assertEquals("new-meeting", event.getMeetingId());
        assertEquals("TEAMS", event.getPlatform());
        assertEquals(MeetingMediaEvent.EventType.MEDIA_PROCESSING, event.getEventType());
    }

    @Test
    @DisplayName("Should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
        MeetingMediaEvent event1 = MeetingMediaEvent.builder()
                .eventId("event-123")
                .meetingId("meeting-456")
                .platform("ZOOM")
                .build();

        MeetingMediaEvent event2 = MeetingMediaEvent.builder()
                .eventId("event-123")
                .meetingId("meeting-456")
                .platform("ZOOM")
                .build();

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString")
    void shouldImplementToString() {
        MeetingMediaEvent event = MeetingMediaEvent.builder()
                .eventId("event-123")
                .platform("GOOGLE_MEET")
                .build();

        String toString = event.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("event-123"));
    }
}

