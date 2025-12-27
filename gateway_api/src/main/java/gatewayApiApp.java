import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * gatewayApiApp - API Gateway ana uygulama sınıfı
 * 
 * Bu sınıf, Spring Boot uygulamasının giriş noktasıdır.
 * Tüm mikroservisler için merkezi API Gateway olarak çalışır.
 * 
 * Sağlanan Özellikler:
 * - REST API endpoint'leri
 * - WebSocket gerçek zamanlı bildirimler
 * - JWT kimlik doğrulama
 * - Rate limiting
 * - Kafka event tüketimi
 * 
 * Bağlı Servisler:
 * - collector_service: Discord/Zoom veri toplama
 * - AI Service: Toplantı özeti ve transkript oluşturma
 * - Frontend UI: React/Vue dashboard
 * 
 * @author Ahmet
 * @version 1.0
 */
@EnableScheduling
@SpringBootApplication
public class gatewayApiApp {
    
    /**
     * Uygulama giriş noktası.
     * 
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        SpringApplication.run(gatewayApiApp.class, args);
    }
}
