package config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import webSocket.WebSocketHandler;

/**
 * WebSocketConfig - WebSocket bağlantı yapılandırması
 * 
 * Bu sınıf, gerçek zamanlı bildirimler için WebSocket
 * endpoint'lerini ve handler'larını yapılandırır.
 * 
 * Endpoint'ler:
 * - /ws/meetings : Toplantı bildirimleri için WebSocket bağlantısı
 * 
 * Özellikler:
 * - Tüm origin'lerden bağlantıya izin verilir
 * - Otomatik ping/pong ile bağlantı canlılığı kontrolü
 * 
 * @author Ahmet
 * @version 1.0
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;

    /**
     * WebSocket handler'larını kaydeder.
     * 
     * @param registry WebSocket handler kayıt nesnesi
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/meetings")
                .setAllowedOrigins("*");
    }
}
