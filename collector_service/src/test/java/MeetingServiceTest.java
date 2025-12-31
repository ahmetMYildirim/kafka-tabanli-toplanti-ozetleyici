import org.example.collector_service.domain.model.Meeting;
import org.example.collector_service.exception.MediaAssetNotFoundException;
import org.example.collector_service.outbox.OutboxEventPublisher;
import org.example.collector_service.repository.MeetingRepository;
import org.example.collector_service.service.MeetingService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingService unit tests")
public class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private MeetingService meetingService;

    @Nested
    @DisplayName("startMeeting() tests")
    class startMeetingTests {

        @Test
        @DisplayName("should start meeting and publish event")
        void startMeeting_ShouldSaveandPublishEvent(){
            Meeting meeting  = Meeting.builder()
                    .platform("DISCORD")
                    .title("Test Meeting")
                    .build();

            when(meetingRepository.save(any(Meeting.class))).thenAnswer(e -> {
                Meeting m = e.getArgument(0);
                m.setId(1L);
                return m;
            });

            meetingService.startMeeting(meeting);

            assertThat(meeting.getStartTime()).isNotNull();
            verify(meetingRepository).save(meeting);
            verify(outboxEventPublisher).publishStarted(eq(meeting), eq("1"), eq("Meeting"));
        }
    }

    @Nested
    @DisplayName("endMeeting() tests")
    class EndMeetingTests{

        @Test
        @DisplayName("Should end meeting adn publish event")
        void endMeeting_ShouldUpdateAndPublishEvent() {
            Long meetingId = 1L;
            Meeting existingMeeting = Meeting.builder()
                    .id(meetingId)
                    .platform("ZOOM")
                    .startTime(LocalDateTime.now().minusHours(1))
                    .build();

            when(meetingRepository.findById(meetingId)).thenReturn(Optional.of(existingMeeting));
            when(meetingRepository.save(any(Meeting.class))).thenReturn(existingMeeting);

            meetingService.endMeeting(meetingId);

            assertThat(existingMeeting.getEndTime()).isNotNull();
            verify(meetingRepository).save(existingMeeting);
            verify(outboxEventPublisher).publishEnded(eq(existingMeeting), eq("1"), eq("Meeting"));
        }

        @Test
        @DisplayName("Should throw exception for non-existent meeting")
        void endMeeting_WithInvalidId_ShouldThrowException() {
            Long invalidId = 999L;
            when(meetingRepository.findById(invalidId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> meetingService.endMeeting(invalidId))
                    .isInstanceOf(MediaAssetNotFoundException.class);
        }
    }
}
