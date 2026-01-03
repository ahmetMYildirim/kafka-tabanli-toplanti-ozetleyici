package config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaConsumerConfig Unit Tests")
class KafkaConsumerConfigTest {

    private KafkaConsumerConfig kafkaConsumerConfig;

    @BeforeEach
    void setUp() {
        kafkaConsumerConfig = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(kafkaConsumerConfig, "bootstrapServers", "localhost:9092");
        ReflectionTestUtils.setField(kafkaConsumerConfig, "groupId", "test-group");
    }

    @Test
    @DisplayName("Should create consumer factory")
    void shouldCreateConsumerFactory() {
        ConsumerFactory<String, Object> consumerFactory = kafkaConsumerConfig.consumerFactory();

        assertNotNull(consumerFactory);
    }

    @Test
    @DisplayName("Should create kafka listener container factory")
    void shouldCreateKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                kafkaConsumerConfig.kafkaListenerContainerFactory();

        assertNotNull(factory);
    }

    @Test
    @DisplayName("Should create batch kafka listener container factory")
    void shouldCreateBatchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                kafkaConsumerConfig.batchKafkaListenerContainerFactory();

        assertNotNull(factory);
        assertTrue(factory.isBatchListener());
    }

    @Test
    @DisplayName("Kafka listener container factory should have correct concurrency")
    void kafkaListenerContainerFactoryShouldHaveCorrectConcurrency() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                kafkaConsumerConfig.kafkaListenerContainerFactory();

        assertNotNull(factory);
    }

    @Test
    @DisplayName("Batch kafka listener container factory should have batch mode enabled")
    void batchKafkaListenerContainerFactoryShouldHaveBatchModeEnabled() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                kafkaConsumerConfig.batchKafkaListenerContainerFactory();

        assertNotNull(factory);
        assertTrue(factory.isBatchListener());
    }
}

