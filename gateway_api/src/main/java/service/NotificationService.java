package service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.event.ProcessedActionItem;
import model.event.ProcessedSummary;
import model.event.ProcessedTranscription;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import webSocket.SessionManager;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;

/**
 * NotificationService - WebSocket üzerinden UI'a bildirim gönderen servis
 * 
 * Bu sınıf, Kafka'dan alınan yeni veriler için bağlı WebSocket istemcilerine
 * gerçek zamanlı bildirimler gönderir.
 * 
 * Bildirim Türleri:
 * - NEW_SUMMARY: Yeni toplantı özeti
 * - NEW_TRANSCRIPTION: Yeni transkript
 * - NEW_ACTION_ITEMS: Yeni görev listesi
 * 
 * Gönderim Stratejisi:
 * - İlgili toplantıya abone olan istemcilere özel bildirim
 * - Tüm bağlı istemcilere genel broadcast (dashboard için)
 * 
 * @author Ahmet
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * Yeni toplantı özeti için bildirim gönderir.
     * Hem ilgili toplantıya abone olanlara hem de tüm istemcilere gönderilir.
     * 
     * @param summary AI tarafından oluşturulan toplantı özeti
     */
    public void notifyNewSummary(ProcessedSummary summary) {
        String meetingId = summary.getMeetingId();
        String message = createNotification("NEW_SUMMARY", summary);
        
        broadcastToMeetingSubscribers(meetingId, message);
        broadcastToAll(message);
        
        log.info("Summary notification sent. Meeting: {}", meetingId);
    }

    /**
     * Yeni transkript için bildirim gönderir.
     * 
     * @param transcription AI tarafından oluşturulan transkript
     */
    public void notifyNewTranscript(ProcessedTranscription transcription) {
        String meetingId = transcription.getMeetingId();
        String message = createNotification("NEW_TRANSCRIPTION", transcription);
        
        broadcastToMeetingSubscribers(meetingId, message);
        broadcastToAll(message);
        
        log.info("Transcription notification sent. Meeting: {}", meetingId);
    }

    /**
     * Yeni görev listesi için bildirim gönderir.
     * 
     * @param actionItems AI tarafından belirlenen görevler
     */
    public void notifyNewActionItems(ProcessedActionItem actionItems) {
        String meetingId = actionItems.getMeetingId();
        String message = createNotification("NEW_ACTION_ITEMS", actionItems);
        
        broadcastToMeetingSubscribers(meetingId, message);
        broadcastToAll(message);
        
        log.info("Action items notification sent. Meeting: {}", meetingId);
    }

    /**
     * Belirli bir toplantıya abone olan istemcilere mesaj gönderir.
     */
    private void broadcastToMeetingSubscribers(String meetingId, String message) {
        Set<WebSocketSession> subscribers = sessionManager.getMeetingSubscribers(meetingId);
        subscribers.forEach(session -> sendMessage(session, message));
        
        log.debug("Sent to meeting subscribers. ID: {}, Subscribers: {}", meetingId, subscribers.size());
    }

    /**
     * Tüm bağlı WebSocket istemcilerine mesaj gönderir.
     * Dashboard güncelleme bildirimleri için kullanılır.
     */
    private void broadcastToAll(String message) {
        Set<WebSocketSession> allSessions = sessionManager.getAllSessions();
        allSessions.forEach(session -> sendMessage(session, message));
    }

    /**
     * Tek bir WebSocket oturumuna mesaj gönderir.
     * Oturum kapalı ise hata loglanır.
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error("WebSocket message send error. Session: {}, Error: {}", 
                    session.getId(), e.getMessage());
        }
    }

    /**
     * Bildirim JSON mesajını oluşturur.
     * 
     * @param type Bildirim kategorisi
     * @param data Gönderilecek veri objesi
     * @return JSON formatında bildirim metni
     */
    private <T> String createNotification(String type, T data) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", type);
            notification.put("data", data);
            notification.put("timestamp", System.currentTimeMillis());
            
            return objectMapper.writeValueAsString(notification);
        } catch (Exception e) {
            log.error("Notification JSON conversion error: {}", e.getMessage());
            return String.format("{\"type\":\"%s\",\"error\":\"Data conversion error\"}", type);
        }
    }
}
