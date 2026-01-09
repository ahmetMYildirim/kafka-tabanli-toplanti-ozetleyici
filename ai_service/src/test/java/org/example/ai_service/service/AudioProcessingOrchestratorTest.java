package org.example.ai_service.service;

import org.example.ai_service.domain.model.AudioEvent;
import org.example.ai_service.domain.model.ExtractedTask;
import org.example.ai_service.domain.model.MeetingSummary;
import org.example.ai_service.domain.model.TranscriptionResult;
import org.example.ai_service.entity.AudioMessageEntity;
import org.example.ai_service.producer.ActionItemProducer;
import org.example.ai_service.producer.SummaryProducer;
import org.example.ai_service.producer.TranscriptionProducer;
import org.example.ai_service.repository.AudioMessageRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioProcessingOrchestrator Unit Tests")
public class AudioProcessingOrchestratorTest {

    @Mock
    private TranscriptionService transcriptionService;

    @Mock
    private TaskExtractionService taskExtractionService;

    @Mock
    private MeetingSummrayService summaryService;

    @Mock
    private TranscriptionProducer transcriptionProducer;

    @Mock
    private ActionItemProducer actionItemProducer;

    @Mock
    private SummaryProducer summaryProducer;

    @Mock
    private AudioMessageRepository audioMessageRepository;

    @InjectMocks
    private AudioProcessingOrchestrator orchestrator;

    private AudioEvent validAudioEvent;
    private TranscriptionResult mockTranscription;
    private ExtractedTask mockTasks;
    private MeetingSummary mockSummary;

