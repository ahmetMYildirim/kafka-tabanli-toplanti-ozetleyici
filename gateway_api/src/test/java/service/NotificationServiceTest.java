package service;

import model.event.ProcessedActionItem;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import webSocket.SessionManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private SessionManager sessionManager;

    @Mock
    private WebSocketSession webSocketSession;

    @InjectMocks
    private NotificationService notificationService;

    @Nested
    @DisplayName("Summary Notification Tests")
    class SummaryNotificationTests{
        @Test
        @DisplayName("Should send summary notification to subscribers")
        void notifyNewSummary_ShouldBroadcastToSubscribers() throws Exception {
            ProcessedSummary summary = ProcessedSummary.builder()
                    .meetingId("meeting-123")
                    .platform("DISCORD")
                    .summary("Test özet")
                    .build();

            Set<WebSocketSession> subscribers = new HashSet<>();
            subscribers.add(webSocketSession);

            when(sessionManager.getMeetingSubscribers("meeting-123")).thenReturn(subscribers);
            when(sessionManager.getAllSessions()).thenReturn(subscribers);
            when(webSocketSession.isOpen()).thenReturn(true);

            notificationService.notifyNewSummary(summary);

            verify(sessionManager).getMeetingSubscribers("meeting-123");
            verify(webSocketSession, atLeastOnce()).sendMessage(any(TextMessage.class));
        }

        @Test
        @DisplayName("Should not send to closed sessions")
        void notifyNewSummary_WhenSessionClosed_ShouldNotSend() throws Exception {
            ProcessedSummary summary = ProcessedSummary.builder()
                    .meetingId("meeting-123")
                    .platform("DISCORD")
                    .build();

            Set<WebSocketSession> subscribers = new HashSet<>();
            subscribers.add(webSocketSession);

            when(sessionManager.getMeetingSubscribers("meeting-123")).thenReturn(subscribers);
            when(sessionManager.getAllSessions()).thenReturn(subscribers);
            when(webSocketSession.isOpen()).thenReturn(false);

            notificationService.notifyNewSummary(summary);

            verify(webSocketSession, never()).sendMessage(any(TextMessage.class));
        }
    }

    @Nested
    @DisplayName("Transcription Notification Tests")
    class TranscriptionNotificationTests {

        @Test
        @DisplayName("Should send transcription notification")
        void notifyNewTranscript_ShouldBroadcast() throws Exception {
            ProcessedTranscription transcription = ProcessedTranscription.builder()
                    .meetingId("meeting-456")
                    .fullTranscription("Test transkript")
                    .build();

            Set<WebSocketSession> sessions = new HashSet<>();
            sessions.add(webSocketSession);

            when(sessionManager.getMeetingSubscribers("meeting-456")).thenReturn(sessions);
            when(sessionManager.getAllSessions()).thenReturn(sessions);
            when(webSocketSession.isOpen()).thenReturn(true);

            notificationService.notifyNewTranscript(transcription);

            verify(sessionManager).getMeetingSubscribers("meeting-456");
        }
    }

    @Nested
    @DisplayName("ActionItems Notification Tests")
    class ActionItemsNotificationTests {

        @Test
        @DisplayName("Should send action items notification")
        void notifyNewActionItems_ShouldBroadcast() throws Exception {
            ProcessedActionItem actionItems = ProcessedActionItem.builder()
                    .meetingId("meeting-789")
                    .actionItems(List.of("Task 1", "Task 2"))
                    .build();

            Set<WebSocketSession> sessions = new HashSet<>();
            sessions.add(webSocketSession);

            when(sessionManager.getMeetingSubscribers("meeting-789")).thenReturn(sessions);
            when(sessionManager.getAllSessions()).thenReturn(sessions);
            when(webSocketSession.isOpen()).thenReturn(true);

            notificationService.notifyNewActionItems(actionItems);

            verify(sessionManager).getMeetingSubscribers("meeting-789");
            verify(webSocketSession, atLeastOnce()).sendMessage(any(TextMessage.class));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle WebSocket send exception gracefully")
        void sendMessage_WhenException_ShouldLogError() throws Exception {
            ProcessedSummary summary = ProcessedSummary.builder()
                    .meetingId("meeting-error")
                    .platform("DISCORD")
                    .build();

            Set<WebSocketSession> sessions = new HashSet<>();
            sessions.add(webSocketSession);

            when(sessionManager.getMeetingSubscribers("meeting-error")).thenReturn(sessions);
            when(sessionManager.getAllSessions()).thenReturn(sessions);
            when(webSocketSession.isOpen()).thenReturn(true);
            doThrow(new RuntimeException("Connection error"))
                    .when(webSocketSession).sendMessage(any(TextMessage.class));

            notificationService.notifyNewSummary(summary);
        }
    }
}
