package org.example.ai_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_service.client.OpenAIClient;
import org.example.ai_service.domain.model.AudioEvent;
import org.example.ai_service.domain.model.TranscriptionResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TranscriptionService Unit Tests")
public class TranscriptionServiceTest {

    @Mock
    private OpenAIClient openAIClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TranscriptionService transcriptionService;

    private AudioEvent validAudioEvent;

    @BeforeEach
    void setUp() {
        validAudioEvent = AudioEvent.builder()
                .meetingId("meeting-123")
                .channelId("channel-456")
                .platform("ZOOM")
                .audioUrl("/path/to/audio.wav")
                .build();
    }

    @Nested
    @DisplayName("transcribe() tests")
    class TranscribeTests {

        @Test
        @DisplayName("Valid audio event should return transcription result")
        void transcribe_WithValidAudioEvent_ShouldReturnResult() throws IOException {
            String transcriptionText = "John: Hello everyone\nSarah: Hi John, how are you?\nJohn: I'm good, thanks!";

            when(openAIClient.transcribeAudio(anyString())).thenReturn(transcriptionText);

            TranscriptionResult result = transcriptionService.transcribe(validAudioEvent);

            assertThat(result).isNotNull();
            assertThat(result.getMeetingId()).isEqualTo("meeting-123");
            assertThat(result.getChannelId()).isEqualTo("channel-456");
            assertThat(result.getPlatform()).isEqualTo("ZOOM");
            assertThat(result.getFullTranscription()).isEqualTo(transcriptionText);
            assertThat(result.getSegments()).hasSize(3);
            assertThat(result.getLanguage()).isEqualTo("tr");
            assertThat(result.getConfidence()).isEqualTo(0.95);

            verify(openAIClient).transcribeAudio("/path/to/audio.wav");
        }

        @Test
        @DisplayName("Transcription with single speaker should parse correctly")
        void transcribe_WithSingleSpeaker_ShouldParseCorrectly() throws IOException {
            String transcriptionText = "Host: Welcome to the meeting. Today we will discuss the project status.";

            when(openAIClient.transcribeAudio(anyString())).thenReturn(transcriptionText);

            TranscriptionResult result = transcriptionService.transcribe(validAudioEvent);

            assertThat(result).isNotNull();
            assertThat(result.getSegments()).hasSize(1);
            assertThat(result.getSegments().get(0).getSpeakerName()).isEqualTo("Host");
            assertThat(result.getSegments().get(0).getText()).contains("Welcome");
        }

        @Test
        @DisplayName("Transcription with bracketed speaker names should parse correctly")
        void transcribe_WithBracketedSpeakers_ShouldParseCorrectly() throws IOException {
            String transcriptionText = "[Alice]: I agree with the proposal\n[Bob]: Me too\n[Charlie]: Let's vote";

            when(openAIClient.transcribeAudio(anyString())).thenReturn(transcriptionText);

            TranscriptionResult result = transcriptionService.transcribe(validAudioEvent);

            assertThat(result).isNotNull();
            assertThat(result.getSegments()).hasSize(3);
            assertThat(result.getSegments().get(0).getSpeakerName()).isEqualTo("Alice");
            assertThat(result.getSegments().get(1).getSpeakerName()).isEqualTo("Bob");
            assertThat(result.getSegments().get(2).getSpeakerName()).isEqualTo("Charlie");
        }

        @Test
        @DisplayName("OpenAI API failure should return null")
        void transcribe_WithOpenAIFailure_ShouldReturnNull() throws IOException {
            when(openAIClient.transcribeAudio(anyString())).thenThrow(new IOException("Whisper API failed"));

            TranscriptionResult result = transcriptionService.transcribe(validAudioEvent);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Empty transcription should return result with empty segments")
        void transcribe_WithEmptyTranscription_ShouldReturnEmptySegments() throws IOException {
            when(openAIClient.transcribeAudio(anyString())).thenReturn("");

            TranscriptionResult result = transcriptionService.transcribe(validAudioEvent);

            assertThat(result).isNotNull();
            assertThat(result.getSegments()).isEmpty();
        }

        @Test
        @DisplayName("Transcription without speaker pattern should return empty segments")
        void transcribe_WithoutSpeakerPattern_ShouldReturnEmptySegments() throws IOException {
            String transcriptionText = "This is just plain text without any speaker indicators";

            when(openAIClient.transcribeAudio(anyString())).thenReturn(transcriptionText);

            TranscriptionResult result = transcriptionService.transcribe(validAudioEvent);

            assertThat(result).isNotNull();
            assertThat(result.getSegments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Speaker ID generation tests")
    class SpeakerIdTests {

        @Test
        @DisplayName("Speaker names should generate correct IDs")
        void speakerNames_ShouldGenerateCorrectIds() throws IOException {
            String transcriptionText = "John Doe: Hello\nJane Smith: Hi\nBob: Hey";

            when(openAIClient.transcribeAudio(anyString())).thenReturn(transcriptionText);

            TranscriptionResult result = transcriptionService.transcribe(validAudioEvent);

            assertThat(result.getSegments()).hasSize(3);
            assertThat(result.getSegments().get(0).getSpeakerId()).isEqualTo("speaker_john_doe");
            assertThat(result.getSegments().get(1).getSpeakerId()).isEqualTo("speaker_jane_smith");
            assertThat(result.getSegments().get(2).getSpeakerId()).isEqualTo("speaker_bob");
        }
    }

    @Nested
    @DisplayName("Time estimation tests")
    class TimeEstimationTests {

        @Test
        @DisplayName("Segments should have estimated timestamps")
        void segments_ShouldHaveEstimatedTimestamps() throws IOException {
            String transcriptionText = "Speaker1: Short\nSpeaker2: This is a longer text";

            when(openAIClient.transcribeAudio(anyString())).thenReturn(transcriptionText);

            TranscriptionResult result = transcriptionService.transcribe(validAudioEvent);

            assertThat(result.getSegments()).hasSize(2);
            assertThat(result.getSegments().get(0).getStartTimeMs()).isZero();
            assertThat(result.getSegments().get(0).getEndTimeMs()).isGreaterThan(0);
            assertThat(result.getSegments().get(1).getStartTimeMs()).isGreaterThan(result.getSegments().get(0).getEndTimeMs());
        }
    }
}

