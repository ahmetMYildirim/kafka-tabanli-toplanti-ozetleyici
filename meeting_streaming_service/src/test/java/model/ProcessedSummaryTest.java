package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessedSummary Unit Tests")
class ProcessedSummaryTest {

    @Test
    @DisplayName("Should create summary using builder pattern")
    void shouldCreateSummaryUsingBuilder() {
        Instant now = Instant.now();
        List<String> keyPoints = Arrays.asList("Point 1", "Point 2", "Point 3");
        List<String> participants = Arrays.asList("User1", "User2");

        ProcessedSummary summary = ProcessedSummary.builder()
                .meetingId("meeting-123")
                .channelId("channel-456")
                .platform("ZOOM")
                .title("Weekly Standup")
                .summary("This meeting covered project updates and next steps.")
                .keyPoints(keyPoints)
                .participants(participants)
                .meetingStartTime(now.minusSeconds(3600))
                .meetingEndTime(now)
                .processedTime(now)
                .build();

        assertEquals("meeting-123", summary.getMeetingId());
        assertEquals("channel-456", summary.getChannelId());
        assertEquals("ZOOM", summary.getPlatform());
        assertEquals("Weekly Standup", summary.getTitle());
        assertEquals(3, summary.getKeyPoints().size());
        assertEquals(2, summary.getParticipants().size());
    }

    @Test
    @DisplayName("Should create summary using no-args constructor")
    void shouldCreateSummaryUsingNoArgsConstructor() {
        ProcessedSummary summary = new ProcessedSummary();

        assertNull(summary.getMeetingId());
        assertNull(summary.getSummary());
    }

    @Test
    @DisplayName("Should support setter methods")
    void shouldSupportSetterMethods() {
        ProcessedSummary summary = new ProcessedSummary();

        summary.setMeetingId("new-meeting");
        summary.setPlatform("TEAMS");
        summary.setTitle("New Meeting");

        assertEquals("new-meeting", summary.getMeetingId());
        assertEquals("TEAMS", summary.getPlatform());
        assertEquals("New Meeting", summary.getTitle());
    }

    @Test
    @DisplayName("Should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
        Instant now = Instant.now();

        ProcessedSummary summary1 = ProcessedSummary.builder()
                .meetingId("meeting-123")
                .platform("ZOOM")
                .processedTime(now)
                .build();

        ProcessedSummary summary2 = ProcessedSummary.builder()
                .meetingId("meeting-123")
                .platform("ZOOM")
                .processedTime(now)
                .build();

        assertEquals(summary1, summary2);
        assertEquals(summary1.hashCode(), summary2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString")
    void shouldImplementToString() {
        ProcessedSummary summary = ProcessedSummary.builder()
                .meetingId("meeting-123")
                .title("Test Meeting")
                .build();

        String toString = summary.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("meeting-123"));
    }
}

