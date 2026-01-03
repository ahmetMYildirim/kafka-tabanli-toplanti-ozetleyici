import org.example.collector_service.domain.model.OutBoxEvent;
import org.example.collector_service.relayer.OutBoxEventRelayer;
import org.example.collector_service.repository.OutBoxEventRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutBoxEventRelayer Unit Tests")
public class OutBoxEventRelayerTest {

    @Mock
    private OutBoxEventRepository outBoxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private OutBoxEventRelayer outBoxEventRelayer;

    @Nested
    @DisplayName("relayEvents() tests")
    class RelayEventsTests {

        @Test
        @DisplayName("Should relay unprocessed events to Kafka")
        void relayEvents_WithUnprocessedEvents_ShouldSendToKafka() {
            OutBoxEvent event1 = OutBoxEvent.builder()
                    .id(1L)
                    .aggregateType("Meeting")
                    .aggregateId("1")
                    .eventType("Created")
                    .payload("{\"id\":1,\"title\":\"Test Meeting\"}")
                    .processed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            OutBoxEvent event2 = OutBoxEvent.builder()
                    .id(2L)
                    .aggregateType("Message")
                    .aggregateId("2")
                    .eventType("Created")
                    .payload("{\"id\":2,\"content\":\"Hello\"}")
                    .processed(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(outBoxEventRepository.find100Unprocessed(any(PageRequest.class)))
                    .thenReturn(List.of(event1, event2));

            outBoxEventRelayer.relayEvents();

            verify(kafkaTemplate).send(eq("team-messages"), eq("{\"id\":1,\"title\":\"Test Meeting\"}"));
            verify(kafkaTemplate).send(eq("team-messages"), eq("{\"id\":2,\"content\":\"Hello\"}"));

            assertThat(event1.isProcessed()).isTrue();
            assertThat(event2.isProcessed()).isTrue();

            verify(outBoxEventRepository, times(2)).save(any(OutBoxEvent.class));
        }

        @Test
        @DisplayName("Should do nothing when no unprocessed events")
        void relayEvents_NoEvents_ShouldDoNothing() {
            when(outBoxEventRepository.find100Unprocessed(any(PageRequest.class)))
                    .thenReturn(Collections.emptyList());

            outBoxEventRelayer.relayEvents();

            verify(kafkaTemplate, never()).send(anyString(), anyString());
            verify(outBoxEventRepository, never()).save(any(OutBoxEvent.class));
        }

        @Test
        @DisplayName("Should mark event as processed after successful send")
        void relayEvents_SuccessfulSend_ShouldMarkAsProcessed() {
            OutBoxEvent event = OutBoxEvent.builder()
                    .id(1L)
                    .aggregateType("AudioMessage")
                    .aggregateId("100")
                    .eventType("Created")
                    .payload("{\"id\":100}")
                    .processed(false)
                    .build();

            when(outBoxEventRepository.find100Unprocessed(any(PageRequest.class)))
                    .thenReturn(List.of(event));

            outBoxEventRelayer.relayEvents();

            ArgumentCaptor<OutBoxEvent> captor = ArgumentCaptor.forClass(OutBoxEvent.class);
            verify(outBoxEventRepository).save(captor.capture());

            assertThat(captor.getValue().isProcessed()).isTrue();
            assertThat(captor.getValue().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when Kafka send fails")
        void relayEvents_KafkaFailure_ShouldThrowException() {
            OutBoxEvent event = OutBoxEvent.builder()
                    .id(1L)
                    .aggregateType("Meeting")
                    .aggregateId("1")
                    .payload("{\"id\":1}")
                    .processed(false)
                    .build();

            when(outBoxEventRepository.find100Unprocessed(any(PageRequest.class)))
                    .thenReturn(List.of(event));
            when(kafkaTemplate.send(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Kafka connection failed"));

            assertThatThrownBy(() -> outBoxEventRelayer.relayEvents())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Could not relay event");

            // Event should not be marked as processed
            assertThat(event.isProcessed()).isFalse();
        }

        @Test
        @DisplayName("Should process events in order")
        void relayEvents_MultipleEvents_ShouldProcessInOrder() {
            OutBoxEvent event1 = OutBoxEvent.builder()
                    .id(1L)
                    .payload("first")
                    .processed(false)
                    .build();

            OutBoxEvent event2 = OutBoxEvent.builder()
                    .id(2L)
                    .payload("second")
                    .processed(false)
                    .build();

            OutBoxEvent event3 = OutBoxEvent.builder()
                    .id(3L)
                    .payload("third")
                    .processed(false)
                    .build();

            when(outBoxEventRepository.find100Unprocessed(any(PageRequest.class)))
                    .thenReturn(List.of(event1, event2, event3));

            outBoxEventRelayer.relayEvents();

            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate, times(3)).send(eq("team-messages"), payloadCaptor.capture());

            List<String> sentPayloads = payloadCaptor.getAllValues();
            assertThat(sentPayloads).containsExactly("first", "second", "third");
        }
    }
}

