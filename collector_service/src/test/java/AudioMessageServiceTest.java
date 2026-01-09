import org.example.collector_service.domain.model.AudioMessage;
import org.example.collector_service.exception.MediaAssetNotFoundException;
import org.example.collector_service.outbox.OutboxEventPublisher;
import org.example.collector_service.repository.AudioMessageRepository;
import org.example.collector_service.service.AudioMessageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AudioMessageService Unit Tests")
public class AudioMessageServiceTest {

    @Mock
    private AudioMessageRepository audioMessageRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private AudioMessageService audioMessageService;

    @Nested
    @DisplayName("processAndSaveAudioMessage() tests")
    class ProcessAndSaveTests {

        @Test
        @DisplayName("Should save audio message and publish created event")
        void processAndSaveAudioMessage_ShouldSaveAndPublishEvent() {
            AudioMessage audioMessage = AudioMessage.builder()
                    .author("TestUser")
                    .audioUrl("https:
                    .build();

            AudioMessage savedMessage = AudioMessage.builder()
                    .id(1L)
                    .author("TestUser")
                    .audioUrl("https:
                    .build();

            when(audioMessageRepository.save(any(AudioMessage.class))).thenReturn(savedMessage);

            audioMessageService.processAndSaveAudioMessage(audioMessage);

            verify(audioMessageRepository).save(audioMessage);
            verify(outboxEventPublisher).publishCreated(eq(savedMessage), eq("1"), eq("AudioMessage"));
        }

        @Test
        @DisplayName("Should handle audio message with transcription")
        void processAndSaveAudioMessage_WithTranscription_ShouldSave() {
            AudioMessage audioMessage = AudioMessage.builder()
                    .author("TestUser")
                    .audioUrl("https:
                    .transcription("Hello world transcription")
                    .build();

            AudioMessage savedMessage = AudioMessage.builder()
                    .id(2L)
                    .author("TestUser")
                    .audioUrl("https:
                    .transcription("Hello world transcription")
                    .build();

            when(audioMessageRepository.save(any(AudioMessage.class))).thenReturn(savedMessage);

            audioMessageService.processAndSaveAudioMessage(audioMessage);

            ArgumentCaptor<AudioMessage> captor = ArgumentCaptor.forClass(AudioMessage.class);
            verify(audioMessageRepository).save(captor.capture());
            assertThat(captor.getValue().getTranscription()).isEqualTo("Hello world transcription");
        }
    }

    @Nested
    @DisplayName("updateAudioMessage() tests")
    class UpdateAudioMessageTests {

        @Test
        @DisplayName("Should update existing audio message")
        void updateAudioMessage_ExistingMessage_ShouldUpdate() {
            Long messageId = 1L;

            AudioMessage existingMessage = AudioMessage.builder()
                    .id(messageId)
                    .author("TestUser")
                    .audioUrl("https:
                    .build();

            AudioMessage updateRequest = AudioMessage.builder()
                    .id(messageId)
                    .transcription("Updated transcription")
                    .audioUrl("https:
                    .build();

            when(audioMessageRepository.findById(messageId)).thenReturn(Optional.of(existingMessage));
            when(audioMessageRepository.save(any(AudioMessage.class))).thenReturn(existingMessage);

            audioMessageService.updateAudioMessage(updateRequest);

            assertThat(existingMessage.getTranscription()).isEqualTo("Updated transcription");
            assertThat(existingMessage.getAudioUrl()).isEqualTo("https:
            verify(outboxEventPublisher).publishUpdated(eq(existingMessage), eq("1"), eq("AudioMessage"));
        }

        @Test
        @DisplayName("Should throw exception for non-existent message")
        void updateAudioMessage_NonExistentMessage_ShouldThrow() {
            Long invalidId = 999L;

            AudioMessage updateRequest = AudioMessage.builder()
                    .id(invalidId)
                    .transcription("Updated transcription")
                    .build();

            when(audioMessageRepository.findById(invalidId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> audioMessageService.updateAudioMessage(updateRequest))
                    .isInstanceOf(MediaAssetNotFoundException.class)
                    .hasMessageContaining(invalidId.toString());
        }

        @Test
        @DisplayName("Should update only transcription when audioUrl is null")
        void updateAudioMessage_OnlyTranscription_ShouldUpdateTranscription() {
            Long messageId = 1L;

            AudioMessage existingMessage = AudioMessage.builder()
                    .id(messageId)
                    .author("TestUser")
                    .audioUrl("https:
                    .build();

            AudioMessage updateRequest = AudioMessage.builder()
                    .id(messageId)
                    .transcription("New transcription only")
                    .audioUrl(null)
                    .build();

            when(audioMessageRepository.findById(messageId)).thenReturn(Optional.of(existingMessage));
            when(audioMessageRepository.save(any(AudioMessage.class))).thenReturn(existingMessage);

            audioMessageService.updateAudioMessage(updateRequest);

            assertThat(existingMessage.getTranscription()).isEqualTo("New transcription only");
            assertThat(existingMessage.getAudioUrl()).isNull();
            verify(audioMessageRepository).save(existingMessage);
        }
    }
}

