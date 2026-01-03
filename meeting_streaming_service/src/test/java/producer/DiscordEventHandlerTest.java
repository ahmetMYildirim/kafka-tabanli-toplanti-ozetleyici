package producer;

import model.DiscordMessageEvent;
import model.DiscordVoiceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DiscordEventHandler Unit Tests")
class DiscordEventHandlerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SendResult<String, Object> sendResult;

    @InjectMocks
    private DiscordEventHandler discordEventHandler;

    private DiscordVoiceEvent voiceEvent;
    private DiscordMessageEvent messageEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(discordEventHandler, "voiceTopic", "discord-voice");
        ReflectionTestUtils.setField(discordEventHandler, "messageTopic", "discord-messages");

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
    @DisplayName("Should send voice event successfully")
    void shouldSendVoiceEventSuccessfully() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(100L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(eq("discord-voice"), any(String.class), any(DiscordVoiceEvent.class)))
                .thenReturn(future);

        assertDoesNotThrow(() -> discordEventHandler.sendVoiceEvent(voiceEvent));

        verify(kafkaTemplate, times(1)).send(eq("discord-voice"), eq("guild-456-channel-789"), eq(voiceEvent));
    }

    @Test
    @DisplayName("Should send message event successfully")
    void shouldSendMessageEventSuccessfully() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(eq("discord-messages"), any(String.class), any(DiscordMessageEvent.class)))
                .thenReturn(future);

        assertDoesNotThrow(() -> discordEventHandler.sendMessageEvent(messageEvent));

        verify(kafkaTemplate, times(1)).send(eq("discord-messages"), eq("guild-456-channel-789"), eq(messageEvent));
    }

    @Test
    @DisplayName("Should generate correct key for voice event")
    void shouldGenerateCorrectKeyForVoiceEvent() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(100L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        discordEventHandler.sendVoiceEvent(voiceEvent);

        String expectedKey = voiceEvent.getGuildId() + "-" + voiceEvent.getChannelId();
        verify(kafkaTemplate).send(any(String.class), eq(expectedKey), any());
    }

    @Test
    @DisplayName("Should generate correct key for message event")
    void shouldGenerateCorrectKeyForMessageEvent() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        discordEventHandler.sendMessageEvent(messageEvent);

        String expectedKey = messageEvent.getGuildId() + "-" + messageEvent.getChannelId();
        verify(kafkaTemplate).send(any(String.class), eq(expectedKey), any());
    }

    @Test
    @DisplayName("Should handle send failure for voice event")
    void shouldHandleSendFailureForVoiceEvent() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));

        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        assertDoesNotThrow(() -> discordEventHandler.sendVoiceEvent(voiceEvent));
    }

    @Test
    @DisplayName("Should handle send failure for message event")
    void shouldHandleSendFailureForMessageEvent() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));

        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        assertDoesNotThrow(() -> discordEventHandler.sendMessageEvent(messageEvent));
    }

    @Test
    @DisplayName("Should send voice event with different event types")
    void shouldSendVoiceEventWithDifferentEventTypes() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(100L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        for (DiscordVoiceEvent.EventType eventType : DiscordVoiceEvent.EventType.values()) {
            voiceEvent.setEventType(eventType);
            assertDoesNotThrow(() -> discordEventHandler.sendVoiceEvent(voiceEvent));
        }

        verify(kafkaTemplate, times(5)).send(any(String.class), any(String.class), any());
    }

    @Test
    @DisplayName("Should send message event with attachments")
    void shouldSendMessageEventWithAttachments() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        messageEvent.setAttachmentUrls(List.of("http://example.com/file1.png", "http://example.com/file2.jpg"));

        assertDoesNotThrow(() -> discordEventHandler.sendMessageEvent(messageEvent));
    }

    @Test
    @DisplayName("Should send message event with mentions")
    void shouldSendMessageEventWithMentions() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        messageEvent.setMentionedUserIds(List.of("user-1", "user-2"));

        assertDoesNotThrow(() -> discordEventHandler.sendMessageEvent(messageEvent));
    }
}

