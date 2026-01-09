package org.example.ai_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai_service.client.OpenAIClient;
import org.example.ai_service.domain.model.ExtractedTask;
import org.example.ai_service.domain.model.TranscriptionResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskExtractionService Unit Tests")
public class TaskExtractionServiceTest {

    @Mock
    private OpenAIClient openAIClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TaskExtractionService taskExtractionService;

    private TranscriptionResult validTranscription;

    @BeforeEach
    void setUp() {
        validTranscription = TranscriptionResult.builder()
                .meetingId("meeting-999")
                .channelId("channel-888")
                .platform("DISCORD")
                .fullTranscription("John: We need to prepare the report by Friday. Sarah: I will review the code. Mike: Let's schedule a follow-up meeting.")
                .segments(List.of(
                        TranscriptionResult.TranscriptionSegment.builder()
                                .speakerName("John")
                                .text("We need to prepare the report by Friday")
                                .build(),
                        TranscriptionResult.TranscriptionSegment.builder()
                                .speakerName("Sarah")
                                .text("I will review the code")
                                .build(),
                        TranscriptionResult.TranscriptionSegment.builder()
                                .speakerName("Mike")
                                .text("Let's schedule a follow-up meeting")
                                .build()
                ))
                .build();
    }

    @Nested
    @DisplayName("extractedTask() tests")
    class ExtractedTaskTests {

        @Test
        @DisplayName("Valid transcription should return extracted tasks")
        void extractedTask_WithValidTranscription_ShouldReturnTasks() throws IOException {
            String gptResponse = """
                    {
                        "tasks": [
                            {
                                "title": "Prepare report",
                                "description": "Prepare the report by Friday",
                                "assignee": "John",
                                "priority": "HIGH",
                                "sourceText": "We need to prepare the report by Friday"
                            },
                            {
                                "title": "Review code",
                                "description": "Review the codebase",
                                "assignee": "Sarah",
                                "priority": "MEDIUM",
                                "sourceText": "I will review the code"
                            }
                        ]
                    }
                    """;

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getMeetingId()).isEqualTo("meeting-999");
            assertThat(result.getChannelId()).isEqualTo("channel-888");
            assertThat(result.getPlatform()).isEqualTo("DISCORD");
            assertThat(result.getTaskItems()).hasSize(2);

            ExtractedTask.TaskItem firstTask = result.getTaskItems().get(0);
            assertThat(firstTask.getTitle()).isEqualTo("Prepare report");
            assertThat(firstTask.getAssignee()).isEqualTo("John");
            assertThat(firstTask.getPriority()).isEqualTo(ExtractedTask.Priority.HIGH);
            assertThat(firstTask.getStatus()).isEqualTo(ExtractedTask.Status.PENDING);
            assertThat(firstTask.getId()).isNotEmpty();

            verify(openAIClient).extractTasks(
                    eq(validTranscription.getFullTranscription()),
                    argThat(participants -> participants.contains("John") && participants.contains("Sarah") && participants.contains("Mike"))
            );
        }

