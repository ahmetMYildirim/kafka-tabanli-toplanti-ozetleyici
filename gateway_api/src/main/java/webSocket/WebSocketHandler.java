package webSocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * WebSocketHandler - WebSocket bağlantıları ve mesajlarını yöneten sınıf
 * 
 * Bu sınıf, UI istemcilerinden gelen WebSocket bağlantıları ve mesajlarını işler.
 * Gerçek zamanlı bildirim sistemi için temel altyapıyı sağlar.
 * 
 * Desteklenen Mesaj Tipleri:
 * - SUBSCRIBE_MEETING: Belirli bir toplantıya abone olma
 * - UNSUBSCRIBE_MEETING: Abonelikten çıkma
 * - PING: Bağlantı canlılık kontrolü
 * 
 * Bağlantı Yaşam Döngüsü:
 * 1. afterConnectionEstablished: Bağlantı kuruldu
 * 2. handleTextMessage: Mesaj alındı ve işlendi
 * 3. afterConnectionClosed: Bağlantı kapatıldı
 * 
 * @author Ahmet
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {
    
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Yeni WebSocket bağlantısı kurulduğunda çağırılır.
     * Oturum yöneticisine yeni oturumu kaydeder.
     * 
     * @param session Yeni kurulan WebSocket oturumu
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessionManager.addSession(session);
        
        log.info("Yeni WebSocket baglantisi kuruldu. Oturum ID: {}", sessionId);
        
        String welcomeMessage = createResponse("CONNECTED", "Baglanti basarili. Oturum: " + sessionId);
        sendMessage(session, welcomeMessage);
    }

    /**
     * WebSocket bağlantısı kapatıldığında çağırılır.
     * Oturumu ve tüm abonelikleri temizler.
     * 
     * @param session Kapatılan WebSocket oturumu
     * @param status Bağlantı kapatma nedeni
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessionManager.removeSession(session);
        
        log.info("WebSocket baglantisi kapatildi. Oturum: {}, Neden: {}", 
                sessionId, status.getReason());
    }

    /**
     * WebSocket üzerinden metin mesajı alındığında çağırılır.
     * Mesaj tipine göre uygun işlemi gerçekleştirir.
     * 
     * @param session Mesajı gönderen oturum
     * @param message Alınan metin mesajı
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("WebSocket mesaji alindi: {}", payload);

        try {
            JsonNode json = objectMapper.readTree(payload);
            String type = json.get("type").asText();
            
            switch (type) {
                case "SUBSCRIBE_MEETING" -> handleSubscribe(session, json);
                case "UNSUBSCRIBE_MEETING" -> handleUnsubscribe(session, json);
                case "PING" -> handlePing(session);
                default -> handleUnknown(session, type);
            }
            
        } catch (Exception e) {
            log.error("Mesaj isleme hatasi: {}", e.getMessage());
            String errorMessage = createResponse("ERROR", "Mesaj isleme hatasi: " + e.getMessage());
            sendMessage(session, errorMessage);
        }
    }
    
    /**
     * Toplantı abonelik isteğini işler.
     */
    private void handleSubscribe(WebSocketSession session, JsonNode json) {
        String meetingId = json.get("meetingId").asText();
        sessionManager.subscribeToMeeting(session, meetingId);
        
        String response = createResponse("SUBSCRIBED", "Toplantiya abone olundu: " + meetingId);
        sendMessage(session, response);
        
        log.info("Toplanti aboneligi eklendi. Oturum: {}, Toplanti: {}", session.getId(), meetingId);
    }
    
    /**
     * Abonelik iptal isteğini işler.
     */
    private void handleUnsubscribe(WebSocketSession session, JsonNode json) {
        String meetingId = json.get("meetingId").asText();
        sessionManager.unsubscribeFromMeeting(session, meetingId);
        
        String response = createResponse("UNSUBSCRIBED", "Abonelik iptal edildi: " + meetingId);
        sendMessage(session, response);
        
        log.info("Toplanti aboneligi iptal edildi. Oturum: {}, Toplanti: {}", session.getId(), meetingId);
    }
    
    /**
     * Canlılık kontrolü (PING) isteğine yanıt verir.
     */
    private void handlePing(WebSocketSession session) {
        String response = createResponse("PONG", null);
        sendMessage(session, response);
    }
    
    /**
     * Bilinmeyen mesaj tipi için hata yanıtı gönderir.
     */
    private void handleUnknown(WebSocketSession session, String type) {
        String errorMessage = createResponse("ERROR", "Bilinmeyen mesaj tipi: " + type);
        sendMessage(session, errorMessage);
        
        log.warn("Bilinmeyen mesaj tipi alindi: {}", type);
    }

    /**
     * WebSocket oturumuna mesaj gönderir.
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            log.error("Mesaj gonderim hatasi. Oturum: {}, Hata: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * JSON formatında yanıt mesajı oluşturur.
     */
    private String createResponse(String type, String message) {
        String msg = (message != null) ? message : "";
        return String.format("{\"type\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}", 
                type, msg, System.currentTimeMillis());
    }
}
