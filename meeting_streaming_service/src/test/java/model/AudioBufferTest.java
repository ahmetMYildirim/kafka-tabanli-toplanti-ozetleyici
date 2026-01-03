package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AudioBuffer Unit Tests")
class AudioBufferTest {

    private AudioBuffer audioBuffer;

    @BeforeEach
    void setUp() {
        audioBuffer = new AudioBuffer();
    }

    @Test
    @DisplayName("New buffer should be empty")
    void newBufferShouldBeEmpty() {
        assertEquals(0, audioBuffer.getBufferSize());
        assertEquals(0, audioBuffer.getCombinedAudio().length);
    }

    @Test
    @DisplayName("Adding audio chunk should increase buffer size")
    void addAudioChunkShouldIncreaseBufferSize() {
        byte[] chunk = {1, 2, 3, 4, 5};
        audioBuffer.addAudioChunk(chunk);

        assertEquals(5, audioBuffer.getBufferSize());
    }

    @Test
    @DisplayName("Multiple chunks should be combined correctly")
    void multipleChunksShouldBeCombinedCorrectly() {
        byte[] chunk1 = {1, 2, 3};
        byte[] chunk2 = {4, 5, 6};
        byte[] chunk3 = {7, 8, 9};

        audioBuffer.addAudioChunk(chunk1);
        audioBuffer.addAudioChunk(chunk2);
        audioBuffer.addAudioChunk(chunk3);

        assertEquals(9, audioBuffer.getBufferSize());

        byte[] combined = audioBuffer.getCombinedAudio();
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, combined);
    }

    @Test
    @DisplayName("Adding null chunk should not change buffer")
    void addNullChunkShouldNotChangeBuffer() {
        audioBuffer.addAudioChunk(null);
        assertEquals(0, audioBuffer.getBufferSize());
    }

    @Test
    @DisplayName("Adding empty chunk should not change buffer")
    void addEmptyChunkShouldNotChangeBuffer() {
        audioBuffer.addAudioChunk(new byte[0]);
        assertEquals(0, audioBuffer.getBufferSize());
    }

    @Test
    @DisplayName("Clear should reset buffer")
    void clearShouldResetBuffer() {
        byte[] chunk = {1, 2, 3, 4, 5};
        audioBuffer.addAudioChunk(chunk);
        assertEquals(5, audioBuffer.getBufferSize());

        audioBuffer.clear();
        assertEquals(0, audioBuffer.getBufferSize());
        assertEquals(0, audioBuffer.getCombinedAudio().length);
    }

    @Test
    @DisplayName("Should set UserId and UserName")
    void shouldSetUserIdAndUserName() {
        audioBuffer.setUserId("user-123");
        audioBuffer.setUserName("Test User");

        assertEquals("user-123", audioBuffer.getUserId());
        assertEquals("Test User", audioBuffer.getUserName());
    }

    @Test
    @DisplayName("getCombinedAudio should return current data")
    void getCombinedAudioShouldReturnCurrentData() {
        byte[] chunk = {10, 20, 30};
        audioBuffer.addAudioChunk(chunk);

        byte[] result = audioBuffer.getCombinedAudio();
        assertNotNull(result);
        assertArrayEquals(chunk, result);
    }

    @Test
    @DisplayName("Should handle large data sets correctly")
    void shouldHandleLargeDataSets() {
        byte[] largeChunk = new byte[10000];
        for (int i = 0; i < largeChunk.length; i++) {
            largeChunk[i] = (byte) (i % 256);
        }

        audioBuffer.addAudioChunk(largeChunk);
        assertEquals(10000, audioBuffer.getBufferSize());

        byte[] combined = audioBuffer.getCombinedAudio();
        assertArrayEquals(largeChunk, combined);
    }
}

