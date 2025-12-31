import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.collector_service.domain.dto.MediaUploadRequest;
import org.example.collector_service.domain.dto.MediaUploadResponse;
import org.example.collector_service.domain.model.MediaAsset;
import org.example.collector_service.domain.model.MeetingMedia;
import org.example.collector_service.domain.model.OutBoxEvent;
import org.example.collector_service.exception.*;
import org.example.collector_service.repository.MediaAssetRepository;
import org.example.collector_service.repository.MeetingMediaRepository;
import org.example.collector_service.repository.OutBoxEventRepository;
import org.example.collector_service.service.MediaIngestService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaIngestService Unit Tests")
public class MediaIngestServiceTest {

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private MeetingMediaRepository meetingMediaRepository;

    @Mock
    private OutBoxEventRepository outBoxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private MediaIngestService mediaIngestService;

    private Path tempDir;
    private MediaUploadRequest validRequest;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test-media");
        ReflectionTestUtils.setField(mediaIngestService, "mediaStoragePath", tempDir.toString());

        validRequest = MediaUploadRequest.builder()
                .meetingId("meeting-123")
                .platform("ZOOM")
                .meetingTitle("Test Meeting")
                .hostName("Test Host")
                .meetingStartTime(LocalDateTime.now().minusHours(1))
                .meetingEndTime(LocalDateTime.now())
                .participantCount(5)
                .uploadedBy("test-user")
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted((a,b) -> b.compareTo(a))
                .forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException e) {}
                });
    }

    // ===================================== Upload Media Tests ===============================================

    @Nested
    @DisplayName("uploadMedia() tests")
    class UploadMediaTests {

        @Test
        @DisplayName("Valid audio file upload should succeed")
        void uploadMedia_WithValidAudioFile_ShouldReturnSuccess(){
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-audio.mp3",
                    "audio/mpeg",
                    "fake audio content".getBytes()
            );

            when(mediaAssetRepository.existsByChecksum(anyString())).thenReturn(false);
            when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(e -> {
                MediaAsset asset = e.getArgument(0);
                asset.setId(1L);
                return asset;
            });
            when(meetingMediaRepository.save(any(MeetingMedia.class))).thenAnswer(e -> {
                MeetingMedia meetingMedia = e.getArgument(0);
                meetingMedia.setId(1L);
                return meetingMedia;
            });
            when(outBoxEventRepository.save(any(OutBoxEvent.class))).thenAnswer(e -> e.getArgument(0));

            MediaUploadResponse response = mediaIngestService.uploadMedia(file, validRequest);

            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("SUCCESS");
            assertThat(response.getFileKey()).startsWith("zoom_");
            assertThat(response.getMediaStatusId()).isEqualTo(1L);

            verify(mediaAssetRepository).save(any(MediaAsset.class));
            verify(meetingMediaRepository).save(any(MeetingMedia.class));
            verify(outBoxEventRepository).save(any(OutBoxEvent.class));
        }

        @Test
        @DisplayName("Valid video file upload should succeed")
        void uploadMedia_withValidVideoFile_ShouldReturnSuccess(){
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test-video.mp4",
                    "video/mp4",
                    "fake video content".getBytes()
            );

            when(mediaAssetRepository.existsByChecksum(anyString())).thenReturn(false);
            when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(e -> {
                MediaAsset asset = e.getArgument(0);
                asset.setId(1L);
                return asset;
            });
            when(meetingMediaRepository.save(any(MeetingMedia.class))).thenAnswer( e -> {
                MeetingMedia meetingMedia = e.getArgument(0);
                meetingMedia.setId(1L);
                return meetingMedia;
            });
            when(outBoxEventRepository.save(any(OutBoxEvent.class))).thenAnswer(e -> e.getArgument(0));

            MediaUploadResponse response = mediaIngestService.uploadMedia(file, validRequest);

            assertThat(response.getStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("Empty file should throw InvalidFileException")
        void uploadMedia_withEmptyFile_ShouldThrowInvalidFileException(){
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.mp3",
                    "audio/mpeg",
                    new byte[0]
            );

            assertThatThrownBy(() -> mediaIngestService.uploadMedia(emptyFile, validRequest))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("Unsupported file type should throw InvalidFileException")
        void uploadMedia_withUnsupportedFileType_ShouldThrowInvalidFileException(){
            MockMultipartFile invalidFile = new MockMultipartFile(
                    "file",
                    "documnet.pdf",
                    "application/pdf",
                    "pdf content".getBytes()
            );

            assertThatThrownBy(() -> mediaIngestService.uploadMedia(invalidFile, validRequest))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("Unsupported file type");
        }

        @Test
        @DisplayName("File exceeding 500MB should throw FileSizeExceededException")
        void uploadMedia_withFileExceeding500MB_ShouldThrowFileSizeExceededException(){
            MultipartFile oversizedFile = mock(MultipartFile.class);
            lenient().when(oversizedFile.isEmpty()).thenReturn(false);
            lenient().when(oversizedFile.getSize()).thenReturn(600L * 1024 * 1024);
            lenient().when(oversizedFile.getContentType()).thenReturn("audio/mpeg");

            assertThatThrownBy(() -> mediaIngestService.uploadMedia(oversizedFile, validRequest))
                    .isInstanceOf(FileSizeExceededException.class)
                    .hasMessageContaining("500MB");
        }

        @Test
        @DisplayName("Duplicate file should throw DuplicateFileException")
        void uploadMedia_WithDuplicateFile_ShouldThrowDuplicateFileException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "duplicate.mp3",
                    "audio/mpeg",
                    "duplicate content".getBytes()
            );

            lenient().when(mediaAssetRepository.existsByChecksum(anyString())).thenReturn(true);

            assertThatThrownBy(() -> mediaIngestService.uploadMedia(file, validRequest))
                    .isInstanceOf(FileUploadException.class)
                    .hasRootCauseInstanceOf(DuplicateFileException.class);
        }

        // ================================================ Update Status Tests ==========================================================

        @Nested
        @DisplayName("updateMediaStatus() tests")
        class UpdateMediaStatusTests{
            @Test
            @DisplayName("Update status to COMPLETED should succeed")
            void updateMediaStatus_ToCompleted_ShouldSetProcessedAt(){
                String fileKey = "zoom_test-123";
                MediaAsset existingAsset = MediaAsset.builder()
                        .id(1L)
                        .fileKey(fileKey)
                        .status(MediaAsset.MediaStatus.PROCESSING)
                        .build();

                when(mediaAssetRepository.findByFileKey(fileKey)).thenReturn(Optional.of(existingAsset));
                when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(e -> e.getArgument(0));

                mediaIngestService.updateMediaStatus(fileKey, MediaAsset.MediaStatus.COMPLETED);
                assertThat(existingAsset.getProcessedAt()).isNotNull();
                assertThat(existingAsset.getStatus()).isEqualTo(MediaAsset.MediaStatus.COMPLETED);
                verify(mediaAssetRepository).save(existingAsset);
            }
        }

        @Test
        @DisplayName("Update status to PROCESSİNG should not set processedAt")
        void updateMediaStatus_ToProcessing_ShouldNotSetProcessedAt(){
            String fileKey = "zoom_test-456";
            MediaAsset existingAsset = MediaAsset.builder()
                    .id(1L)
                    .fileKey(fileKey)
                    .status(MediaAsset.MediaStatus.PENDING)
                    .build();

            when(mediaAssetRepository.findByFileKey(fileKey)).thenReturn(Optional.of(existingAsset));
            when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(inv -> inv.getArgument(0));

            mediaIngestService.updateMediaStatus(fileKey, MediaAsset.MediaStatus.PROCESSING);

            assertThat(existingAsset.getStatus()).isEqualTo(MediaAsset.MediaStatus.PROCESSING);
            assertThat(existingAsset.getProcessedAt()).isNull();
        }

        @Test
        @DisplayName("Non-existent fileKey should throw MediaAssetNotFoundException")
        void updateMediaStatus_WithInvalidFileKey_ShouldThrowException() {
            String invalidFileKey = "non-existent-key";
            when(mediaAssetRepository.findByFileKey(invalidFileKey)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    mediaIngestService.updateMediaStatus(invalidFileKey, MediaAsset.MediaStatus.COMPLETED))
                    .isInstanceOf(MediaAssetNotFoundException.class)
                    .hasMessageContaining(invalidFileKey);
        }
    }

    @Nested
    @DisplayName("Platform-specific Tests")
    class PlatformTests{
        @Test
        @DisplayName("Zoom platform should generate correct fileKey prefix")
        void uploadMedia_ZoomPlatform_ShouldHaveCorrectPrefix() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "zoom-meeting.mp4", "video/mp4", "content".getBytes());

            MediaUploadRequest zoomRequest = MediaUploadRequest.builder()
                    .meetingId("zoom-meeting-1")
                    .platform("ZOOM")
                    .build();

            when(mediaAssetRepository.existsByChecksum(anyString())).thenReturn(false);
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> {
                MediaAsset a = inv.getArgument(0);
                a.setId(1L);
                return a;
            });
            when(meetingMediaRepository.save(any())).thenAnswer(inv -> {
                MeetingMedia m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });
            when(outBoxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MediaUploadResponse response = mediaIngestService.uploadMedia(file, zoomRequest);

            assertThat(response.getFileKey()).startsWith("zoom_");
        }

        @Test
        @DisplayName("Teams platform should generate correct fileKey prefix")
        void uploadMedia_TeamsPlatform_ShouldHaveCorrectPrefix() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "teams-meeting.mp4", "video/mp4", "content".getBytes());

            MediaUploadRequest teamsRequest = MediaUploadRequest.builder()
                    .meetingId("teams-meeting-1")
                    .platform("TEAMS")
                    .build();

            when(mediaAssetRepository.existsByChecksum(anyString())).thenReturn(false);
            when(mediaAssetRepository.save(any())).thenAnswer(inv -> {
                MediaAsset a = inv.getArgument(0);
                a.setId(1L);
                return a;
            });
            when(meetingMediaRepository.save(any())).thenAnswer(inv -> {
                MeetingMedia m = inv.getArgument(0);
                m.setId(1L);
                return m;
            });
            when(outBoxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MediaUploadResponse response = mediaIngestService.uploadMedia(file, teamsRequest);

            assertThat(response.getFileKey()).startsWith("teams_");
        }
    }
}
