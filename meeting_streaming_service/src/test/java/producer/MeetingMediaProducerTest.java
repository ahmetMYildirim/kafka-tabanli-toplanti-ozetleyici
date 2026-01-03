package producer;

import model.MeetingMediaEvent;
import model.ProcessedActionItem;
import model.ProcessedSummary;
import model.ProcessedTranscription;
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
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MeetingMediaProducer Unit Tests")
class MeetingMediaProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private SendResult<String, Object> sendResult;

    @InjectMocks
    private MeetingMediaProducer meetingMediaProducer;

    private MeetingMediaEvent mediaEvent;
    private ProcessedSummary summary;
    private ProcessedTranscription transcription;
    private ProcessedActionItem actionItems;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(meetingMediaProducer, "meetingMediaTopic", "meeting-media");
        ReflectionTestUtils.setField(meetingMediaProducer, "processedSummaryTopic", "processed-summary");
        ReflectionTestUtils.setField(meetingMediaProducer, "processedTranscriptionTopic", "processed-transcription");
        ReflectionTestUtils.setField(meetingMediaProducer, "processedActionItemsTopic", "processed-action-items");

        mediaEvent = MeetingMediaEvent.builder()
                .eventId("event-123")
                .meetingId("meeting-456")
                .platform("ZOOM")
                .eventType(MeetingMediaEvent.EventType.MEDIA_UPLOADED)
                .build();

        summary = ProcessedSummary.builder()
                .meetingId("meeting-456")
                .platform("ZOOM")
                .title("Team Standup")
                .summary("Weekly team sync meeting")
                .keyPoints(Arrays.asList("Point 1", "Point 2"))
                .build();

        transcription = ProcessedTranscription.builder()
                .meetingId("meeting-456")
                .platform("ZOOM")
                .fullTranscription("Full meeting transcript")
                .language("en")
                .build();

        actionItems = ProcessedActionItem.builder()
                .meetingId("meeting-456")
                .platform("ZOOM")
                .processedTime(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should send meeting media event successfully")
    void shouldSendMeetingMediaEventSuccessfully() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(100L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(eq("meeting-media"), any(String.class), any(MeetingMediaEvent.class)))
                .thenReturn(future);

        assertDoesNotThrow(() -> meetingMediaProducer.sendMeetingMediaEvent(mediaEvent));

        verify(kafkaTemplate, times(1)).send(eq("meeting-media"), eq("ZOOM-meeting-456"), eq(mediaEvent));
    }

    @Test
    @DisplayName("Should send processed summary successfully")
    void shouldSendProcessedSummarySuccessfully() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(eq("processed-summary"), any(String.class), any(ProcessedSummary.class)))
                .thenReturn(future);

        assertDoesNotThrow(() -> meetingMediaProducer.sendProcessedSummary(summary));

        verify(kafkaTemplate, times(1)).send(eq("processed-summary"), eq("ZOOM-meeting-456"), eq(summary));
    }

    @Test
    @DisplayName("Should send processed transcription successfully")
    void shouldSendProcessedTranscriptionSuccessfully() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(eq("processed-transcription"), any(String.class), any(ProcessedTranscription.class)))
                .thenReturn(future);

        assertDoesNotThrow(() -> meetingMediaProducer.sendProcessedTranscription(transcription));

        verify(kafkaTemplate, times(1)).send(eq("processed-transcription"), eq("ZOOM-meeting-456"), eq(transcription));
    }

    @Test
    @DisplayName("Should send processed action items successfully")
    void shouldSendProcessedActionItemsSuccessfully() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(eq("processed-action-items"), any(String.class), any(ProcessedActionItem.class)))
                .thenReturn(future);

        assertDoesNotThrow(() -> meetingMediaProducer.sendProcessedActionItems(actionItems));

        verify(kafkaTemplate, times(1)).send(eq("processed-action-items"), eq("ZOOM-meeting-456"), eq(actionItems));
    }

    @Test
    @DisplayName("Should handle send failure for meeting media event")
    void shouldHandleSendFailureForMeetingMediaEvent() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));

        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        assertDoesNotThrow(() -> meetingMediaProducer.sendMeetingMediaEvent(mediaEvent));
    }

    @Test
    @DisplayName("Should handle send failure for processed summary")
    void shouldHandleSendFailureForProcessedSummary() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka send failed"));

        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        assertDoesNotThrow(() -> meetingMediaProducer.sendProcessedSummary(summary));
    }

    @Test
    @DisplayName("Should generate correct key for TEAMS platform")
    void shouldGenerateCorrectKeyForTeamsPlatform() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(100L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        mediaEvent.setPlatform("TEAMS");
        meetingMediaProducer.sendMeetingMediaEvent(mediaEvent);

        verify(kafkaTemplate).send(any(String.class), eq("TEAMS-meeting-456"), any());
    }

    @Test
    @DisplayName("Should generate correct key for GOOGLE_MEET platform")
    void shouldGenerateCorrectKeyForGoogleMeetPlatform() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);

        org.apache.kafka.clients.producer.RecordMetadata metadata = mock(org.apache.kafka.clients.producer.RecordMetadata.class);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(100L);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(future);

        mediaEvent.setPlatform("GOOGLE_MEET");
        meetingMediaProducer.sendMeetingMediaEvent(mediaEvent);

        verify(kafkaTemplate).send(any(String.class), eq("GOOGLE_MEET-meeting-456"), any());
    }
}

