package config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic discordVoiceTopic() {
        return TopicBuilder.name("discord-voice")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic discordMessageTopic() {
        return TopicBuilder.name("discord-messages")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic processedMessagesTopic() {
        return TopicBuilder.name("processed-messages")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic processedVoiceTopic() {
        return TopicBuilder.name("processed-voice")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic meetingMediaTopic() {
        return TopicBuilder.name("meeting-media")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic processedSummaryTopic() {
        return TopicBuilder.name("processed-summary")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic processedTranscriptionTopic() {
        return TopicBuilder.name("processed-transcription")
                .partitions(6).replicas(1).build();
    }

    @Bean
    public NewTopic processedActionItemsTopic() {
        return TopicBuilder.name("processed-action-items")
                .partitions(6).replicas(1).build();
    }
}