    @BeforeEach
    void setUp() {
        validAudioEvent = AudioEvent.builder()
                .meetingId("meeting-orch-123")
                .channelId("channel-orch-456")
                .platform("ZOOM")
                .audioUrl("/path/to/orch-audio.wav")
                .author("orchestrator-user")
                .timestamp(LocalDateTime.now())
                .voiceSessionId("session-orch-789")
                .build();

        mockTranscription = TranscriptionResult.builder()
                .meetingId("meeting-orch-123")
                .channelId("channel-orch-456")
                .platform("ZOOM")
                .fullTranscription("Mock transcription text")
                .segments(List.of())
                .language("en")
                .confidence(0.95)
                .processedTime("2024-01-01T00:00:00Z")
                .build();

        mockTasks = ExtractedTask.builder()
                .meetingId("meeting-orch-123")
                .channelId("channel-orch-456")
                .platform("ZOOM")
                .taskItems(List.of(
                        ExtractedTask.TaskItem.builder()
                                .id("task-1")
                                .title("Mock Task")
                                .description("Mock Description")
                                .assignee("John")
                                .priority(ExtractedTask.Priority.HIGH)
                                .status(ExtractedTask.Status.PENDING)
                                .build()
                ))
                .processedTime(Instant.now())
                .build();

        mockSummary = MeetingSummary.builder()
                .meetingId("meeting-orch-123")
                .channelId("channel-orch-456")
                .platform("ZOOM")
                .title("Mock Meeting Summary")
                .summary("This is a mock summary")
                .keyPoints(List.of("Point 1", "Point 2"))
                .decisions(List.of("Decision 1"))
                .participants(List.of("John", "Sarah"))
                .processedTime(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("processAudioEvent() tests")
    class ProcessAudioEventTests {

        @Test
        @DisplayName("Valid audio event should trigger full processing pipeline")
        void processAudioEvent_WithValidEvent_ShouldTriggerFullPipeline() {
            when(transcriptionService.transcribe(any(AudioEvent.class))).thenReturn(mockTranscription);
            when(taskExtractionService.extractedTask(any(TranscriptionResult.class))).thenReturn(mockTasks);
            when(summaryService.generateSummary(any(TranscriptionResult.class))).thenReturn(mockSummary);
            when(audioMessageRepository.findByAudioUrl(anyString())).thenReturn(Optional.empty());
            when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            orchestrator.processAudioEvent(validAudioEvent);

            verify(transcriptionService).transcribe(validAudioEvent);
            verify(taskExtractionService).extractedTask(mockTranscription);
            verify(summaryService).generateSummary(mockTranscription);
            verify(transcriptionProducer).send(mockTranscription);
            verify(actionItemProducer).send(mockTasks);
            verify(summaryProducer).send(mockSummary);
            verify(audioMessageRepository).save(any(AudioMessageEntity.class));
        }

        @Test
        @DisplayName("Existing audio message should be updated")
        void processAudioEvent_WithExistingMessage_ShouldUpdateEntity() {
            AudioMessageEntity existingEntity = AudioMessageEntity.builder()
                    .id(1L)
                    .audioUrl("/path/to/orch-audio.wav")
                    .platform("ZOOM")
                    .channelId("channel-orch-456")
                    .build();

            when(transcriptionService.transcribe(any(AudioEvent.class))).thenReturn(mockTranscription);
            when(taskExtractionService.extractedTask(any(TranscriptionResult.class))).thenReturn(mockTasks);
            when(summaryService.generateSummary(any(TranscriptionResult.class))).thenReturn(mockSummary);
            when(audioMessageRepository.findByAudioUrl(anyString())).thenReturn(Optional.of(existingEntity));
            when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            orchestrator.processAudioEvent(validAudioEvent);

            verify(audioMessageRepository).findByAudioUrl("/path/to/orch-audio.wav");
            verify(audioMessageRepository).save(argThat(entity ->
                    entity.getId().equals(1L) &&
                            entity.getTranscription().equals("Mock transcription text")
            ));
        }

        @Test
        @DisplayName("Transcription failure should not crash the pipeline")
        void processAudioEvent_WithTranscriptionFailure_ShouldHandleGracefully() {
            when(transcriptionService.transcribe(any(AudioEvent.class))).thenThrow(new RuntimeException("Transcription failed"));

            assertThatCode(() -> orchestrator.processAudioEvent(validAudioEvent))
                    .doesNotThrowAnyException();

            verify(transcriptionService).transcribe(validAudioEvent);
            verifyNoInteractions(taskExtractionService);
            verifyNoInteractions(summaryService);
            verifyNoInteractions(transcriptionProducer);
        }

        @Test
        @DisplayName("Task extraction failure should not stop summary generation")
        void processAudioEvent_WithTaskExtractionFailure_ShouldContinue() {
            when(transcriptionService.transcribe(any(AudioEvent.class))).thenReturn(mockTranscription);
            when(taskExtractionService.extractedTask(any(TranscriptionResult.class))).thenThrow(new RuntimeException("Task extraction failed"));
            when(summaryService.generateSummary(any(TranscriptionResult.class))).thenReturn(mockSummary);
            when(audioMessageRepository.findByAudioUrl(anyString())).thenReturn(Optional.empty());
            when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> orchestrator.processAudioEvent(validAudioEvent))
                    .doesNotThrowAnyException();

            verify(transcriptionService).transcribe(validAudioEvent);
            verify(taskExtractionService).extractedTask(mockTranscription);
        }

        @Test
        @DisplayName("Kafka producer failure should not crash the pipeline")
        void processAudioEvent_WithProducerFailure_ShouldHandleGracefully() {
            when(transcriptionService.transcribe(any(AudioEvent.class))).thenReturn(mockTranscription);
            when(taskExtractionService.extractedTask(any(TranscriptionResult.class))).thenReturn(mockTasks);
            when(summaryService.generateSummary(any(TranscriptionResult.class))).thenReturn(mockSummary);
            when(audioMessageRepository.findByAudioUrl(anyString())).thenReturn(Optional.empty());
            when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("Kafka send failed")).when(transcriptionProducer).send(any());

            assertThatCode(() -> orchestrator.processAudioEvent(validAudioEvent))
                    .doesNotThrowAnyException();

            verify(transcriptionProducer).send(mockTranscription);
        }
    }

    @Nested
    @DisplayName("Database integration tests")
    class DatabaseIntegrationTests {

