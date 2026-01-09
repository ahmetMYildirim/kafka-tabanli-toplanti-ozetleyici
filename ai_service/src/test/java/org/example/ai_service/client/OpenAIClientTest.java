package org.example.ai_service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAIClient Unit Tests")
public class OpenAIClientTest {

    @Mock
    private OkHttpClient mockHttpClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OpenAIClient openAIClient;

    private Path tempAudioFile;

    @BeforeEach
    void setUp() throws IOException {
        ReflectionTestUtils.setField(openAIClient, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(openAIClient, "baseUrl", "https:
        ReflectionTestUtils.setField(openAIClient, "whisperModel", "whisper-1");
        ReflectionTestUtils.setField(openAIClient, "chatModel", "gpt-4o-mini");
        ReflectionTestUtils.setField(openAIClient, "language", "en");
        ReflectionTestUtils.setField(openAIClient, "httpClient", mockHttpClient);

        tempAudioFile = Files.createTempFile("test-audio", ".wav");
        Files.write(tempAudioFile, "fake audio content".getBytes());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempAudioFile != null && Files.exists(tempAudioFile)) {
            Files.deleteIfExists(tempAudioFile);
        }
    }

    @Nested
    @DisplayName("transcribeAudio() tests")
    class TranscribeAudioTests {

        @Test
        @DisplayName("Valid audio file should return transcription")
        void transcribeAudio_WithValidFile_ShouldReturnText() throws IOException {
            String expectedTranscription = "This is a test transcription";
            String responseJson = String.format("{\"text\":\"%s\"}", expectedTranscription);

            ResponseBody responseBody = ResponseBody.create(
                    responseJson,
                    MediaType.parse("application/json")
            );

            Response response = new Response.Builder()
                    .request(new Request.Builder().url("https:
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody)
                    .build();

            Call mockCall = mock(Call.class);
            when(mockCall.execute()).thenReturn(response);
            when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);

            String result = openAIClient.transcribeAudio(tempAudioFile.toString());

            assertThat(result).isEqualTo(expectedTranscription);
            verify(mockHttpClient).newCall(argThat(request ->
                    request.url().toString().contains("/audio/transcriptions") &&
                            request.header("Authorization").equals("Bearer test-api-key")
            ));
        }

        @Test
        @DisplayName("Non-existent file should throw IllegalArgumentException")
        void transcribeAudio_WithNonExistentFile_ShouldThrowException() {
            String nonExistentPath = "/path/to/nonexistent/file.wav";

            assertThatThrownBy(() -> openAIClient.transcribeAudio(nonExistentPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Audio file not found");
        }

        @Test
        @DisplayName("API error should throw IOException")
        void transcribeAudio_WithAPIError_ShouldThrowIOException() throws IOException {
            String errorJson = "{\"error\":{\"message\":\"Invalid API key\"}}";
            ResponseBody errorBody = ResponseBody.create(errorJson, MediaType.parse("application/json"));

            Response errorResponse = new Response.Builder()
                    .request(new Request.Builder().url("https:
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body(errorBody)
                    .build();

            Call mockCall = mock(Call.class);
            when(mockCall.execute()).thenReturn(errorResponse);
            when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);

            assertThatThrownBy(() -> openAIClient.transcribeAudio(tempAudioFile.toString()))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("Whisper API failed");
        }
    }

    @Nested
    @DisplayName("generateSummary() tests")
    class GenerateSummaryTests {

        @Test
        @DisplayName("Valid transcription should return summary")
        void generateSummary_WithValidTranscription_ShouldReturnSummary() throws IOException {
            String transcription = "This is a long meeting transcription with multiple points discussed";
            String expectedSummary = "Summary of the meeting";
            String responseJson = String.format(
                    "{\"choices\":[{\"message\":{\"content\":\"%s\"}}]}",
                    expectedSummary
            );

            ResponseBody responseBody = ResponseBody.create(responseJson, MediaType.parse("application/json"));

            Response response = new Response.Builder()
                    .request(new Request.Builder().url("https:
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody)
                    .build();

            Call mockCall = mock(Call.class);
            when(mockCall.execute()).thenReturn(response);
            when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);

            String result = openAIClient.generateSummary(transcription);

            assertThat(result).isEqualTo(expectedSummary);
            verify(mockHttpClient).newCall(argThat(request ->
                    request.url().toString().contains("/chat/completions") &&
                            request.header("Authorization").equals("Bearer test-api-key")
            ));
        }

        @Test
        @DisplayName("Empty transcription should throw IllegalArgumentException")
        void generateSummary_WithEmptyTranscription_ShouldThrowException() {
            assertThatThrownBy(() -> openAIClient.generateSummary(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transcription cannot be empty");
        }

        @Test
        @DisplayName("Null transcription should throw IllegalArgumentException")
        void generateSummary_WithNullTranscription_ShouldThrowException() {
            assertThatThrownBy(() -> openAIClient.generateSummary(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transcription cannot be empty");
        }
    }

    @Nested
    @DisplayName("extractTasks() tests")
    class ExtractTasksTests {

        @Test
        @DisplayName("Valid transcription should return task list")
        void extractTasks_WithValidTranscription_ShouldReturnTasks() throws IOException {
            String transcription = "John needs to prepare the report by Friday. Sarah will review it";
            List<String> participants = List.of("John", "Sarah");
            String tasksJson = "[\"John: Prepare report by Friday\", \"Sarah: Review report\"]";
            String responseJson = String.format(
                    "{\"choices\":[{\"message\":{\"content\":\"%s\"}}]}",
                    tasksJson.replace("\"", "\\\"")
            );

            ResponseBody responseBody = ResponseBody.create(responseJson, MediaType.parse("application/json"));

            Response response = new Response.Builder()
                    .request(new Request.Builder().url("https:
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(responseBody)
                    .build();

            Call mockCall = mock(Call.class);
            when(mockCall.execute()).thenReturn(response);
            when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);

            String result = openAIClient.extractTasks(transcription, participants);

            assertThat(result).contains("John").contains("Sarah");
        }

        @Test
        @DisplayName("Empty transcription should throw IllegalArgumentException")
        void extractTasks_WithEmptyTranscription_ShouldThrowException() {
            assertThatThrownBy(() -> openAIClient.extractTasks("", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transcription cannot be empty");
        }
    }
}

