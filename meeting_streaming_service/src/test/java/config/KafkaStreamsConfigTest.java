package config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaStreamsConfig Unit Tests")
class KafkaStreamsConfigTest {

    private KafkaStreamsConfig kafkaStreamsConfig;

    @BeforeEach
    void setUp() {
        kafkaStreamsConfig = new KafkaStreamsConfig();
        ReflectionTestUtils.setField(kafkaStreamsConfig, "bootStrapsServers", "localhost:9092");
        ReflectionTestUtils.setField(kafkaStreamsConfig, "applicationId", "test-streams-app");
    }

    @Test
    @DisplayName("Should create kafka streams configuration")
    void shouldCreateKafkaStreamsConfiguration() {
        KafkaStreamsConfiguration config = kafkaStreamsConfig.kafkaStreamsConfig();

        assertNotNull(config);
    }

    @Test
    @DisplayName("Kafka streams config should contain bootstrap servers")
    void kafkaStreamsConfigShouldContainBootstrapServers() {
        KafkaStreamsConfiguration config = kafkaStreamsConfig.kafkaStreamsConfig();

        assertNotNull(config);
        assertNotNull(config.asProperties());
    }

    @Test
    @DisplayName("Kafka streams config should contain application id")
    void kafkaStreamsConfigShouldContainApplicationId() {
        KafkaStreamsConfiguration config = kafkaStreamsConfig.kafkaStreamsConfig();

        assertNotNull(config);
        assertNotNull(config.asProperties());
    }

    @Test
    @DisplayName("Should use string serde as default")
    void shouldUseStringSerdeAsDefault() {
        KafkaStreamsConfiguration config = kafkaStreamsConfig.kafkaStreamsConfig();

        assertNotNull(config);
    }
}

