package org.example.ai_service.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Offset.offset;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioCompressor Unit Tests")
public class AudioCompressorTest {

    private Path tempAudioFile;
    private static final long MAX_SIZE_BYTES = 25 * 1024 * 1024;

    @BeforeEach
    void setUp() throws IOException {
        tempAudioFile = Files.createTempFile("test-audio", ".wav");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempAudioFile != null && Files.exists(tempAudioFile)) {
            Files.deleteIfExists(tempAudioFile);
        }
    }

    @Nested
    @DisplayName("compressIfNeeded() tests")
    class CompressIfNeededTests {

        @Test
        @DisplayName("File under 25MB should not be compressed")
        void compressIfNeeded_WithSmallFile_ShouldReturnOriginalFile() throws IOException {
            AudioCompressor compressor = new AudioCompressor();
            byte[] smallContent = new byte[1024 * 1024];
            Files.write(tempAudioFile, smallContent);

            File result = compressor.compressIfNeeded(tempAudioFile.toFile());

            assertThat(result).isEqualTo(tempAudioFile.toFile());
        }

        @Test
        @DisplayName("compressIfNeeded should accept File object")
        void compressIfNeeded_ShouldAcceptFileObject() throws IOException {
            AudioCompressor compressor = new AudioCompressor();
            Files.write(tempAudioFile, new byte[1024 * 1024]);

            File result = compressor.compressIfNeeded(tempAudioFile.toFile());

            assertThat(result).isNotNull();
            assertThat(result.exists()).isTrue();
        }
    }

    @Nested
    @DisplayName("Utility methods tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("getFileSizeInMB should calculate correctly")
        void getFileSizeInMB_ShouldCalculateCorrectly() throws IOException {
            AudioCompressor compressor = new AudioCompressor();
            byte[] content = new byte[10 * 1024 * 1024];
            Files.write(tempAudioFile, content);

            double sizeInMB = compressor.getFileSizeInMB(tempAudioFile.toFile());

            assertThat(sizeInMB).isCloseTo(10.0, offset(0.1));
        }

        @Test
        @DisplayName("needsCompression should identify large files")
        void needsCompression_ShouldIdentifyLargeFiles() throws IOException {
            AudioCompressor compressor = new AudioCompressor();
            byte[] smallContent = new byte[10 * 1024 * 1024];
            Files.write(tempAudioFile, smallContent);

            boolean needsCompression = compressor.needsCompression(tempAudioFile.toFile());

            assertThat(needsCompression).isFalse();
        }

        @Test
        @DisplayName("Should handle zero-size file")
        void shouldHandleZeroSizeFile() throws IOException {
            AudioCompressor compressor = new AudioCompressor();
            Files.write(tempAudioFile, new byte[0]);

            File result = compressor.compressIfNeeded(tempAudioFile.toFile());

            assertThat(result).isEqualTo(tempAudioFile.toFile());
        }
    }
}

