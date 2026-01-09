package org.example.ai_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${kafka.topics.input.meeting}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMeetingEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Meeting event received: key={}", record.key());

        try {
            JsonNode meetingNode = objectMapper.readTree(record.value());
            String meetingId = meetingNode.get("id").asText();
            String status = meetingNode.has("status") ? meetingNode.get("status").asText() : "UNKNOWN";

            log.info("meeting status: meetingId={}, status={}", meetingId, status);

            if ("ENDED".equals(status) || "COMPLETED".equals(status)) {
                log.info("The meeting has ended, final transactions can be made.: {}", meetingId);
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("The meeting event could not be processed.: {}", record.value(), e);
            ack.acknowledge();
        }
    }
}
