package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DiscordMessageEvent Unit Tests")
class DiscordMessageEventTest {

    @Test
    @DisplayName("Should create event using builder pattern")
    void shouldCreateEventUsingBuilder() {
        Instant now = Instant.now();
        List<String> attachments = Arrays.asList("url1", "url2");
        List<String> mentions = Arrays.asList("user1", "user2");

        DiscordMessageEvent event = DiscordMessageEvent.builder()
                .messageId("msg-123")
                .guildId("guild-456")
                .channelId("channel-789")
                .channelName("general")
                .authorId("author-111")
                .authorName("TestUser")
                .content("Hello World")
                .attachmentUrls(attachments)
                .mentionedUserIds(mentions)
                .timestamp(now)
                .isEdited(false)
                .build();

        assertEquals("msg-123", event.getMessageId());
        assertEquals("guild-456", event.getGuildId());
        assertEquals("channel-789", event.getChannelId());
        assertEquals("general", event.getChannelName());
        assertEquals("author-111", event.getAuthorId());
        assertEquals("TestUser", event.getAuthorName());
        assertEquals("Hello World", event.getContent());
        assertEquals(attachments, event.getAttachmentUrls());
        assertEquals(mentions, event.getMentionedUserIds());
        assertEquals(now, event.getTimestamp());
        assertFalse(event.isEdited());
    }

    @Test
    @DisplayName("Should create event using no-args constructor")
    void shouldCreateEventUsingNoArgsConstructor() {
        DiscordMessageEvent event = new DiscordMessageEvent();

        assertNull(event.getMessageId());
        assertNull(event.getContent());
        assertFalse(event.isEdited());
    }

    @Test
    @DisplayName("Should support setter methods")
    void shouldSupportSetterMethods() {
        DiscordMessageEvent event = new DiscordMessageEvent();

        event.setMessageId("msg-new");
        event.setChannelId("channel-new");
        event.setContent("New content");
        event.setEdited(true);

        assertEquals("msg-new", event.getMessageId());
        assertEquals("channel-new", event.getChannelId());
        assertEquals("New content", event.getContent());
        assertTrue(event.isEdited());
    }

    @Test
    @DisplayName("Should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
        DiscordMessageEvent event1 = DiscordMessageEvent.builder()
                .messageId("msg-123")
                .channelId("channel-456")
                .build();

        DiscordMessageEvent event2 = DiscordMessageEvent.builder()
                .messageId("msg-123")
                .channelId("channel-456")
                .build();

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString")
    void shouldImplementToString() {
        DiscordMessageEvent event = DiscordMessageEvent.builder()
                .messageId("msg-123")
                .content("Test message")
                .build();

        String toString = event.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("msg-123"));
    }

    @Test
    @DisplayName("Should handle empty attachment and mention lists")
    void shouldHandleEmptyLists() {
        DiscordMessageEvent event = DiscordMessageEvent.builder()
                .messageId("msg-123")
                .attachmentUrls(List.of())
                .mentionedUserIds(List.of())
                .build();

        assertNotNull(event.getAttachmentUrls());
        assertNotNull(event.getMentionedUserIds());
        assertTrue(event.getAttachmentUrls().isEmpty());
        assertTrue(event.getMentionedUserIds().isEmpty());
    }
}

