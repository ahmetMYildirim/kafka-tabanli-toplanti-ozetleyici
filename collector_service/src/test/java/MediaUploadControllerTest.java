import org.example.collector_service.MainApp;
import org.example.collector_service.controller.MediaUploadController;
import org.example.collector_service.domain.dto.MediaUploadRequest;
import org.example.collector_service.domain.dto.MediaUploadResponse;
import org.example.collector_service.exception.*;
import org.example.collector_service.service.MediaIngestService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaUploadController Unit Tests")
public class MediaUploadControllerTest {

    @Mock
    private MediaIngestService mediaIngestService;

    @InjectMocks
    private MediaUploadController mediaUploadController;

    private MockMultipartFile validAudioFile;
    private MockMultipartFile validVideoFile;

    @BeforeEach
    void setUp() {
        validAudioFile = new MockMultipartFile(
                "file",
                "meeting-audio.mp3",
                "audio/mpeg",
                "audio content".getBytes()
        );

        validVideoFile = new MockMultipartFile(
                "file",
                "meeting-video.mp4",
                "video/mp4",
                "video content".getBytes()
        );
    }

    @Nested
    @DisplayName("uploadMedia() tests")
    class UploadMediaTests {

        @Test
        @DisplayName("Valid upload should return success response")
        void uploadMedia_ValidRequest_ShouldReturnSuccess() {
            MediaUploadRequest request = MediaUploadRequest.builder()
                    .meetingId("meeting-123")
                    .platform("ZOOM")
                    .meetingTitle("Sprint Planning")
                    .hostName("John Doe")
                    .build();

            MediaUploadResponse expectedResponse = MediaUploadResponse.builder()
                    .mediaStatusId(1L)
                    .fileKey("zoom_abc123")
                    .status("SUCCESS")
                    .message("File uploaded successfully.")
                    .uploadedAt(LocalDateTime.now())
                    .build();

            when(mediaIngestService.uploadMedia(any(MultipartFile.class), any(MediaUploadRequest.class)))
                    .thenReturn(expectedResponse);

            ResponseEntity<MediaUploadResponse> response = mediaUploadController.uploadMedia(validAudioFile, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
            assertThat(response.getBody().getFileKey()).startsWith("zoom_");
        }

        @Test
        @DisplayName("Upload with video file should succeed")
        void uploadMedia_VideoFile_ShouldReturnSuccess() {
            MediaUploadRequest request = MediaUploadRequest.builder()
                    .meetingId("meeting-456")
                    .platform("TEAMS")
                    .build();

            MediaUploadResponse expectedResponse = MediaUploadResponse.builder()
                    .mediaStatusId(2L)
                    .fileKey("teams_def456")
                    .status("SUCCESS")
                    .build();

            when(mediaIngestService.uploadMedia(any(MultipartFile.class), any(MediaUploadRequest.class)))
                    .thenReturn(expectedResponse);

            ResponseEntity<MediaUploadResponse> response = mediaUploadController.uploadMedia(validVideoFile, request);

            assertThat(response.getBody().getFileKey()).startsWith("teams_");
        }
    }

    @Nested
    @DisplayName("uploadZoomMedia() tests")
    class UploadZoomMediaTests {

        @Test
        @DisplayName("Zoom upload should set platform to ZOOM")
        void uploadZoomMedia_ShouldSetZoomPlatform() {
            MediaUploadResponse expectedResponse = MediaUploadResponse.builder()
                    .mediaStatusId(1L)
                    .fileKey("zoom_xyz789")
                    .status("SUCCESS")
                    .build();

            when(mediaIngestService.uploadMedia(any(MultipartFile.class), any(MediaUploadRequest.class)))
                    .thenReturn(expectedResponse);

            ResponseEntity<MediaUploadResponse> response = mediaUploadController.uploadZoomMedia(
                    validVideoFile,
                    "zoom-meeting-123",
                    "Daily Standup",
                    "Jane Smith"
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getFileKey()).startsWith("zoom_");

            verify(mediaIngestService).uploadMedia(eq(validVideoFile), argThat(request ->
                    request.getPlatform().equals("ZOOM") &&
                    request.getMeetingId().equals("zoom-meeting-123") &&
                    request.getMeetingTitle().equals("Daily Standup") &&
                    request.getHostName().equals("Jane Smith")
            ));
        }

        @Test
        @DisplayName("Zoom upload with optional fields null should work")
        void uploadZoomMedia_OptionalFieldsNull_ShouldWork() {
            MediaUploadResponse expectedResponse = MediaUploadResponse.builder()
                    .mediaStatusId(3L)
                    .fileKey("zoom_optional")
                    .status("SUCCESS")
                    .build();

            when(mediaIngestService.uploadMedia(any(MultipartFile.class), any(MediaUploadRequest.class)))
                    .thenReturn(expectedResponse);

            ResponseEntity<MediaUploadResponse> response = mediaUploadController.uploadZoomMedia(
                    validAudioFile,
                    "zoom-meeting-456",
                    null,
                    null
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            verify(mediaIngestService).uploadMedia(eq(validAudioFile), argThat(request ->
                    request.getPlatform().equals("ZOOM") &&
                    request.getMeetingTitle() == null &&
                    request.getHostName() == null
            ));
        }
    }

    @Nested
    @DisplayName("uploadTeamsMedia() tests")
    class UploadTeamsMediaTests {

        @Test
        @DisplayName("Teams upload should set platform to TEAMS")
        void uploadTeamsMedia_ShouldSetTeamsPlatform() {
            MediaUploadResponse expectedResponse = MediaUploadResponse.builder()
                    .mediaStatusId(4L)
                    .fileKey("teams_abc")
                    .status("SUCCESS")
                    .build();

            when(mediaIngestService.uploadMedia(any(MultipartFile.class), any(MediaUploadRequest.class)))
                    .thenReturn(expectedResponse);

            ResponseEntity<MediaUploadResponse> response = mediaUploadController.uploadTeamsMedia(
                    validVideoFile,
                    "teams-meeting-789",
                    "Project Review"
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getFileKey()).startsWith("teams_");

            verify(mediaIngestService).uploadMedia(eq(validVideoFile), argThat(request ->
                    request.getPlatform().equals("TEAMS") &&
                    request.getMeetingId().equals("teams-meeting-789") &&
                    request.getMeetingTitle().equals("Project Review")
            ));
        }

        @Test
        @DisplayName("Teams upload without title should work")
        void uploadTeamsMedia_NoTitle_ShouldWork() {
            MediaUploadResponse expectedResponse = MediaUploadResponse.builder()
                    .mediaStatusId(5L)
                    .fileKey("teams_notitle")
                    .status("SUCCESS")
                    .build();

            when(mediaIngestService.uploadMedia(any(MultipartFile.class), any(MediaUploadRequest.class)))
                    .thenReturn(expectedResponse);

            ResponseEntity<MediaUploadResponse> response = mediaUploadController.uploadTeamsMedia(
                    validAudioFile,
                    "teams-meeting-999",
                    null
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            verify(mediaIngestService).uploadMedia(eq(validAudioFile), argThat(request ->
                    request.getMeetingTitle() == null
            ));
        }
    }
}

