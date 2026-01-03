package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessedTranscription Unit Tests")
class ProcessedTranscriptionTest {

    @Test
    @DisplayName("Should create transcription using builder pattern")
    void shouldCreateTranscriptionUsingBuilder() {
        Instant now = Instant.now();

        ProcessedTranscription.TranscriptionSegment segment1 = ProcessedTranscription.TranscriptionSegment.builder()
                .speakerName("John")
                .speakerId("user-1")
                .text("Hello everyone")
                .startTimeMs(0L)
                .endTimeMs(2000L)
                .confidence(0.95)
                .build();

        ProcessedTranscription.TranscriptionSegment segment2 = ProcessedTranscription.TranscriptionSegment.builder()
                .speakerName("Jane")
                .speakerId("user-2")
                .text("Hi John")
                .startTimeMs(2000L)
                .endTimeMs(3500L)
                .confidence(0.92)
                .build();

        List<ProcessedTranscription.TranscriptionSegment> segments = Arrays.asList(segment1, segment2);

        ProcessedTranscription transcription = ProcessedTranscription.builder()
                .meetingId("meeting-123")
                .channelId("channel-456")
                .platform("ZOOM")
                .fullTranscription("John: Hello everyone\nJane: Hi John")
                .segments(segments)
                .language("en")
                .confidence(0.93)
                .durationSeconds(3600L)
                .processedTime(now)
                .build();

        assertEquals("meeting-123", transcription.getMeetingId());
        assertEquals("ZOOM", transcription.getPlatform());
        assertEquals(2, transcription.getSegments().size());
        assertEquals("en", transcription.getLanguage());
        assertEquals(0.93, transcription.getConfidence());
    }

    @Test
    @DisplayName("Should create transcription segment correctly")
    void shouldCreateTranscriptionSegmentCorrectly() {
        ProcessedTranscription.TranscriptionSegment segment = ProcessedTranscription.TranscriptionSegment.builder()
                .speakerName("Speaker1")
                .speakerId("spk-123")
                .text("This is a test")
                .startTimeMs(1000L)
                .endTimeMs(5000L)
                .confidence(0.98)
                .build();

        assertEquals("Speaker1", segment.getSpeakerName());
        assertEquals("spk-123", segment.getSpeakerId());
        assertEquals("This is a test", segment.getText());
        assertEquals(1000L, segment.getStartTimeMs());
        assertEquals(5000L, segment.getEndTimeMs());
        assertEquals(0.98, segment.getConfidence());
    }

    @Test
    @DisplayName("Should create transcription using no-args constructor")
    void shouldCreateTranscriptionUsingNoArgsConstructor() {
        ProcessedTranscription transcription = new ProcessedTranscription();

        assertNull(transcription.getMeetingId());
        assertNull(transcription.getFullTranscription());
    }

    @Test
    @DisplayName("Should support setter methods")
    void shouldSupportSetterMethods() {
        ProcessedTranscription transcription = new ProcessedTranscription();

        transcription.setMeetingId("new-meeting");
        transcription.setPlatform("TEAMS");
        transcription.setLanguage("tr");

        assertEquals("new-meeting", transcription.getMeetingId());
        assertEquals("TEAMS", transcription.getPlatform());
        assertEquals("tr", transcription.getLanguage());
    }

    @Test
    @DisplayName("Should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
        Instant now = Instant.now();

        ProcessedTranscription transcription1 = ProcessedTranscription.builder()
                .meetingId("meeting-123")
                .platform("ZOOM")
                .processedTime(now)
                .build();

        ProcessedTranscription transcription2 = ProcessedTranscription.builder()
                .meetingId("meeting-123")
                .platform("ZOOM")
                .processedTime(now)
                .build();

        assertEquals(transcription1, transcription2);
        assertEquals(transcription1.hashCode(), transcription2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString")
    void shouldImplementToString() {
        ProcessedTranscription transcription = ProcessedTranscription.builder()
                .meetingId("meeting-123")
                .platform("GOOGLE_MEET")
                .build();

        String toString = transcription.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("meeting-123"));
    }
}

