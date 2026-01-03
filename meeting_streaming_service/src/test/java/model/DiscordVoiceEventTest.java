package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DiscordVoiceEvent Unit Tests")
class DiscordVoiceEventTest {

    @Test
    @DisplayName("Should create event using builder pattern")
    void shouldCreateEventUsingBuilder() {
        Instant now = Instant.now();
        byte[] audioData = {1, 2, 3, 4, 5};

        DiscordVoiceEvent event = DiscordVoiceEvent.builder()
                .eventId("event-123")
                .guildId("guild-456")
                .channelId("channel-789")
                .userId("user-111")
                .userName("TestUser")
                .audioData(audioData)
                .audioFormat("PCM")
                .sampleRate(48000)
                .timestamp(now)
                .eventType(DiscordVoiceEvent.EventType.VOICE_CHUNK)
                .build();

        assertEquals("event-123", event.getEventId());
        assertEquals("guild-456", event.getGuildId());
        assertEquals("channel-789", event.getChannelId());
        assertEquals("user-111", event.getUserId());
        assertEquals("TestUser", event.getUserName());
        assertArrayEquals(audioData, event.getAudioData());
        assertEquals("PCM", event.getAudioFormat());
        assertEquals(48000, event.getSampleRate());
        assertEquals(now, event.getTimestamp());
        assertEquals(DiscordVoiceEvent.EventType.VOICE_CHUNK, event.getEventType());
    }

    @Test
    @DisplayName("Should create event using no-args constructor")
    void shouldCreateEventUsingNoArgsConstructor() {
        DiscordVoiceEvent event = new DiscordVoiceEvent();

        assertNull(event.getEventId());
        assertNull(event.getEventType());
        assertEquals(0, event.getSampleRate());
    }

    @Test
    @DisplayName("Should have all event types defined")
    void shouldHaveAllEventTypesDefined() {
        DiscordVoiceEvent.EventType[] eventTypes = DiscordVoiceEvent.EventType.values();

        assertEquals(5, eventTypes.length);
        assertNotNull(DiscordVoiceEvent.EventType.valueOf("VOICE_CHUNK"));
        assertNotNull(DiscordVoiceEvent.EventType.valueOf("JOIN"));
        assertNotNull(DiscordVoiceEvent.EventType.valueOf("LEAVE"));
        assertNotNull(DiscordVoiceEvent.EventType.valueOf("MUTE"));
        assertNotNull(DiscordVoiceEvent.EventType.valueOf("UNMUTE"));
    }

    @Test
    @DisplayName("Should support setter methods")
    void shouldSupportSetterMethods() {
        DiscordVoiceEvent event = new DiscordVoiceEvent();

        event.setEventId("new-event");
        event.setChannelId("new-channel");
        event.setEventType(DiscordVoiceEvent.EventType.JOIN);
        event.setSampleRate(44100);

        assertEquals("new-event", event.getEventId());
        assertEquals("new-channel", event.getChannelId());
        assertEquals(DiscordVoiceEvent.EventType.JOIN, event.getEventType());
        assertEquals(44100, event.getSampleRate());
    }

    @Test
    @DisplayName("Should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
        DiscordVoiceEvent event1 = DiscordVoiceEvent.builder()
                .eventId("event-123")
                .channelId("channel-456")
                .eventType(DiscordVoiceEvent.EventType.JOIN)
                .build();

        DiscordVoiceEvent event2 = DiscordVoiceEvent.builder()
                .eventId("event-123")
                .channelId("channel-456")
                .eventType(DiscordVoiceEvent.EventType.JOIN)
                .build();

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString")
    void shouldImplementToString() {
        DiscordVoiceEvent event = DiscordVoiceEvent.builder()
                .eventId("event-123")
                .userId("user-456")
                .build();

        String toString = event.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("event-123"));
    }

    @Test
    @DisplayName("Should handle null audio data")
    void shouldHandleNullAudioData() {
        DiscordVoiceEvent event = DiscordVoiceEvent.builder()
                .eventId("event-123")
                .audioData(null)
                .build();

        assertNull(event.getAudioData());
    }

    @Test
    @DisplayName("Should create JOIN event correctly")
    void shouldCreateJoinEventCorrectly() {
        DiscordVoiceEvent event = DiscordVoiceEvent.builder()
                .eventId("join-event")
                .userId("user-123")
                .channelId("voice-channel")
                .eventType(DiscordVoiceEvent.EventType.JOIN)
                .build();

        assertEquals(DiscordVoiceEvent.EventType.JOIN, event.getEventType());
        assertEquals("user-123", event.getUserId());
    }

    @Test
    @DisplayName("Should create LEAVE event correctly")
    void shouldCreateLeaveEventCorrectly() {
        DiscordVoiceEvent event = DiscordVoiceEvent.builder()
                .eventId("leave-event")
                .userId("user-123")
                .eventType(DiscordVoiceEvent.EventType.LEAVE)
                .build();

        assertEquals(DiscordVoiceEvent.EventType.LEAVE, event.getEventType());
    }

    @Test
    @DisplayName("Should create MUTE event correctly")
    void shouldCreateMuteEventCorrectly() {
        DiscordVoiceEvent event = DiscordVoiceEvent.builder()
                .eventType(DiscordVoiceEvent.EventType.MUTE)
                .build();

        assertEquals(DiscordVoiceEvent.EventType.MUTE, event.getEventType());
    }

    @Test
    @DisplayName("Should create UNMUTE event correctly")
    void shouldCreateUnmuteEventCorrectly() {
        DiscordVoiceEvent event = DiscordVoiceEvent.builder()
                .eventType(DiscordVoiceEvent.EventType.UNMUTE)
                .build();

        assertEquals(DiscordVoiceEvent.EventType.UNMUTE, event.getEventType());
    }
}

