package org.example.collector_service.outbox;

/**
 * OutboxPublicationException - Outbox event yayınlama sırasında oluşan hatalar için exception
 * 
 * JSON serialization hatası veya veritabanı kaydetme hatası durumunda fırlatılır.
 * Bu exception, transaction'ın rollback edilmesine neden olur.
 * 
 * Kullanım: OutboxEventPublisher tarafından internal olarak fırlatılır.
 * 
 * @author Ahmet
 * @version 1.0
 */
public class OutboxPublicationException extends RuntimeException {
    
    /**
     * Detaylı hata mesajı ile exception oluşturur.
     *
     * @param message Hata mesajı
     */
    public OutboxPublicationException(String message) {
        super(message);
    }

    /**
     * Detaylı hata mesajı ve cause ile exception oluşturur.
     *
     * @param message Hata mesajı
     * @param cause   Altta yatan exception (örn: JsonProcessingException)
     */
    public OutboxPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

