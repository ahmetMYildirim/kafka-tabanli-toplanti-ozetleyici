import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.collector_service.domain.model.Meeting;
import org.example.collector_service.domain.model.OutBoxEvent;
import org.example.collector_service.outbox.OutboxEventPublisher;
import org.example.collector_service.repository.OutBoxEventRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxEventPublisher Unit Tests")
public class OutboxEventPubliserEvent {

    @Mock
    private OutBoxEventRepository outBoxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @Nested
    @DisplayName("publishEvent() tests")
    class PublishEventTests{

        @Test
        @DisplayName("Should create and save outbox event correctly")
        void publishEvent_ShouldSaveCorrectEvent(){
            Meeting meeting = Meeting.builder()
                    .id(1L)
                    .platform("ZOOM")
                    .title("Test Meeting")
                    .build();

            when(outBoxEventRepository.save(any(OutBoxEvent.class))).thenAnswer(e -> e.getArgument(0));

            outboxEventPublisher.publishEvent(meeting, "1", "Meeting", "Created");

            ArgumentCaptor<OutBoxEvent> eventCaptor = ArgumentCaptor.forClass(OutBoxEvent.class);
            verify(outBoxEventRepository).save(eventCaptor.capture());

            OutBoxEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getAggregateId()).isEqualTo("1");
            assertThat(savedEvent.getAggregateType()).isEqualTo("Meeting");
            assertThat(savedEvent.getEventType()).isEqualTo("Created");
            assertThat(savedEvent.getPayload()).contains("ZOOM");
            assertThat(savedEvent.isProcessed()).isFalse();
        }
    }

    @Nested
    @DisplayName("Convenience Methods Tests")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("publishCreated should use 'Created' event type")
        void publishCreated_ShouldUseCreatedEventType() {
            Meeting meeting = Meeting.builder().id(1L).build();
            when(outBoxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxEventPublisher.publishCreated(meeting, "1", "Meeting");

            ArgumentCaptor<OutBoxEvent> captor = ArgumentCaptor.forClass(OutBoxEvent.class);
            verify(outBoxEventRepository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo("Created");
        }

        @Test
        @DisplayName("publishStarted should use 'Started' event type")
        void publishStarted_ShouldUseStartedEventType() {
            Meeting meeting = Meeting.builder().id(1L).build();
            when(outBoxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxEventPublisher.publishStarted(meeting, "1", "Meeting");

            ArgumentCaptor<OutBoxEvent> captor = ArgumentCaptor.forClass(OutBoxEvent.class);
            verify(outBoxEventRepository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo("Started");
        }

        @Test
        @DisplayName("publishUpdated should use 'Updated' event type")
        void publishUpdated_ShouldUseUpdatedEventType() {
            Meeting meeting = Meeting.builder().id(1L).title("Updated Meeting").build();
            when(outBoxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxEventPublisher.publishUpdated(meeting, "1", "Meeting");

            ArgumentCaptor<OutBoxEvent> captor = ArgumentCaptor.forClass(OutBoxEvent.class);
            verify(outBoxEventRepository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo("Updated");
            assertThat(captor.getValue().getPayload()).contains("Updated Meeting");
        }

        @Test
        @DisplayName("publishEnded should use 'Ended' event type")
        void publishEnded_ShouldUseEndedEventType() {
            Meeting meeting = Meeting.builder().id(1L).build();
            when(outBoxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxEventPublisher.publishEnded(meeting, "1", "Meeting");

            ArgumentCaptor<OutBoxEvent> captor = ArgumentCaptor.forClass(OutBoxEvent.class);
            verify(outBoxEventRepository).save(captor.capture());
            assertThat(captor.getValue().getEventType()).isEqualTo("Ended");
        }
    }

    @Nested
    @DisplayName("Event Payload Tests")
    class EventPayloadTests {

        @Test
        @DisplayName("Event payload should contain all aggregate fields")
        void publishEvent_ShouldSerializeAllFields() {
            Meeting meeting = Meeting.builder()
                    .id(1L)
                    .platform("DISCORD")
                    .title("Team Standup")
                    .build();

            when(outBoxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxEventPublisher.publishCreated(meeting, "1", "Meeting");

            ArgumentCaptor<OutBoxEvent> captor = ArgumentCaptor.forClass(OutBoxEvent.class);
            verify(outBoxEventRepository).save(captor.capture());

            String payload = captor.getValue().getPayload();
            assertThat(payload).contains("DISCORD");
            assertThat(payload).contains("Team Standup");
        }

        @Test
        @DisplayName("Event should have createdAt timestamp")
        void publishEvent_ShouldHaveTimestamp() {
            Meeting meeting = Meeting.builder().id(1L).build();
            when(outBoxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            outboxEventPublisher.publishCreated(meeting, "1", "Meeting");

            ArgumentCaptor<OutBoxEvent> captor = ArgumentCaptor.forClass(OutBoxEvent.class);
            verify(outBoxEventRepository).save(captor.capture());

            assertThat(captor.getValue().getCreatedAt()).isNotNull();
        }
    }
}
