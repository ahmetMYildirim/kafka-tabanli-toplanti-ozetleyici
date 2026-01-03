import org.example.collector_service.domain.model.Message;
import org.example.collector_service.outbox.OutboxEventPublisher;
import org.example.collector_service.repository.MessageRepository;
import org.example.collector_service.service.MessageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService Unit Tests")
public class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private MessageService messageService;

    @Nested
    @DisplayName("processAndSaveMessage() tests")
    class ProcessAndSaveMessageTests {

        @Test
        @DisplayName("Should save Discord message and publish created event")
        void processAndSaveMessage_Discord_ShouldSaveAndPublish() {
            Message message = Message.builder()
                    .author("DiscordUser")
                    .content("Hello from Discord!")
                    .platform("DISCORD")
                    .build();

            Message savedMessage = Message.builder()
                    .id(1L)
                    .author("DiscordUser")
                    .content("Hello from Discord!")
                    .platform("DISCORD")
                    .build();

            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            messageService.processAndSaveMessage(message);

            verify(messageRepository).save(message);
            verify(outboxEventPublisher).publishCreated(eq(savedMessage), eq("1"), eq("Message"));
        }

        @Test
        @DisplayName("Should save Zoom message and publish created event")
        void processAndSaveMessage_Zoom_ShouldSaveAndPublish() {
            Message message = Message.builder()
                    .author("ZoomUser")
                    .content("Meeting notes from Zoom")
                    .platform("ZOOM")
                    .build();

            Message savedMessage = Message.builder()
                    .id(2L)
                    .author("ZoomUser")
                    .content("Meeting notes from Zoom")
                    .platform("ZOOM")
                    .build();

            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            messageService.processAndSaveMessage(message);

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());

            assertThat(captor.getValue().getPlatform()).isEqualTo("ZOOM");
            assertThat(captor.getValue().getContent()).isEqualTo("Meeting notes from Zoom");
        }

        @Test
        @DisplayName("Should save Teams message and publish created event")
        void processAndSaveMessage_Teams_ShouldSaveAndPublish() {
            Message message = Message.builder()
                    .author("TeamsUser")
                    .content("Teams meeting chat")
                    .platform("TEAMS")
                    .build();

            Message savedMessage = Message.builder()
                    .id(3L)
                    .author("TeamsUser")
                    .content("Teams meeting chat")
                    .platform("TEAMS")
                    .build();

            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            messageService.processAndSaveMessage(message);

            verify(outboxEventPublisher).publishCreated(eq(savedMessage), eq("3"), eq("Message"));
        }

        @Test
        @DisplayName("Should handle message with special characters")
        void processAndSaveMessage_SpecialCharacters_ShouldSave() {
            Message message = Message.builder()
                    .author("User123")
                    .content("Message with émojis 🎉 and spëcial çharacters")
                    .platform("DISCORD")
                    .build();

            Message savedMessage = Message.builder()
                    .id(4L)
                    .author("User123")
                    .content("Message with émojis 🎉 and spëcial çharacters")
                    .platform("DISCORD")
                    .build();

            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            messageService.processAndSaveMessage(message);

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            assertThat(captor.getValue().getContent()).contains("🎉");
        }

        @Test
        @DisplayName("Should handle empty content message")
        void processAndSaveMessage_EmptyContent_ShouldSave() {
            Message message = Message.builder()
                    .author("User")
                    .content("")
                    .platform("DISCORD")
                    .build();

            Message savedMessage = Message.builder()
                    .id(5L)
                    .author("User")
                    .content("")
                    .platform("DISCORD")
                    .build();

            when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

            messageService.processAndSaveMessage(message);

            verify(messageRepository).save(message);
            verify(outboxEventPublisher).publishCreated(any(), anyString(), anyString());
        }
    }
}

