package poller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxPoller Unit Tests")
class OutboxPollerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SendResult<String, Object> sendResult;

    @InjectMocks
    private OutboxPoller outboxPoller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboxPoller, "discordMessagesTopic", "discord-messages");
        ReflectionTestUtils.setField(outboxPoller, "discordVoiceTopic", "discord-voice");
        ReflectionTestUtils.setField(outboxPoller, "meetingMediaTopic", "meeting-media");
    }

    @Test
    @DisplayName("Should initialize successfully")
    void shouldInitializeSuccessfully() {
        assertDoesNotThrow(() -> outboxPoller.init());
    }

    @Test
    @DisplayName("Should handle empty outbox")
    void shouldHandleEmptyOutbox() {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should process Message aggregate type")
    void shouldProcessMessageAggregateType() {
        Map<String, Object> event = createEvent(1L, "Message", "MESSAGE_CREATED",
                "{\"channelId\":\"channel-123\",\"content\":\"test\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("discord-messages"), anyString(), any())).thenReturn(future);
        when(jdbcTemplate.update(anyString(), any(Long.class))).thenReturn(1);

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());

        verify(kafkaTemplate).send(eq("discord-messages"), eq("channel-123"), any());
    }

    @Test
    @DisplayName("Should process AudioMessage aggregate type")
    void shouldProcessAudioMessageAggregateType() {
        Map<String, Object> event = createEvent(1L, "AudioMessage", "AUDIO_CHUNK",
                "{\"channelId\":\"voice-channel\",\"data\":\"audio\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("discord-voice"), anyString(), any())).thenReturn(future);

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());

        verify(kafkaTemplate).send(eq("discord-voice"), anyString(), any());
    }

    @Test
    @DisplayName("Should process AudioMess aggregate type")
    void shouldProcessAudioMessAggregateType() {
        Map<String, Object> event = createEvent(1L, "AudioMess", "AUDIO_CHUNK",
                "{\"channelId\":\"voice-channel\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("discord-voice"), anyString(), any())).thenReturn(future);

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());

        verify(kafkaTemplate).send(eq("discord-voice"), anyString(), any());
    }

    @Test
    @DisplayName("Should handle unknown aggregate type with default topic")
    void shouldHandleUnknownAggregateTypeWithDefaultTopic() {
        Map<String, Object> event = createEvent(1L, "UnknownType", "UNKNOWN_EVENT",
                "{\"data\":\"test\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("discord-messages"), anyString(), any())).thenReturn(future);

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());

        verify(kafkaTemplate).send(eq("discord-messages"), anyString(), any());
    }

    @Test
    @DisplayName("Should handle null aggregate type")
    void shouldHandleNullAggregateType() {
        Map<String, Object> event = createEvent(1L, null, "EVENT", "{\"data\":\"test\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq("discord-messages"), anyString(), any())).thenReturn(future);

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());
    }

    @Test
    @DisplayName("Should extract channelId from payload for key")
    void shouldExtractChannelIdFromPayloadForKey() {
        Map<String, Object> event = createEvent(1L, "Message", "MESSAGE_CREATED",
                "{\"channelId\":\"extracted-channel\",\"content\":\"test\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), eq("extracted-channel"), any())).thenReturn(future);

        outboxPoller.pollOutbox();

        verify(kafkaTemplate).send(anyString(), eq("extracted-channel"), any());
    }

    @Test
    @DisplayName("Should generate fallback key when channelId not found")
    void shouldGenerateFallbackKeyWhenChannelIdNotFound() {
        Map<String, Object> event = createEvent(1L, "Message", "MESSAGE_CREATED",
                "{\"content\":\"no channel id here\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), argThat(key -> key.startsWith("Message-")), any()))
                .thenReturn(future);

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());
    }

    @Test
    @DisplayName("Should mark event as processed on successful send")
    void shouldMarkEventAsProcessedOnSuccessfulSend() {
        Map<String, Object> event = createEvent(1L, "Message", "MESSAGE_CREATED",
                "{\"channelId\":\"channel-123\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(1);

        outboxPoller.pollOutbox();

        verify(jdbcTemplate).update(contains("UPDATE outbox SET is_processed = 1"), eq(1L));
    }

    @Test
    @DisplayName("Should not mark event as processed on send failure")
    void shouldNotMarkEventAsProcessedOnSendFailure() {
        Map<String, Object> event = createEvent(1L, "Message", "MESSAGE_CREATED",
                "{\"channelId\":\"channel-123\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        outboxPoller.pollOutbox();

        verify(jdbcTemplate, never()).update(contains("UPDATE outbox SET is_processed = 1"), anyLong());
    }

    @Test
    @DisplayName("Should process multiple events")
    void shouldProcessMultipleEvents() {
        List<Map<String, Object>> events = Arrays.asList(
                createEvent(1L, "Message", "MESSAGE_CREATED", "{\"channelId\":\"ch-1\"}"),
                createEvent(2L, "Message", "MESSAGE_CREATED", "{\"channelId\":\"ch-2\"}"),
                createEvent(3L, "AudioMessage", "AUDIO_CHUNK", "{\"channelId\":\"ch-3\"}")
        );

        when(jdbcTemplate.queryForList(anyString())).thenReturn(events);

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(1);

        outboxPoller.pollOutbox();

        verify(kafkaTemplate, times(3)).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle database exception gracefully")
    void shouldHandleDatabaseExceptionGracefully() {
        when(jdbcTemplate.queryForList(anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());
    }

    @Test
    @DisplayName("Should handle Kafka exception gracefully")
    void shouldHandleKafkaExceptionGracefully() {
        Map<String, Object> event = createEvent(1L, "Message", "MESSAGE_CREATED",
                "{\"channelId\":\"channel-123\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka exception"));

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());
    }

    @Test
    @DisplayName("Should handle mark as processed failure")
    void shouldHandleMarkAsProcessedFailure() {
        Map<String, Object> event = createEvent(1L, "Message", "MESSAGE_CREATED",
                "{\"channelId\":\"channel-123\"}");

        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
        when(jdbcTemplate.update(anyString(), anyLong()))
                .thenThrow(new RuntimeException("Update failed"));

        assertDoesNotThrow(() -> outboxPoller.pollOutbox());
    }

    private Map<String, Object> createEvent(Long id, String aggregateType, String eventType, String payload) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("aggregate_type", aggregateType);
        event.put("event_type", eventType);
        event.put("payload", payload);
        return event;
    }
}

