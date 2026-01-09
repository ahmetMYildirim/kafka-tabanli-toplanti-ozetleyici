import org.example.collector_service.domain.model.VoiceSession;
import org.example.collector_service.exception.MediaAssetNotFoundException;
import org.example.collector_service.outbox.OutboxEventPublisher;
import org.example.collector_service.repository.VoiceSessionRepository;
import org.example.collector_service.service.VoiceSessionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoiceSessionService Unit Tests")
public class VoiceSessionServiceTest {

    @Mock
    private VoiceSessionRepository voiceSessionRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private VoiceSessionService voiceSessionService;

    private static final String PLATFORM = "DISCORD";
    private static final String CHANNEL_ID = "channel-123";
    private static final String CHANNEL_NAME = "General Voice";
    private static final String USER_NAME = "TestUser";

    @BeforeEach
    void setUp() {
        
        Map<String, Long> activeSessions = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(voiceSessionService, "activeSessions", activeSessions);
    }

    @Nested
    @DisplayName("handleUserJoinedVoiceChannel() tests")
    class HandleUserJoinedTests {

        @Test
        @DisplayName("First user joining should start new session")
        void handleUserJoined_FirstUser_ShouldStartNewSession() {
            VoiceSession savedSession = VoiceSession.builder()
                    .id(1L)
                    .platform(PLATFORM)
                    .channelId(CHANNEL_ID)
                    .channelName(CHANNEL_NAME)
                    .startTime(LocalDateTime.now())
                    .participantCount(1)
                    .build();

            when(voiceSessionRepository.save(any(VoiceSession.class))).thenReturn(savedSession);

            voiceSessionService.handleUserJoinedVoiceChannel(PLATFORM, CHANNEL_ID, CHANNEL_NAME, USER_NAME);

            ArgumentCaptor<VoiceSession> captor = ArgumentCaptor.forClass(VoiceSession.class);
            verify(voiceSessionRepository).save(captor.capture());

            VoiceSession captured = captor.getValue();
            assertThat(captured.getPlatform()).isEqualTo(PLATFORM);
            assertThat(captured.getChannelId()).isEqualTo(CHANNEL_ID);
            assertThat(captured.getParticipantCount()).isEqualTo(1);

            verify(outboxEventPublisher).publishStarted(eq(savedSession), eq("1"), eq("VoiceSession"));
        }

        @Test
        @DisplayName("Second user joining should increment participant count")
        void handleUserJoined_SecondUser_ShouldIncrementCount() {
            
            VoiceSession existingSession = VoiceSession.builder()
                    .id(1L)
                    .platform(PLATFORM)
                    .channelId(CHANNEL_ID)
                    .channelName(CHANNEL_NAME)
                    .participantCount(1)
                    .build();

            when(voiceSessionRepository.save(any(VoiceSession.class))).thenReturn(existingSession);
            when(voiceSessionRepository.findById(1L)).thenReturn(Optional.of(existingSession));

            
            voiceSessionService.handleUserJoinedVoiceChannel(PLATFORM, CHANNEL_ID, CHANNEL_NAME, USER_NAME);

            
            voiceSessionService.handleUserJoinedVoiceChannel(PLATFORM, CHANNEL_ID, CHANNEL_NAME, "SecondUser");

            assertThat(existingSession.getParticipantCount()).isEqualTo(2);
            verify(voiceSessionRepository, times(2)).save(any(VoiceSession.class));
        }

