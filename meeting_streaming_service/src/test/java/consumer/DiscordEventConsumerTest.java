package consumer;

import model.DiscordMessageEvent;
import model.DiscordVoiceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscordEventConsumer Unit Tests")
class DiscordEventConsumerTest {

    @InjectMocks
    private DiscordEventConsumer discordEventConsumer;

    private DiscordVoiceEvent voiceEvent;
    private DiscordMessageEvent messageEvent;

    @BeforeEach
    void setUp() {
        voiceEvent = DiscordVoiceEvent.builder()
                .eventId("voice-123")
                .guildId("guild-456")
                .channelId("channel-789")
                .userId("user-111")
                .userName("TestUser")
                .audioData(new byte[]{1, 2, 3, 4, 5})
                .audioFormat("PCM")
                .sampleRate(48000)
                .timestamp(Instant.now())
                .eventType(DiscordVoiceEvent.EventType.VOICE_CHUNK)
                .build();

        messageEvent = DiscordMessageEvent.builder()
                .messageId("msg-123")
                .guildId("guild-456")
                .channelId("channel-789")
                .channelName("general")
                .authorId("author-111")
                .authorName("TestAuthor")
                .content("Hello, this is a test message")
                .attachmentUrls(List.of())
                .mentionedUserIds(List.of())
                .timestamp(Instant.now())
                .isEdited(false)
                .build();
    }

    @Test
    @DisplayName("Should consume voice event successfully")
    void shouldConsumeVoiceEventSuccessfully() {
        assertDoesNotThrow(() ->
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should consume message event successfully")
    void shouldConsumeMessageEventSuccessfully() {
        assertDoesNotThrow(() ->
            discordEventConsumer.consumeMessageEvent(messageEvent, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle VOICE_CHUNK event type")
    void shouldHandleVoiceChunkEventType() {
        voiceEvent.setEventType(DiscordVoiceEvent.EventType.VOICE_CHUNK);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle JOIN event type")
    void shouldHandleJoinEventType() {
        voiceEvent.setEventType(DiscordVoiceEvent.EventType.JOIN);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle LEAVE event type")
    void shouldHandleLeaveEventType() {
        voiceEvent.setEventType(DiscordVoiceEvent.EventType.LEAVE);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle MUTE event type")
    void shouldHandleMuteEventType() {
        voiceEvent.setEventType(DiscordVoiceEvent.EventType.MUTE);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle UNMUTE event type")
    void shouldHandleUnmuteEventType() {
        voiceEvent.setEventType(DiscordVoiceEvent.EventType.UNMUTE);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 0, 100L, "test-key")
        );
    }


    @Test
    @DisplayName("Should consume batch voice events successfully")
    void shouldConsumeBatchVoiceEventsSuccessfully() {
        DiscordVoiceEvent event1 = DiscordVoiceEvent.builder()
                .eventId("event-1")
                .eventType(DiscordVoiceEvent.EventType.JOIN)
                .build();

        DiscordVoiceEvent event2 = DiscordVoiceEvent.builder()
                .eventId("event-2")
                .eventType(DiscordVoiceEvent.EventType.VOICE_CHUNK)
                .audioData(new byte[]{1, 2, 3})
                .build();

        List<DiscordVoiceEvent> events = Arrays.asList(event1, event2);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeBatchVoiceEvents(events)
        );
    }

    @Test
    @DisplayName("Should handle empty batch events")
    void shouldHandleEmptyBatchEvents() {
        List<DiscordVoiceEvent> emptyEvents = List.of();

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeBatchVoiceEvents(emptyEvents)
        );
    }

    @Test
    @DisplayName("Should handle message with null content")
    void shouldHandleMessageWithNullContent() {
        messageEvent.setContent(null);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeMessageEvent(messageEvent, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle message with long content")
    void shouldHandleMessageWithLongContent() {
        String longContent = "A".repeat(200);
        messageEvent.setContent(longContent);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeMessageEvent(messageEvent, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle voice event with null audio data")
    void shouldHandleVoiceEventWithNullAudioData() {
        voiceEvent.setAudioData(null);
        voiceEvent.setEventType(DiscordVoiceEvent.EventType.VOICE_CHUNK);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 0, 100L, "test-key")
        );
    }

    @Test
    @DisplayName("Should handle different partitions")
    void shouldHandleDifferentPartitions() {
        assertDoesNotThrow(() -> {
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 0, 100L, "key-1");
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 1, 101L, "key-2");
            discordEventConsumer.consumeVoiceEvent(voiceEvent, 2, 102L, "key-3");
        });
    }

    @Test
    @DisplayName("Should handle edited message")
    void shouldHandleEditedMessage() {
        messageEvent.setEdited(true);

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeMessageEvent(messageEvent, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle message with attachments")
    void shouldHandleMessageWithAttachments() {
        messageEvent.setAttachmentUrls(Arrays.asList("http://example.com/file1.png", "http://example.com/file2.jpg"));

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeMessageEvent(messageEvent, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle message with mentions")
    void shouldHandleMessageWithMentions() {
        messageEvent.setMentionedUserIds(Arrays.asList("user-1", "user-2", "user-3"));

        assertDoesNotThrow(() ->
            discordEventConsumer.consumeMessageEvent(messageEvent, 0, 100L)
        );
    }
}

