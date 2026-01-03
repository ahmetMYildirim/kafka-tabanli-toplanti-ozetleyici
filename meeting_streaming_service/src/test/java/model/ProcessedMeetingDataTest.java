package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessedMeetingData Unit Tests")
class ProcessedMeetingDataTest {

    @Test
    @DisplayName("Should create data using builder pattern")
    void shouldCreateDataUsingBuilder() {
        Instant windowStart = Instant.now().minusSeconds(300);
        Instant windowEnd = Instant.now();
        Instant processedAt = Instant.now();
        byte[] audioData = {1, 2, 3, 4, 5};

        ProcessedMeetingData data = ProcessedMeetingData.builder()
                .sourceType("DISCORD_MESSAGES")
                .channelId("channel-123")
                .rawContent("Test content")
                .audioData(audioData)
                .speakerName("TestUser")
                .speakerId("speaker-456")
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .processedAt(processedAt)
                .build();

        assertEquals("DISCORD_MESSAGES", data.getSourceType());
        assertEquals("channel-123", data.getChannelId());
        assertEquals("Test content", data.getRawContent());
        assertArrayEquals(audioData, data.getAudioData());
        assertEquals("TestUser", data.getSpeakerName());
        assertEquals("speaker-456", data.getSpeakerId());
        assertEquals(windowStart, data.getWindowStart());
        assertEquals(windowEnd, data.getWindowEnd());
        assertEquals(processedAt, data.getProcessedAt());
    }

    @Test
    @DisplayName("Should create data using no-args constructor")
    void shouldCreateDataUsingNoArgsConstructor() {
        ProcessedMeetingData data = new ProcessedMeetingData();

        assertNull(data.getSourceType());
        assertNull(data.getChannelId());
        assertNull(data.getRawContent());
    }

    @Test
    @DisplayName("Should support setter methods")
    void shouldSupportSetterMethods() {
        ProcessedMeetingData data = new ProcessedMeetingData();

        data.setSourceType("DISCORD_VOICE");
        data.setChannelId("new-channel");
        data.setSpeakerName("NewSpeaker");

        assertEquals("DISCORD_VOICE", data.getSourceType());
        assertEquals("new-channel", data.getChannelId());
        assertEquals("NewSpeaker", data.getSpeakerName());
    }

    @Test
    @DisplayName("Should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
        Instant now = Instant.now();

        ProcessedMeetingData data1 = ProcessedMeetingData.builder()
                .sourceType("DISCORD_MESSAGES")
                .channelId("channel-123")
                .windowStart(now)
                .build();

        ProcessedMeetingData data2 = ProcessedMeetingData.builder()
                .sourceType("DISCORD_MESSAGES")
                .channelId("channel-123")
                .windowStart(now)
                .build();

        assertEquals(data1, data2);
        assertEquals(data1.hashCode(), data2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString")
    void shouldImplementToString() {
        ProcessedMeetingData data = ProcessedMeetingData.builder()
                .sourceType("DISCORD_MESSAGES")
                .channelId("channel-123")
                .build();

        String toString = data.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("DISCORD_MESSAGES"));
        assertTrue(toString.contains("channel-123"));
    }

    @Test
    @DisplayName("Should handle DISCORD_MESSAGES source type")
    void shouldHandleDiscordMessagesSourceType() {
        ProcessedMeetingData data = ProcessedMeetingData.builder()
                .sourceType("DISCORD_MESSAGES")
                .rawContent("User1: Hello\nUser2: Hi")
                .build();

        assertEquals("DISCORD_MESSAGES", data.getSourceType());
        assertNotNull(data.getRawContent());
    }

    @Test
    @DisplayName("Should handle DISCORD_VOICE source type")
    void shouldHandleDiscordVoiceSourceType() {
        byte[] audioData = new byte[1024];
        ProcessedMeetingData data = ProcessedMeetingData.builder()
                .sourceType("DISCORD_VOICE")
                .audioData(audioData)
                .speakerName("VoiceUser")
                .build();

        assertEquals("DISCORD_VOICE", data.getSourceType());
        assertEquals(1024, data.getAudioData().length);
    }

    @Test
    @DisplayName("Should handle null audio data")
    void shouldHandleNullAudioData() {
        ProcessedMeetingData data = ProcessedMeetingData.builder()
                .sourceType("DISCORD_MESSAGES")
                .audioData(null)
                .build();

        assertNull(data.getAudioData());
    }

    @Test
    @DisplayName("Should handle window time range")
    void shouldHandleWindowTimeRange() {
        Instant start = Instant.parse("2024-01-01T10:00:00Z");
        Instant end = Instant.parse("2024-01-01T10:05:00Z");

        ProcessedMeetingData data = ProcessedMeetingData.builder()
                .windowStart(start)
                .windowEnd(end)
                .build();

        assertEquals(start, data.getWindowStart());
        assertEquals(end, data.getWindowEnd());
        assertTrue(data.getWindowEnd().isAfter(data.getWindowStart()));
    }
}

