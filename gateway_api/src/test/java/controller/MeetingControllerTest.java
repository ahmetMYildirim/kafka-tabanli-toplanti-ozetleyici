package controller;

import cache.dataStore;
import model.dto.ApiResponse;
import model.dto.MeetingDetailDTO;
import model.event.ProcessedActionItem;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingController Unit Tests")
class MeetingControllerTest {

    @Mock
    private dataStore dataStore;

    @InjectMocks
    private MeetingController meetingController;

    private ProcessedSummary testSummary;
    private ProcessedTranscription testTranscription;
    private ProcessedActionItem testActionItem;

    @BeforeEach
    void setUp() {
        testSummary = ProcessedSummary.builder()
                .meetingId("meeting-123")
                .platform("DISCORD")
                .summary("Test özeti")
                .processedTime(Instant.now())
                .build();

        testTranscription = ProcessedTranscription.builder()
                .meetingId("meeting-123")
                .fullTranscription("Test transkript")
                .processedTime(Instant.now())
                .build();

        testActionItem = ProcessedActionItem.builder()
                .meetingId("meeting-123")
                .actionItems(List.of("Görev 1", "Görev 2"))
                .build();
    }

    @Nested
    @DisplayName("Get All Meetings Tests")
    class GetAllMeetingsTests {

        @Test
        @DisplayName("Should return all meetings without filter")
        void getAllMeetings_WithoutFilter_ShouldReturnAll() {
            
            when(dataStore.getLastSummaries(20)).thenReturn(List.of(testSummary));

            
            ResponseEntity<ApiResponse<List<ProcessedSummary>>> response =
                    meetingController.getAllMeetings(null, 20);

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData()).hasSize(1);
            verify(dataStore).getLastSummaries(20);
        }

        @Test
        @DisplayName("Should filter meetings by platform")
        void getAllMeetings_WithPlatformFilter_ShouldFilterCorrectly() {
            
            when(dataStore.getSummariesByPlatform("DISCORD")).thenReturn(List.of(testSummary));

            
            ResponseEntity<ApiResponse<List<ProcessedSummary>>> response =
                    meetingController.getAllMeetings("DISCORD", 20);

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getPlatform()).isEqualTo("DISCORD");
            verify(dataStore).getSummariesByPlatform("DISCORD");
        }

        @Test
        @DisplayName("Should handle empty platform filter")
        void getAllMeetings_WithEmptyPlatform_ShouldReturnAll() {
            
            when(dataStore.getLastSummaries(10)).thenReturn(List.of(testSummary));

            
            ResponseEntity<ApiResponse<List<ProcessedSummary>>> response =
                    meetingController.getAllMeetings("", 10);

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(dataStore).getLastSummaries(10);
        }
    }

    @Nested
    @DisplayName("Get Meeting Detail Tests")
    class GetMeetingDetailTests {

        @Test
        @DisplayName("Should return meeting detail when found")
        void getMeetingDetail_WhenExists_ShouldReturnDetail() {
            
            when(dataStore.getSummary("meeting-123")).thenReturn(Optional.of(testSummary));
            when(dataStore.getTranscription("meeting-123")).thenReturn(Optional.of(testTranscription));
            when(dataStore.getActionItem("meeting-123")).thenReturn(Optional.of(testActionItem));

            
            ResponseEntity<ApiResponse<MeetingDetailDTO>> response =
                    meetingController.getMeetingDetail("meeting-123");

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getData().getProcessedSummary()).isNotNull();
            assertThat(response.getBody().getData().getProcessedTranscription()).isNotNull();
            assertThat(response.getBody().getData().getProcessedActionItem()).isNotNull();
        }

        @Test
        @DisplayName("Should return 404 when meeting not found")
        void getMeetingDetail_WhenNotExists_ShouldReturn404() {
            
            when(dataStore.getSummary("non-existent")).thenReturn(Optional.empty());

            
            ResponseEntity<ApiResponse<MeetingDetailDTO>> response =
                    meetingController.getMeetingDetail("non-existent");

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Get Meeting Summary Tests")
    class GetMeetingSummaryTests {

        @Test
        @DisplayName("Should return summary when found")
        void getMeetingSummary_WhenExists_ShouldReturnSummary() {
            
            when(dataStore.getSummary("meeting-123")).thenReturn(Optional.of(testSummary));

            
            ResponseEntity<ApiResponse<ProcessedSummary>> response =
                    meetingController.getMeetingSummary("meeting-123");

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getSummary()).isEqualTo("Test özeti");
        }

        @Test
        @DisplayName("Should return 404 when summary not found")
        void getMeetingSummary_WhenNotExists_ShouldReturn404() {
            
            when(dataStore.getSummary("non-existent")).thenReturn(Optional.empty());

            
            ResponseEntity<ApiResponse<ProcessedSummary>> response =
                    meetingController.getMeetingSummary("non-existent");

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Get Meeting Transcription Tests")
    class GetMeetingTranscriptionTests {

        @Test
        @DisplayName("Should return transcription when found")
        void getMeetingTranscription_WhenExists_ShouldReturnTranscription() {
            
            when(dataStore.getTranscription("meeting-123")).thenReturn(Optional.of(testTranscription));

            
            ResponseEntity<ApiResponse<ProcessedTranscription>> response =
                    meetingController.getMeetingTranscription("meeting-123");

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getData().getFullTranscription()).isEqualTo("Test transkript");
        }

        @Test
        @DisplayName("Should return 404 when transcription not found")
        void getMeetingTranscription_WhenNotExists_ShouldReturn404() {
            
            when(dataStore.getTranscription("non-existent")).thenReturn(Optional.empty());

            
            ResponseEntity<ApiResponse<ProcessedTranscription>> response =
                    meetingController.getMeetingTranscription("non-existent");

            
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}

