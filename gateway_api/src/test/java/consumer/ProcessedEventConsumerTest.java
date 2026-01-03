package consumer;

import cache.dataStore;
import model.event.ProcessedActionItem;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.NotificationService;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessedEventConsumer Unit Tests")
class ProcessedEventConsumerTest {

    @Mock
    private dataStore dataStore;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProcessedEventConsumer processedEventConsumer;

    @Nested
    @DisplayName("Summary Consumer Tests")
    class SummaryConsumerTests {

        @Test
        @DisplayName("Should consume and save summary")
        void consumeSummary_ShouldSaveAndNotify() {
            // Given
            ProcessedSummary summary = ProcessedSummary.builder()
                    .meetingId("meeting-123")
                    .platform("DISCORD")
                    .summary("Test özeti")
                    .processedTime(Instant.now())
                    .build();

            // When
            processedEventConsumer.consumeSummary(summary);

            // Then
            verify(dataStore).saveSummary(summary);
            verify(notificationService).notifyNewSummary(summary);
        }

        @Test
        @DisplayName("Should handle ZOOM platform summary")
        void consumeSummary_WithZoomPlatform_ShouldProcess() {
            // Given
            ProcessedSummary summary = ProcessedSummary.builder()
                    .meetingId("zoom-meeting-456")
                    .platform("ZOOM")
                    .summary("Zoom toplantı özeti")
                    .processedTime(Instant.now())
                    .build();

            // When
            processedEventConsumer.consumeSummary(summary);

            // Then
            verify(dataStore).saveSummary(summary);
            verify(notificationService).notifyNewSummary(summary);
        }
    }

    @Nested
    @DisplayName("Transcription Consumer Tests")
    class TranscriptionConsumerTests {

        @Test
        @DisplayName("Should consume and save transcription")
        void consumeTranscription_ShouldSaveAndNotify() {
            // Given
            ProcessedTranscription transcription = ProcessedTranscription.builder()
                    .meetingId("meeting-456")
                    .fullTranscription("Bu bir test transkriptidir.")
                    .processedTime(Instant.now())
                    .build();

            // When
            processedEventConsumer.consumeTranscription(transcription);

            // Then
            verify(dataStore).saveTranscription(transcription);
            verify(notificationService).notifyNewTranscript(transcription);
        }

        @Test
        @DisplayName("Should handle long transcription")
        void consumeTranscription_WithLongText_ShouldProcess() {
            // Given
            String longTranscription = "A".repeat(10000);
            ProcessedTranscription transcription = ProcessedTranscription.builder()
                    .meetingId("meeting-long")
                    .fullTranscription(longTranscription)
                    .processedTime(Instant.now())
                    .build();

            // When
            processedEventConsumer.consumeTranscription(transcription);

            // Then
            verify(dataStore).saveTranscription(transcription);
            verify(notificationService).notifyNewTranscript(transcription);
        }
    }

    @Nested
    @DisplayName("ActionItems Consumer Tests")
    class ActionItemsConsumerTests {

        @Test
        @DisplayName("Should consume and save action items")
        void consumeActionItems_ShouldSaveAndNotify() {
            // Given
            ProcessedActionItem actionItem = ProcessedActionItem.builder()
                    .meetingId("meeting-789")
                    .actionItems(List.of("Görev 1", "Görev 2", "Görev 3"))
                    .build();

            // When
            processedEventConsumer.consumeActionItems(actionItem);

            // Then
            verify(dataStore).saveActionItems(actionItem);
            verify(notificationService).notifyNewActionItems(actionItem);
        }

        @Test
        @DisplayName("Should handle empty action items list")
        void consumeActionItems_WithEmptyList_ShouldProcess() {
            // Given
            ProcessedActionItem actionItem = ProcessedActionItem.builder()
                    .meetingId("meeting-empty")
                    .actionItems(List.of())
                    .build();

            // When
            processedEventConsumer.consumeActionItems(actionItem);

            // Then
            verify(dataStore).saveActionItems(actionItem);
            verify(notificationService).notifyNewActionItems(actionItem);
        }

        @Test
        @DisplayName("Should handle many action items")
        void consumeActionItems_WithManyItems_ShouldProcess() {
            // Given
            List<String> manyItems = List.of(
                    "Görev 1", "Görev 2", "Görev 3", "Görev 4", "Görev 5",
                    "Görev 6", "Görev 7", "Görev 8", "Görev 9", "Görev 10"
            );
            ProcessedActionItem actionItem = ProcessedActionItem.builder()
                    .meetingId("meeting-many")
                    .actionItems(manyItems)
                    .build();

            // When
            processedEventConsumer.consumeActionItems(actionItem);

            // Then
            verify(dataStore).saveActionItems(actionItem);
            verify(notificationService).notifyNewActionItems(actionItem);
        }
    }
}

