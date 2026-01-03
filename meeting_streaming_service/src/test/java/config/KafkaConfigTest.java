package config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaConfig Unit Tests")
class KafkaConfigTest {

    private KafkaConfig kafkaConfig;

    @BeforeEach
    void setUp() {
        kafkaConfig = new KafkaConfig();
        ReflectionTestUtils.setField(kafkaConfig, "bootstrapServers", "localhost:9092");
    }

    @Test
    @DisplayName("Should create producer factory")
    void shouldCreateProducerFactory() {
        ProducerFactory<String, Object> producerFactory = kafkaConfig.producerFactory();

        assertNotNull(producerFactory);
    }

    @Test
    @DisplayName("Should create kafka template")
    void shouldCreateKafkaTemplate() {
        KafkaTemplate<String, Object> kafkaTemplate = kafkaConfig.kafkaTemplate();

        assertNotNull(kafkaTemplate);
    }

    @Test
    @DisplayName("Should create discord voice topic")
    void shouldCreateDiscordVoiceTopic() {
        NewTopic topic = kafkaConfig.discordVoiceTopic();

        assertNotNull(topic);
        assertEquals("discord-voice", topic.name());
        assertEquals(6, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
    }

    @Test
    @DisplayName("Should create discord message topic")
    void shouldCreateDiscordMessageTopic() {
        NewTopic topic = kafkaConfig.discordMessageTopic();

        assertNotNull(topic);
        assertEquals("discord-messages", topic.name());
        assertEquals(6, topic.numPartitions());
        assertEquals(1, topic.replicationFactor());
    }

    @Test
    @DisplayName("Should create processed messages topic")
    void shouldCreateProcessedMessagesTopic() {
        NewTopic topic = kafkaConfig.processedMessagesTopic();

        assertNotNull(topic);
        assertEquals("processed-messages", topic.name());
        assertEquals(6, topic.numPartitions());
    }

    @Test
    @DisplayName("Should create processed voice topic")
    void shouldCreateProcessedVoiceTopic() {
        NewTopic topic = kafkaConfig.processedVoiceTopic();

        assertNotNull(topic);
        assertEquals("processed-voice", topic.name());
        assertEquals(6, topic.numPartitions());
    }

    @Test
    @DisplayName("Should create meeting media topic")
    void shouldCreateMeetingMediaTopic() {
        NewTopic topic = kafkaConfig.meetingMediaTopic();

        assertNotNull(topic);
        assertEquals("meeting-media", topic.name());
        assertEquals(6, topic.numPartitions());
    }

    @Test
    @DisplayName("Should create processed summary topic")
    void shouldCreateProcessedSummaryTopic() {
        NewTopic topic = kafkaConfig.processedSummaryTopic();

        assertNotNull(topic);
        assertEquals("processed-summary", topic.name());
        assertEquals(6, topic.numPartitions());
    }

    @Test
    @DisplayName("Should create processed transcription topic")
    void shouldCreateProcessedTranscriptionTopic() {
        NewTopic topic = kafkaConfig.processedTranscriptionTopic();

        assertNotNull(topic);
        assertEquals("processed-transcription", topic.name());
        assertEquals(6, topic.numPartitions());
    }

    @Test
    @DisplayName("Should create processed action items topic")
    void shouldCreateProcessedActionItemsTopic() {
        NewTopic topic = kafkaConfig.processedActionItemsTopic();

        assertNotNull(topic);
        assertEquals("processed-action-items", topic.name());
        assertEquals(6, topic.numPartitions());
    }
}

