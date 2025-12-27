package webSocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager - WebSocket oturum yöneticisi
 * 
 * Bu sınıf, aktif WebSocket bağlantılarını ve toplantı
 * aboneliklerini thread-safe bir şekilde yönetir.
 * 
 * Sorumluluklar:
 * - Yeni oturumları kaydetme
 * - Kapatılan oturumları temizleme
 * - Toplantı aboneliklerini yönetme
 * - Belirli bir toplantının abonelerini getirme
 * 
 * Thread Safety:
 * - CopyOnWriteArraySet ile oturum listesi
 * - ConcurrentHashMap ile abonelik haritası
 * 
 * @author Ahmet
 * @version 1.0
 */
@Component
@Slf4j
public class SessionManager {
    
    /** Tüm aktif WebSocket oturumları */
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    
    /** Toplantı ID -> Abone oturumlar eşlemesi */
    private final Map<String, Set<WebSocketSession>> meetingSubscriptions = new ConcurrentHashMap<>();

    /**
     * Yeni WebSocket oturumunu kaydeder.
     * 
     * @param session Yeni kurulan oturum
     */
    public void addSession(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket oturumu eklendi: {}", session.getId());
    }

    /**
     * WebSocket oturumunu ve tüm aboneliklerini kaldırır.
     * 
     * @param session Kaldırılacak oturum
     */
    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        meetingSubscriptions.values().forEach(set -> set.remove(session));
        log.info("WebSocket oturumu kaldirildi: {}", session.getId());
    }

    /**
     * Oturumu belirtilen toplantıya abone yapar.
     * 
     * @param session Abone olacak oturum
     * @param meetingId Abone olunacak toplantı ID'si
     */
    public void subscribeToMeeting(WebSocketSession session, String meetingId) {
        meetingSubscriptions
                .computeIfAbsent(meetingId, key -> new CopyOnWriteArraySet<>())
                .add(session);
        log.info("Toplantiya abone olundu: {}", meetingId);
    }

    /**
     * Oturumun toplantı aboneliğini iptal eder.
     * 
     * @param session Aboneliği iptal edilecek oturum
     * @param meetingId Aboneliği iptal edilecek toplantı ID'si
     */
    public void unsubscribeFromMeeting(WebSocketSession session, String meetingId) {
        Set<WebSocketSession> subscribers = meetingSubscriptions.get(meetingId);
        if (subscribers != null) {
            subscribers.remove(session);
            log.info("Toplanti aboneligi iptal edildi: {}", meetingId);
        }
    }

    /**
     * Tüm aktif oturumları döndürür.
     * 
     * @return Aktif oturum kümesi
     */
    public Set<WebSocketSession> getAllSessions() {
        return sessions;
    }

    /**
     * Belirtilen toplantının abonelerini döndürür.
     * 
     * @param meetingId Toplantı ID'si
     * @return Abone oturumlar kümesi (boş küme olabilir)
     */
    public Set<WebSocketSession> getMeetingSubscribers(String meetingId) {
        return meetingSubscriptions.getOrDefault(meetingId, Set.of());
    }
}
