package consumer;

import model.ProcessedMeetingData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessDataConsumer Unit Tests")
class ProcessDataConsumerTest {

    @InjectMocks
    private ProcessDataConsumer processDataConsumer;

    private ProcessedMeetingData meetingData;

    @BeforeEach
    void setUp() {
        meetingData = ProcessedMeetingData.builder()
                .sourceType("DISCORD_MESSAGES")
                .channelId("channel-123")
                .rawContent("User1: Hello\nUser2: Hi there")
                .speakerName("TestSpeaker")
                .speakerId("speaker-456")
                .windowStart(Instant.now().minusSeconds(300))
                .windowEnd(Instant.now())
                .processedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should consume processed message successfully")
    void shouldConsumeProcessedMessageSuccessfully() {
        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessMessage(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should consume processed voice successfully")
    void shouldConsumeProcessedVoiceSuccessfully() {
        meetingData.setSourceType("DISCORD_VOICE");
        meetingData.setAudioData(new byte[]{1, 2, 3, 4, 5});

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessVoice(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle DISCORD_MESSAGES source type")
    void shouldHandleDiscordMessagesSourceType() {
        meetingData.setSourceType("DISCORD_MESSAGES");

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessMessage(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle DISCORD_VOICE source type")
    void shouldHandleDiscordVoiceSourceType() {
        meetingData.setSourceType("DISCORD_VOICE");
        meetingData.setAudioData(new byte[1024]);

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessVoice(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle unknown source type")
    void shouldHandleUnknownSourceType() {
        meetingData.setSourceType("UNKNOWN_TYPE");

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessMessage(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle empty source type as unknown")
    void shouldHandleEmptySourceTypeAsUnknown() {
        meetingData.setSourceType("");

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessMessage(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle null raw content")
    void shouldHandleNullRawContent() {
        meetingData.setRawContent(null);

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessMessage(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle null audio data for voice")
    void shouldHandleNullAudioDataForVoice() {
        meetingData.setSourceType("DISCORD_VOICE");
        meetingData.setAudioData(null);

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessVoice(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle empty raw content")
    void shouldHandleEmptyRawContent() {
        meetingData.setRawContent("");

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessMessage(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle large audio data")
    void shouldHandleLargeAudioData() {
        meetingData.setSourceType("DISCORD_VOICE");
        meetingData.setAudioData(new byte[100000]);

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessVoice(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle different partitions")
    void shouldHandleDifferentPartitions() {
        assertDoesNotThrow(() -> {
            processDataConsumer.consumeProcessMessage(meetingData, 0, 100L);
            processDataConsumer.consumeProcessMessage(meetingData, 1, 101L);
            processDataConsumer.consumeProcessMessage(meetingData, 2, 102L);
        });
    }

    @Test
    @DisplayName("Should handle null speaker name")
    void shouldHandleNullSpeakerName() {
        meetingData.setSpeakerName(null);

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessVoice(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should handle window time correctly")
    void shouldHandleWindowTimeCorrectly() {
        Instant start = Instant.parse("2024-01-01T10:00:00Z");
        Instant end = Instant.parse("2024-01-01T10:05:00Z");

        meetingData.setWindowStart(start);
        meetingData.setWindowEnd(end);

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessMessage(meetingData, 0, 100L)
        );
    }

    @Test
    @DisplayName("Should process long raw content")
    void shouldProcessLongRawContent() {
        String longContent = "A".repeat(10000);
        meetingData.setRawContent(longContent);

        assertDoesNotThrow(() ->
            processDataConsumer.consumeProcessMessage(meetingData, 0, 100L)
        );
    }
}

