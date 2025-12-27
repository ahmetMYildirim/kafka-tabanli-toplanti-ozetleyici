package config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaConfig - Apache Kafka tüketici yapılandırması
 * 
 * Bu sınıf, AI servisinden gelen işlenmiş toplantı verilerini
 * dinlemek için Kafka consumer ayarlarını yapılandırır.
 * 
 * Dinlenen Topic'ler:
 * - processed-summaries: AI toplantı özetleri
 * - processed-transcripts: Ses-metin dönüşüm sonuçları
 * - processed-action-items: Toplantıdan çıkarılan görevler
 * 
 * Mimari Rol: Event-Driven Architecture içinde consumer görevini üstlenir
 * 
 * @author Ahmet
 * @version 1.0
 */
@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    /**
     * Kafka mesaj tüketici fabrikasını oluşturur.
     * JSON formatındaki mesajları otomatik deserialize eder.
     * 
     * @return Yapılandırılmış ConsumerFactory nesnesi
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(Object.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        log.info("Kafka consumer factory olusturuldu. Sunucu: {}", bootstrapServers);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /**
     * Kafka dinleyici konteyner fabrikasını oluşturur.
     * @KafkaListener annotasyonu ile işaretlenen metodlar bu fabrikayı kullanır.
     * 
     * @return Yapılandırılmış listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        log.info("Kafka listener factory basariyla yapilandirildi");
        
        return factory;
    }
}
