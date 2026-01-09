package org.example.ai_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_service.client.OpenAIClient;
import org.example.ai_service.domain.model.MeetingSummary;
import org.example.ai_service.domain.model.TranscriptionResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingSummaryService Unit Tests")
public class MeetingSummaryServiceTest {

    @Mock
    private OpenAIClient openAIClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MeetingSummrayService meetingSummaryService;

    private TranscriptionResult validTranscription;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(meetingSummaryService, "objectMapper", objectMapper);

        validTranscription = TranscriptionResult.builder()
                .meetingId("meeting-789")
                .channelId("channel-101")
                .platform("TEAMS")
                .fullTranscription("This is a meeting transcription with multiple participants discussing project status.")
                .segments(List.of(
                        TranscriptionResult.TranscriptionSegment.builder()
                                .speakerName("Alice")
                                .text("Hello everyone")
                                .build(),
                        TranscriptionResult.TranscriptionSegment.builder()
                                .speakerName("Bob")
                                .text("Hi Alice")
                                .build()
                ))
                .durationSeconds(1800L)
                .build();
    }

    @Nested
    @DisplayName("generateSummary() tests")
    class GenerateSummaryTests {

        @Test
        @DisplayName("Valid transcription should return summary")
        void generateSummary_WithValidTranscription_ShouldReturnSummary() throws IOException {
            String gptResponse = """
                    {
                        "title": "Project Status Meeting",
                        "summary": "Team discussed the current project status and upcoming milestones.",
                        "keyPoints": ["Project is on track", "New features to be added"],
                        "decisions": ["Deploy by end of month", "Schedule follow-up meeting"],
                        "participants": ["Alice", "Bob"]
                    }
                    """;

            when(openAIClient.generateSummary(anyString())).thenReturn(gptResponse);

            MeetingSummary result = meetingSummaryService.generateSummary(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getMeetingId()).isEqualTo("meeting-789");
            assertThat(result.getChannelId()).isEqualTo("channel-101");
            assertThat(result.getPlatform()).isEqualTo("TEAMS");
            assertThat(result.getTitle()).isEqualTo("Project Status Meeting");
            assertThat(result.getSummary()).contains("discussed");
            assertThat(result.getKeyPoints()).hasSize(2);
            assertThat(result.getDecisions()).hasSize(2);
            assertThat(result.getParticipants()).contains("Alice", "Bob");
            assertThat(result.getDurationMinutes()).isEqualTo(30L);

            verify(openAIClient).generateSummary(validTranscription.getFullTranscription());
        }

        @Test
        @DisplayName("GPT response with extra text should extract JSON correctly")
        void generateSummary_WithExtraText_ShouldExtractJson() throws IOException {
            String gptResponse = """
                    Here is the summary:
                    {
                        "title": "Quick Meeting",
                        "summary": "Brief discussion",
                        "keyPoints": ["Point 1"],
                        "decisions": [],
                        "participants": ["Alice"]
                    }
                    Hope this helps!
                    """;

            when(openAIClient.generateSummary(anyString())).thenReturn(gptResponse);

            MeetingSummary result = meetingSummaryService.generateSummary(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Quick Meeting");
            assertThat(result.getSummary()).isEqualTo("Brief discussion");
        }

        @Test
        @DisplayName("OpenAI API failure should return fallback summary")
        void generateSummary_WithOpenAIFailure_ShouldReturnFallback() throws IOException {
            when(openAIClient.generateSummary(anyString())).thenThrow(new IOException("GPT API failed"));

            MeetingSummary result = meetingSummaryService.generateSummary(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getMeetingId()).isEqualTo("meeting-789");
            assertThat(result.getTitle()).contains("Meeting - meeting-789");
            assertThat(result.getSummary()).contains("could not be generated");
            assertThat(result.getKeyPoints()).isEmpty();
            assertThat(result.getDecisions()).isEmpty();
            assertThat(result.getParticipants()).containsExactlyInAnyOrder("Alice", "Bob");
        }

        @Test
        @DisplayName("Invalid JSON should return fallback summary")
        void generateSummary_WithInvalidJson_ShouldReturnFallback() throws IOException {
            String invalidJson = "This is not valid JSON at all";

            when(openAIClient.generateSummary(anyString())).thenReturn(invalidJson);

            MeetingSummary result = meetingSummaryService.generateSummary(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).contains("Meeting");
            assertThat(result.getParticipants()).containsExactlyInAnyOrder("Alice", "Bob");
        }

        @Test
        @DisplayName("Empty transcription should return fallback summary")
        void generateSummary_WithEmptyTranscription_ShouldReturnFallback() throws IOException {
            TranscriptionResult emptyTranscription = TranscriptionResult.builder()
                    .meetingId("empty-meeting")
                    .channelId("channel-empty")
                    .platform("ZOOM")
                    .fullTranscription("")
                    .segments(List.of())
                    .build();

            when(openAIClient.generateSummary(anyString())).thenThrow(new IOException("Empty transcription"));

            MeetingSummary result = meetingSummaryService.generateSummary(emptyTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getMeetingId()).isEqualTo("empty-meeting");
            assertThat(result.getParticipants()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Fallback summary tests")
    class FallbackSummaryTests {

        @Test
        @DisplayName("Fallback summary should contain participants from transcription")
        void fallbackSummary_ShouldContainParticipants() throws IOException {
            when(openAIClient.generateSummary(anyString())).thenThrow(new IOException("Test failure"));

            MeetingSummary result = meetingSummaryService.generateSummary(validTranscription);

            assertThat(result.getParticipants()).contains("Alice", "Bob");
            assertThat(result.getParticipants()).hasSize(2);
        }

        @Test
        @DisplayName("Fallback summary should have valid structure")
        void fallbackSummary_ShouldHaveValidStructure() throws IOException {
            when(openAIClient.generateSummary(anyString())).thenThrow(new IOException("Test failure"));

            MeetingSummary result = meetingSummaryService.generateSummary(validTranscription);

            assertThat(result.getTitle()).isNotEmpty();
            assertThat(result.getSummary()).isNotEmpty();
            assertThat(result.getKeyPoints()).isNotNull();
            assertThat(result.getDecisions()).isNotNull();
            assertThat(result.getProcessedTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("JSON parsing tests")
    class JsonParsingTests {

        @Test
        @DisplayName("Should handle missing optional fields")
        void shouldHandleMissingOptionalFields() throws IOException {
            String minimalJson = """
                    {
                        "title": "Minimal Meeting",
                        "summary": "Basic summary"
                    }
                    """;

            when(openAIClient.generateSummary(anyString())).thenReturn(minimalJson);

            MeetingSummary result = meetingSummaryService.generateSummary(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Minimal Meeting");
            assertThat(result.getKeyPoints()).isEmpty();
            assertThat(result.getDecisions()).isEmpty();
            assertThat(result.getParticipants()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null array values")
        void shouldHandleNullArrayValues() throws IOException {
            String jsonWithNulls = """
                    {
                        "title": "Test Meeting",
                        "summary": "Test summary",
                        "keyPoints": null,
                        "decisions": null,
                        "participants": null
                    }
                    """;

            when(openAIClient.generateSummary(anyString())).thenReturn(jsonWithNulls);

            MeetingSummary result = meetingSummaryService.generateSummary(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getKeyPoints()).isEmpty();
            assertThat(result.getDecisions()).isEmpty();
            assertThat(result.getParticipants()).isEmpty();
        }
    }
}