        @Test
        @DisplayName("Should save new audio message entity")
        void shouldSaveNewAudioMessageEntity() {
            when(transcriptionService.transcribe(any(AudioEvent.class))).thenReturn(mockTranscription);
            when(taskExtractionService.extractedTask(any(TranscriptionResult.class))).thenReturn(mockTasks);
            when(summaryService.generateSummary(any(TranscriptionResult.class))).thenReturn(mockSummary);
            when(audioMessageRepository.findByAudioUrl(anyString())).thenReturn(Optional.empty());
            when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            orchestrator.processAudioEvent(validAudioEvent);

            verify(audioMessageRepository).save(argThat(entity ->
                    entity.getPlatform().equals("ZOOM") &&
                            entity.getChannelId().equals("channel-orch-456") &&
                            entity.getAuthor().equals("orchestrator-user") &&
                            entity.getAudioUrl().equals("/path/to/orch-audio.wav") &&
                            entity.getTranscription().equals("Mock transcription text") &&
                            entity.getVoiceSessionId().equals("session-orch-789") &&
                            entity.getMeetingId() == null
            ));
        }

        @Test
        @DisplayName("Should update existing audio message transcription")
        void shouldUpdateExistingAudioMessageTranscription() {
            AudioMessageEntity existingEntity = AudioMessageEntity.builder()
                    .id(5L)
                    .audioUrl("/path/to/orch-audio.wav")
                    .transcription(null)
                    .build();

            when(transcriptionService.transcribe(any(AudioEvent.class))).thenReturn(mockTranscription);
            when(taskExtractionService.extractedTask(any(TranscriptionResult.class))).thenReturn(mockTasks);
            when(summaryService.generateSummary(any(TranscriptionResult.class))).thenReturn(mockSummary);
            when(audioMessageRepository.findByAudioUrl(anyString())).thenReturn(Optional.of(existingEntity));
            when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            orchestrator.processAudioEvent(validAudioEvent);

            assertThat(existingEntity.getTranscription()).isEqualTo("Mock transcription text");
            verify(audioMessageRepository).save(existingEntity);
        }
    }

    @Nested
    @DisplayName("Kafka producer integration tests")
    class KafkaProducerIntegrationTests {

        @Test
        @DisplayName("Should send all three types of events to Kafka")
        void shouldSendAllThreeEventTypes() {
            when(transcriptionService.transcribe(any(AudioEvent.class))).thenReturn(mockTranscription);
            when(taskExtractionService.extractedTask(any(TranscriptionResult.class))).thenReturn(mockTasks);
            when(summaryService.generateSummary(any(TranscriptionResult.class))).thenReturn(mockSummary);
            when(audioMessageRepository.findByAudioUrl(anyString())).thenReturn(Optional.empty());
            when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            orchestrator.processAudioEvent(validAudioEvent);

            verify(transcriptionProducer).send(mockTranscription);
            verify(actionItemProducer).send(mockTasks);
            verify(summaryProducer).send(mockSummary);
        }

        @Test
        @DisplayName("Should send correct data to each producer")
        void shouldSendCorrectDataToEachProducer() {
            when(transcriptionService.transcribe(any(AudioEvent.class))).thenReturn(mockTranscription);
            when(taskExtractionService.extractedTask(any(TranscriptionResult.class))).thenReturn(mockTasks);
            when(summaryService.generateSummary(any(TranscriptionResult.class))).thenReturn(mockSummary);
            when(audioMessageRepository.findByAudioUrl(anyString())).thenReturn(Optional.empty());
            when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            orchestrator.processAudioEvent(validAudioEvent);

            verify(transcriptionProducer).send(argThat(t ->
                    t.getMeetingId().equals("meeting-orch-123") &&
                            t.getFullTranscription().equals("Mock transcription text")
            ));

            verify(actionItemProducer).send(argThat(t ->
                    t.getMeetingId().equals("meeting-orch-123") &&
                            t.getTaskItems().size() == 1
            ));

            verify(summaryProducer).send(argThat(s ->
                    s.getMeetingId().equals("meeting-orch-123") &&
                            s.getTitle().equals("Mock Meeting Summary")
            ));
        }
    }
}

