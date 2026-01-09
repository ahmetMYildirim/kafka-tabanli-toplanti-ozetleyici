import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MeetingStreamingServiceApplication - Kafka Streams tabanlı toplantı verisi işleme servisi
 * Bu servis, Kafka ekosisteminde merkezi hub rolü oynar.
 * Collector servisinden gelen raw event'leri işler, aggregate eder ve
 * downstream servislere (AI, Gateway) yönlendirir.
 * @author Ahmet
 * @version 1.0
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"config", "producer", "poller", "consumer"})
public class MeetingStreamingServiceApplication {

    /**
     * Uygulama giriş noktası.
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        SpringApplication.run(MeetingStreamingServiceApplication.class, args);
    }
}
