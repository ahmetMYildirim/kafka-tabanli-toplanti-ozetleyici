package consumer;

import model.MeetingMediaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingMediaConsumer Unit Tests")
class MeetingMediaConsumerTest {

    @InjectMocks
    private MeetingMediaConsumer meetingMediaConsumer;

    private MeetingMediaEvent mediaEvent;

    @BeforeEach
    void setUp() {
        mediaEvent = MeetingMediaEvent.builder()
                .eventId("event-123")
                .meetingId("meeting-456")
                .fileKey("zoom_abc123")
                .storagePath("/storage/zoom/file.mp4")
                .mimeType("video/mp4")
                .fileSize(1024000L)
                .platform("ZOOM")
                .meetingTitle("Team Standup")
                .hostName("John Doe")
                .timestamp(Instant.now())
                .eventType(MeetingMediaEvent.EventType.MEDIA_UPLOADED)
                .status(MeetingMediaEvent.ProcessingStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should consume meeting media event successfully")
    void shouldConsumeMeetingMediaEventSuccessfully() {
        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle MEDIA_UPLOADED event type")
    void shouldHandleMediaUploadedEventType() {
        mediaEvent.setEventType(MeetingMediaEvent.EventType.MEDIA_UPLOADED);

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle MEDIA_PROCESSING event type")
    void shouldHandleMediaProcessingEventType() {
        mediaEvent.setEventType(MeetingMediaEvent.EventType.MEDIA_PROCESSING);

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle MEDIA_PROCESSED event type")
    void shouldHandleMediaProcessedEventType() {
        mediaEvent.setEventType(MeetingMediaEvent.EventType.MEDIA_PROCESSED);

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle MEDIA_FAILED event type")
    void shouldHandleMediaFailedEventType() {
        mediaEvent.setEventType(MeetingMediaEvent.EventType.MEDIA_FAILED);

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle TRANSCRIPTION_STARTED event type")
    void shouldHandleTranscriptionStartedEventType() {
        mediaEvent.setEventType(MeetingMediaEvent.EventType.TRANSCRIPTION_STARTED);

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle TRANSCRIPTION_COMPLETED event type")
    void shouldHandleTranscriptionCompletedEventType() {
        mediaEvent.setEventType(MeetingMediaEvent.EventType.TRANSCRIPTION_COMPLETED);

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle SUMMARY_GENERATED event type")
    void shouldHandleSummaryGeneratedEventType() {
        mediaEvent.setEventType(MeetingMediaEvent.EventType.SUMMARY_GENERATED);

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle null event type gracefully")
    void shouldHandleNullEventTypeGracefully() {
        mediaEvent.setEventType(null);

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle ZOOM platform")
    void shouldHandleZoomPlatform() {
        mediaEvent.setPlatform("ZOOM");

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle TEAMS platform")
    void shouldHandleTeamsPlatform() {
        mediaEvent.setPlatform("TEAMS");

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle GOOGLE_MEET platform")
    void shouldHandleGoogleMeetPlatform() {
        mediaEvent.setPlatform("GOOGLE_MEET");

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle WEBEX platform")
    void shouldHandleWebexPlatform() {
        mediaEvent.setPlatform("WEBEX");

        assertDoesNotThrow(() ->
                meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle different partitions")
    void shouldHandleDifferentPartitions() {
        assertDoesNotThrow(() -> {
            meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 0, 100L, "key-1");
            meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 1, 101L, "key-2");
            meetingMediaConsumer.consumeMeetingMediaEvent(mediaEvent, 2, 102L, "key-3");
        });
    }
}

