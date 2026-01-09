package com.toplanti.dashboard.service;

import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocketService - Gerçek zamanlı güncellemeler için WebSocket istemcisi
 * @author Ömer
 * @version 1.0
 */
public class WebSocketService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketService.class);
    private static final String DEFAULT_WS_URL = "ws://localhost:8084/ws/meetings";
    private static final int RECONNECT_DELAY_SECONDS = 5;

    private final String wsUrl;
    private WebSocketClient client;
    private final ScheduledExecutorService reconnectExecutor;

    private Consumer<String> onNewSummary;
    private Consumer<String> onNewTranscription;
    private Consumer<String> onNewActionItem;
    private Consumer<Void> onConnected;
    private Consumer<String> onDisconnected;
    private Consumer<Exception> onError;

    private boolean shouldReconnect = true;

    public WebSocketService() {
        this(DEFAULT_WS_URL);
    }

    public WebSocketService(String wsUrl) {
        this.wsUrl = wsUrl;
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * WebSocket bağlantısını başlatır
     * @param authToken Kimlik doğrulama token'ı
     */
    public void connect(String authToken) {
        try {
            String url = authToken != null && !authToken.isEmpty()
                    ? wsUrl + "?token=" + authToken
                    : wsUrl;

            log.info("WebSocket bağlantısı kuruluyor: {}", wsUrl);

            client = new WebSocketClient(new URI(url)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("WebSocket bağlantısı kuruldu");
                    if (onConnected != null) {
                        Platform.runLater(() -> onConnected.accept(null));
                    }
                }

                @Override
                public void onMessage(String message) {
                    log.debug("WebSocket mesajı alındı: {}", message);
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("WebSocket bağlantısı kapandı: {} (kod: {})", reason, code);
                    if (onDisconnected != null) {
                        Platform.runLater(() -> onDisconnected.accept(reason));
                    }

                    if (shouldReconnect) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    log.error("WebSocket hatası", ex);
                    if (onError != null) {
                        Platform.runLater(() -> onError.accept(ex));
                    }
                }
            };

            client.connect();

        } catch (Exception e) {
            log.error("WebSocket bağlantısı kurulamadı", e);
            if (onError != null) {
                Platform.runLater(() -> onError.accept(e));
            }
        }
    }

    /**
     * Gelen mesajları işler ve uygun callback'leri çağırır
     */
    private void handleMessage(String message) {
        Platform.runLater(() -> {
            if (message.contains("\"type\":\"SUMMARY\"") || message.contains("\"type\":\"summary\"")) {
                if (onNewSummary != null) {
                    onNewSummary.accept(message);
                }
            } else if (message.contains("\"type\":\"TRANSCRIPTION\"") || message.contains("\"type\":\"transcription\"")) {
                if (onNewTranscription != null) {
                    onNewTranscription.accept(message);
                }
            } else if (message.contains("\"type\":\"ACTION_ITEMS\"") || message.contains("\"type\":\"action_items\"")) {
                if (onNewActionItem != null) {
                    onNewActionItem.accept(message);
                }
            }
        });
    }

    /**
     * Yeniden bağlanmayı planlar
     */
    private void scheduleReconnect() {
        log.info("{}  saniye sonra yeniden bağlanılacak...", RECONNECT_DELAY_SECONDS);
        reconnectExecutor.schedule(() -> {
            if (shouldReconnect && (client == null || !client.isOpen())) {
                log.info("Yeniden bağlanılıyor...");
                client.reconnect();
            }
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Yeni özet bildirimi callback'i
     */
    public void setOnNewSummary(Consumer<String> callback) {
        this.onNewSummary = callback;
    }

    /**
     * Yeni transkript bildirimi callback'i
     */
    public void setOnNewTranscription(Consumer<String> callback) {
        this.onNewTranscription = callback;
    }

    /**
     * Yeni görev bildirimi callback'i
     */
    public void setOnNewActionItem(Consumer<String> callback) {
        this.onNewActionItem = callback;
    }

    /**
     * Bağlantı kurulduğunda callback
     */
    public void setOnConnected(Consumer<Void> callback) {
        this.onConnected = callback;
    }

    /**
     * Bağlantı kesildiğinde callback
     */
    public void setOnDisconnected(Consumer<String> callback) {
        this.onDisconnected = callback;
    }

    /**
     * Hata oluştuğunda callback
     */
    public void setOnError(Consumer<Exception> callback) {
        this.onError = callback;
    }

    /**
     * Bağlantıyı kapatır
     */
    public void disconnect() {
        shouldReconnect = false;
        if (client != null) {
            client.close();
        }
        reconnectExecutor.shutdown();
        log.info("WebSocket bağlantısı kapatıldı");
    }

    /**
     * Bağlantı durumunu döndürür
     */
    public boolean isConnected() {
        return client != null && client.isOpen();
    }
}

