package org.example.collector_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MainApp - Collector Service ana uygulama sınıfı
 * 
 * Bu servis, Discord ve Zoom platformlarından toplantı verilerini toplar
 * ve Outbox pattern kullanarak güvenilir şekilde Kafka'ya iletir.
 * 
 * Ana Sorumluluklar:
 * - Discord bot entegrasyonu (mesaj ve ses toplama)
 * - Zoom webhook ve API entegrasyonu
 * - Veritabanına kayıt (MySQL)
 * - Outbox pattern ile event sourcing
 * 
 * Mimari: Mikroservis mimarisinde veri toplama katmanı
 * 
 * @author Ahmet
 * @version 1.0
 */
@SpringBootApplication(scanBasePackages = "org.example.collector_service")
@EnableScheduling
@EnableJpaRepositories(basePackages = "org.example.collector_service.repository")
@EntityScan(basePackages = "org.example.collector_service.domain.model")
public class MainApp {

    /**
     * Uygulama başlangıç noktası.
     * Spring Boot container'ını başlatır ve tüm servisleri aktif eder.
     *
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        SpringApplication.run(MainApp.class, args);
    }

    /**
     * Jackson ObjectMapper bean yapılandırması.
     * Java 8 Date/Time API desteği ve ISO-8601 tarih formatı ile yapılandırılır.
     *
     * @return Yapılandırılmış ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