        @Test
        @DisplayName("GPT response with extra text should extract JSON correctly")
        void extractedTask_WithExtraText_ShouldExtractJson() throws IOException {
            String gptResponse = """
                    Here are the extracted tasks:
                    {
                        "tasks": [
                            {
                                "title": "Task 1",
                                "description": "Description 1",
                                "assignee": "Alice",
                                "priority": "LOW",
                                "sourceText": "Source text 1"
                            }
                        ]
                    }
                    Hope this helps!
                    """;

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getTaskItems()).hasSize(1);
            assertThat(result.getTaskItems().get(0).getTitle()).isEqualTo("Task 1");
        }

        @Test
        @DisplayName("OpenAI API failure should return null")
        void extractedTask_WithOpenAIFailure_ShouldReturnNull() throws IOException {
            when(openAIClient.extractTasks(anyString(), anyList())).thenThrow(new IOException("GPT API failed"));

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Empty task list should return empty tasks")
        void extractedTask_WithEmptyTasks_ShouldReturnEmptyList() throws IOException {
            String gptResponse = """
                    {
                        "tasks": []
                    }
                    """;

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getTaskItems()).isEmpty();
        }

        @Test
        @DisplayName("Invalid JSON should return null")
        void extractedTask_WithInvalidJson_ShouldReturnNull() throws IOException {
            String invalidJson = "This is not valid JSON";

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(invalidJson);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result).isNotNull();
            assertThat(result.getTaskItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Priority parsing tests")
    class PriorityParsingTests {

        @Test
        @DisplayName("Should parse all priority levels correctly")
        void shouldParseAllPriorityLevels() throws IOException {
            String gptResponse = """
                    {
                        "tasks": [
                            {
                                "title": "High priority task",
                                "description": "Urgent",
                                "assignee": "John",
                                "priority": "HIGH",
                                "sourceText": "Urgent task"
                            },
                            {
                                "title": "Medium priority task",
                                "description": "Normal",
                                "assignee": "Sarah",
                                "priority": "MEDIUM",
                                "sourceText": "Normal task"
                            },
                            {
                                "title": "Low priority task",
                                "description": "Can wait",
                                "assignee": "Mike",
                                "priority": "LOW",
                                "sourceText": "Low task"
                            }
                        ]
                    }
                    """;

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result.getTaskItems()).hasSize(3);
            assertThat(result.getTaskItems().get(0).getPriority()).isEqualTo(ExtractedTask.Priority.HIGH);
            assertThat(result.getTaskItems().get(1).getPriority()).isEqualTo(ExtractedTask.Priority.MEDIUM);
            assertThat(result.getTaskItems().get(2).getPriority()).isEqualTo(ExtractedTask.Priority.LOW);
        }

        @Test
        @DisplayName("Should handle invalid priority with default MEDIUM")
        void shouldHandleInvalidPriorityWithDefault() throws IOException {
            String gptResponse = """
                    {
                        "tasks": [
                            {
                                "title": "Task with invalid priority",
                                "description": "Test",
                                "assignee": "John",
                                "priority": "INVALID_PRIORITY",
                                "sourceText": "Test"
                            }
                        ]
                    }
                    """;

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result.getTaskItems()).hasSize(1);
            assertThat(result.getTaskItems().get(0).getPriority()).isEqualTo(ExtractedTask.Priority.MEDIUM);
        }

        @Test
        @DisplayName("Should handle missing priority with default MEDIUM")
        void shouldHandleMissingPriorityWithDefault() throws IOException {
            String gptResponse = """
                    {
                        "tasks": [
                            {
                                "title": "Task without priority",
                                "description": "Test",
                                "assignee": "John",
                                "sourceText": "Test"
                            }
                        ]
                    }
                    """;

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result.getTaskItems()).hasSize(1);
            assertThat(result.getTaskItems().get(0).getPriority()).isEqualTo(ExtractedTask.Priority.MEDIUM);
        }
    }

    @Nested
    @DisplayName("Task item structure tests")
    class TaskItemStructureTests {

        @Test
        @DisplayName("Each task should have unique ID")
        void eachTask_ShouldHaveUniqueId() throws IOException {
            String gptResponse = """
                    {
                        "tasks": [
                            {
                                "title": "Task 1",
                                "description": "Description 1",
                                "assignee": "John",
                                "priority": "HIGH",
                                "sourceText": "Source 1"
                            },
                            {
                                "title": "Task 2",
                                "description": "Description 2",
                                "assignee": "Sarah",
                                "priority": "LOW",
                                "sourceText": "Source 2"
                            }
                        ]
                    }
                    """;

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result.getTaskItems()).hasSize(2);
            String id1 = result.getTaskItems().get(0).getId();
            String id2 = result.getTaskItems().get(1).getId();
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1).isNotEmpty();
            assertThat(id2).isNotEmpty();
        }

        @Test
        @DisplayName("All tasks should have PENDING status by default")
        void allTasks_ShouldHavePendingStatus() throws IOException {
            String gptResponse = """
                    {
                        "tasks": [
                            {
                                "title": "Task 1",
                                "description": "Desc 1",
                                "assignee": "John",
                                "priority": "HIGH",
                                "sourceText": "Source"
                            },
                            {
                                "title": "Task 2",
                                "description": "Desc 2",
                                "assignee": "Sarah",
                                "priority": "LOW",
                                "sourceText": "Source"
                            }
                        ]
                    }
                    """;

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result.getTaskItems())
                    .allMatch(task -> task.getStatus() == ExtractedTask.Status.PENDING);
        }

        @Test
        @DisplayName("All tasks should have confidence score")
        void allTasks_ShouldHaveConfidenceScore() throws IOException {
            String gptResponse = """
                    {
                        "tasks": [
                            {
                                "title": "Task with confidence",
                                "description": "Test",
                                "assignee": "John",
                                "priority": "MEDIUM",
                                "sourceText": "Source"
                            }
                        ]
                    }
                    """;

            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            ExtractedTask result = taskExtractionService.extractedTask(validTranscription);

            assertThat(result.getTaskItems()).hasSize(1);
            assertThat(result.getTaskItems().get(0).getConfidenceScore()).isEqualTo(0.85);
        }
    }

    @Nested
    @DisplayName("Participant extraction tests")
    class ParticipantExtractionTests {

        @Test
        @DisplayName("Should extract unique participants from segments")
        void shouldExtractUniqueParticipants() throws IOException {
            TranscriptionResult transcriptionWithDuplicates = TranscriptionResult.builder()
                    .meetingId("meeting-dup")
                    .channelId("channel-dup")
                    .platform("ZOOM")
                    .fullTranscription("Test transcription")
                    .segments(List.of(
                            TranscriptionResult.TranscriptionSegment.builder().speakerName("Alice").text("Text 1").build(),
                            TranscriptionResult.TranscriptionSegment.builder().speakerName("Bob").text("Text 2").build(),
                            TranscriptionResult.TranscriptionSegment.builder().speakerName("Alice").text("Text 3").build()
                    ))
                    .build();

            String gptResponse = "{\"tasks\": []}";
            when(openAIClient.extractTasks(anyString(), anyList())).thenReturn(gptResponse);

            taskExtractionService.extractedTask(transcriptionWithDuplicates);

            verify(openAIClient).extractTasks(
                    anyString(),
                    argThat(participants -> participants.size() == 2 && participants.contains("Alice") && participants.contains("Bob"))
            );
        }
    }
}

