package org.example.ai_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_service.client.OpenAIClient;
import org.example.ai_service.entity.*;
import org.example.ai_service.repository.*;
import org.example.ai_service.util.AudioCompressor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioAnalysisController Unit Tests")
public class AudioAnalysisControllerTest {

    @Mock
    private OpenAIClient openAIClient;

    @Mock
    private AudioCompressor audioCompressor;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private AudioMessageRepository audioMessageRepository;

    @Mock
    private TranscriptionRepository transcriptionRepository;

    @Mock
    private MeetingSummaryRepository meetingSummaryRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AudioAnalysisController audioAnalysisController;

    private MockMultipartFile validAudioFile;

    @BeforeEach
    void setUp() {
        validAudioFile = new MockMultipartFile(
                "file",
                "test-meeting.wav",
                "audio/wav",
                "fake audio content".getBytes()
        );
    }

    @Nested
    @DisplayName("analyzeAudio() tests")
    class AnalyzeAudioTests {

        @Test
        @DisplayName("Valid audio file should return analysis result")
        void analyzeAudio_WithValidFile_ShouldReturnResult() throws IOException {
            String transcription = "This is a test meeting transcription";
            String summary = "Meeting summary";
            String tasks = "[\"Task 1\", \"Task 2\"]";

            when(audioCompressor.compressIfNeeded(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(openAIClient.transcribeAudio(anyString())).thenReturn(transcription);
            when(openAIClient.generateSummary(anyString())).thenReturn(summary);
            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(tasks);

            when(meetingRepository.save(any(MeetingEntity.class))).thenAnswer(inv -> {
                MeetingEntity meeting = inv.getArgument(0);
                meeting.setId(1L);
                return meeting;
            });
            when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> {
                AudioMessageEntity audio = inv.getArgument(0);
                audio.setId(1L);
                return audio;
            });
            when(transcriptionRepository.save(any(TranscriptionEntity.class))).thenAnswer(inv -> {
                TranscriptionEntity trans = inv.getArgument(0);
                trans.setId(1L);
                return trans;
            });
            when(meetingSummaryRepository.save(any(MeetingSummaryEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            ResponseEntity<Map<String, Object>> response = audioAnalysisController.analyzeAudio(
                    validAudioFile,
                    "meeting-123",
                    "Test Meeting"
            );

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsKey("success");
            assertThat(response.getBody().get("success")).isEqualTo(true);

            verify(openAIClient).transcribeAudio(anyString());
            verify(openAIClient).generateSummary(transcription);
            verify(openAIClient).extractTasks(eq(transcription), anyList());
        }

        @Test
        @DisplayName("Empty file should handle gracefully")
        void analyzeAudio_WithEmptyFile_ShouldHandleGracefully() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.wav",
                    "audio/wav",
                    new byte[0]
            );

            ResponseEntity<Map<String, Object>> response = audioAnalysisController.analyzeAudio(
                    emptyFile,
                    "meeting-456",
                    "Empty Test"
            );

            assertThat(response.getStatusCode().is5xxServerError() || response.getStatusCode().is4xxClientError()).isTrue();
        }

        @Test
        @DisplayName("Transcription failure should return error")
        void analyzeAudio_WithTranscriptionFailure_ShouldReturnError() throws IOException {
            when(audioCompressor.compressIfNeeded(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(openAIClient.transcribeAudio(anyString())).thenThrow(new IOException("Whisper API failed"));

            ResponseEntity<Map<String, Object>> response = audioAnalysisController.analyzeAudio(
                    validAudioFile,
                    null,
                    "Failed Meeting"
            );

            assertThat(response.getStatusCode().is5xxServerError() || response.getStatusCode().is4xxClientError()).isTrue();
        }
    }

    @Nested
    @DisplayName("analyzeAudioFromPath() tests")
    class AnalyzeAudioFromPathTests {

        @Test
        @DisplayName("Valid file path should return analysis result")
        void analyzeAudioFromPath_WithValidPath_ShouldReturnResult() throws IOException {
            Path tempFile = Files.createTempFile("test-audio", ".wav");
            Files.write(tempFile, "fake audio content".getBytes());
            
            String audioFilePath = tempFile.toString();
            String meetingTitle = "Path Test Meeting";
            String transcription = "Path transcription test";
            String summary = "Path summary";
            String tasks = "[\"Path Task 1\"]";

            try {
                when(audioCompressor.compressIfNeeded(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(openAIClient.transcribeAudio(anyString())).thenReturn(transcription);
                when(openAIClient.generateSummary(anyString())).thenReturn(summary);
                when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(tasks);

                when(meetingRepository.save(any(MeetingEntity.class))).thenAnswer(inv -> {
                    MeetingEntity meeting = inv.getArgument(0);
                    meeting.setId(2L);
                    return meeting;
                });
                when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> {
                    AudioMessageEntity audio = inv.getArgument(0);
                    audio.setId(2L);
                    return audio;
                });
                when(transcriptionRepository.save(any(TranscriptionEntity.class))).thenAnswer(inv -> {
                    TranscriptionEntity trans = inv.getArgument(0);
                    trans.setId(2L);
                    return trans;
                });
                when(meetingSummaryRepository.save(any(MeetingSummaryEntity.class))).thenAnswer(inv -> inv.getArgument(0));
                when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));

                ResponseEntity<Map<String, Object>> response = audioAnalysisController.analyzeAudioFromPath(
                        audioFilePath,
                        "meeting-123",
                        meetingTitle
                );

                assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody()).containsKey("success");
                assertThat(response.getBody().get("success")).isEqualTo(true);
                
                verify(openAIClient).transcribeAudio(anyString());
                verify(openAIClient).generateSummary(anyString());
                verify(openAIClient).extractTasks(anyString(), anyList());
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("Missing file path should return error")
        void analyzeAudioFromPath_WithMissingPath_ShouldReturnError() {
            ResponseEntity<Map<String, Object>> response = audioAnalysisController.analyzeAudioFromPath(
                    null,
                    null,
                    "Missing Path Meeting"
            );

            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        }

        @Test
        @DisplayName("Empty meeting title should use default")
        void analyzeAudioFromPath_WithEmptyTitle_ShouldUseDefault() throws IOException {
            Path tempFile = Files.createTempFile("test-audio-empty-title", ".wav");
            Files.write(tempFile, "fake audio content".getBytes());
            
            String audioFilePath = tempFile.toString();
            String transcription = "Default title transcription";
            String summary = "Default summary";
            String tasks = "[]";

            try {
                when(audioCompressor.compressIfNeeded(any(File.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(openAIClient.transcribeAudio(anyString())).thenReturn(transcription);
                when(openAIClient.generateSummary(anyString())).thenReturn(summary);
                when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(tasks);

                when(meetingRepository.save(any(MeetingEntity.class))).thenAnswer(inv -> {
                    MeetingEntity meeting = inv.getArgument(0);
                    meeting.setId(3L);
                    return meeting;
                });
                when(audioMessageRepository.save(any(AudioMessageEntity.class))).thenAnswer(inv -> {
                    AudioMessageEntity audio = inv.getArgument(0);
                    audio.setId(3L);
                    return audio;
                });
                when(transcriptionRepository.save(any(TranscriptionEntity.class))).thenAnswer(inv -> {
                    TranscriptionEntity trans = inv.getArgument(0);
                    trans.setId(3L);
                    return trans;
                });
                when(meetingSummaryRepository.save(any(MeetingSummaryEntity.class))).thenAnswer(inv -> inv.getArgument(0));
                when(taskRepository.save(any(TaskEntity.class))).thenAnswer(inv -> inv.getArgument(0));

                ResponseEntity<Map<String, Object>> response = audioAnalysisController.analyzeAudioFromPath(
                        audioFilePath,
                        null,
                        null
                );

                assertThat(response).isNotNull();
                assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody()).containsKey("success");
                assertThat(response.getBody().get("success")).isEqualTo(true);
                assertThat(response.getBody()).containsKey("meetingTitle");
                
                verify(openAIClient).transcribeAudio(anyString());
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    @Nested
    @DisplayName("Health check tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Health endpoint should return healthy status")
        void health_ShouldReturnHealthyStatus() {
            ResponseEntity<Map<String, String>> response = audioAnalysisController.health();

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getBody()).containsKey("status");
            assertThat(response.getBody()).containsKey("service");
            assertThat(response.getBody()).containsKey("timestamp");
        }
    }
}