        @Test
        @DisplayName("Session increment should throw exception for non-existent session")
        void handleUserJoined_NonExistentSession_ShouldThrowException() {
            VoiceSession savedSession = VoiceSession.builder()
                    .id(1L)
                    .platform(PLATFORM)
                    .channelId(CHANNEL_ID)
                    .channelName(CHANNEL_NAME)
                    .participantCount(1)
                    .build();

            when(voiceSessionRepository.save(any(VoiceSession.class))).thenReturn(savedSession);
            when(voiceSessionRepository.findById(1L)).thenReturn(Optional.empty());

            
            voiceSessionService.handleUserJoinedVoiceChannel(PLATFORM, CHANNEL_ID, CHANNEL_NAME, USER_NAME);

            
            assertThatThrownBy(() ->
                voiceSessionService.handleUserJoinedVoiceChannel(PLATFORM, CHANNEL_ID, CHANNEL_NAME, "SecondUser"))
                .isInstanceOf(MediaAssetNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("handleUserLeftVoiceChannel() tests")
    class HandleUserLeftTests {

        @Test
        @DisplayName("Last user leaving should end session")
        void handleUserLeft_LastUser_ShouldEndSession() {
            VoiceSession existingSession = VoiceSession.builder()
                    .id(1L)
                    .platform(PLATFORM)
                    .channelId(CHANNEL_ID)
                    .channelName(CHANNEL_NAME)
                    .participantCount(1)
                    .startTime(LocalDateTime.now().minusMinutes(30))
                    .build();

            when(voiceSessionRepository.save(any(VoiceSession.class))).thenReturn(existingSession);
            when(voiceSessionRepository.findById(1L)).thenReturn(Optional.of(existingSession));

            
            voiceSessionService.handleUserJoinedVoiceChannel(PLATFORM, CHANNEL_ID, CHANNEL_NAME, USER_NAME);

            
            voiceSessionService.handleUserLeftVoiceChannel(PLATFORM, CHANNEL_ID);

            assertThat(existingSession.getParticipantCount()).isEqualTo(0);
            assertThat(existingSession.getEndTime()).isNotNull();
            verify(outboxEventPublisher).publishEnded(eq(existingSession), eq("1"), eq("VoiceSession"));
        }

        @Test
        @DisplayName("User leaving with others remaining should just decrement count")
        void handleUserLeft_WithOthersRemaining_ShouldDecrementCount() {
            VoiceSession existingSession = VoiceSession.builder()
                    .id(1L)
                    .platform(PLATFORM)
                    .channelId(CHANNEL_ID)
                    .channelName(CHANNEL_NAME)
                    .participantCount(2)
                    .startTime(LocalDateTime.now().minusMinutes(30))
                    .build();

            when(voiceSessionRepository.save(any(VoiceSession.class))).thenReturn(existingSession);
            when(voiceSessionRepository.findById(1L)).thenReturn(Optional.of(existingSession));

            
            Map<String, Long> activeSessions = new ConcurrentHashMap<>();
            activeSessions.put(PLATFORM + "_" + CHANNEL_ID, 1L);
            ReflectionTestUtils.setField(voiceSessionService, "activeSessions", activeSessions);

            
            voiceSessionService.handleUserLeftVoiceChannel(PLATFORM, CHANNEL_ID);

            assertThat(existingSession.getParticipantCount()).isEqualTo(1);
            verify(outboxEventPublisher, never()).publishEnded(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("Leaving non-existent session should not throw")
        void handleUserLeft_NoActiveSession_ShouldDoNothing() {
            
            assertThatCode(() ->
                voiceSessionService.handleUserLeftVoiceChannel(PLATFORM, CHANNEL_ID))
                .doesNotThrowAnyException();

            verify(voiceSessionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("Session not found in repository should throw and remove from active sessions")
        void handleUserLeft_SessionNotInRepo_ShouldThrowAndCleanup() {
            Map<String, Long> activeSessions = new ConcurrentHashMap<>();
            activeSessions.put(PLATFORM + "_" + CHANNEL_ID, 999L);
            ReflectionTestUtils.setField(voiceSessionService, "activeSessions", activeSessions);

            when(voiceSessionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                voiceSessionService.handleUserLeftVoiceChannel(PLATFORM, CHANNEL_ID))
                .isInstanceOf(MediaAssetNotFoundException.class);
        }
    }
}

