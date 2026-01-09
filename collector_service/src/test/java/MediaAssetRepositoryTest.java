import org.example.collector_service.MainApp;
import org.example.collector_service.domain.model.MediaAsset;
import org.example.collector_service.repository.MediaAssetRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(classes = MainApp.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("MediaAssetRepository Integration Tests")
class MediaAssetRepositoryTest {

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @BeforeEach
    void setUp() {
        mediaAssetRepository.deleteAll();
    }

    @Test
    @DisplayName("Should find MediaAsset by fileKey")
    void findByFileKey_ShouldReturnAsset() {
        MediaAsset asset = MediaAsset.builder()
                .fileKey("zoom_test-123")
                .mimeType("audio/mpeg")
                .originalFileName("test.mp3")
                .storagePath("/path/to/file")
                .status(MediaAsset.MediaStatus.PENDING)
                .checksum("abc123")
                .build();

        mediaAssetRepository.save(asset);

        Optional<MediaAsset> found = mediaAssetRepository.findByFileKey("zoom_test-123");

        assertThat(found).isPresent();
        assertThat(found.get().getFileKey()).isEqualTo("zoom_test-123");
    }

    @Test
    @DisplayName("Should check if checksum exists")
    void existsByChecksum_ShouldReturnTrue() {
        MediaAsset asset = MediaAsset.builder()
                .fileKey("zoom_test-456")
                .mimeType("video/mp4")
                .originalFileName("test.mp4")
                .storagePath("/path/to/video")
                .status(MediaAsset.MediaStatus.PENDING)
                .checksum("unique-checksum-xyz")
                .build();

        mediaAssetRepository.save(asset);

        assertThat(mediaAssetRepository.existsByChecksum("unique-checksum-xyz")).isTrue();
        assertThat(mediaAssetRepository.existsByChecksum("non-existent")).isFalse();
    }
}
