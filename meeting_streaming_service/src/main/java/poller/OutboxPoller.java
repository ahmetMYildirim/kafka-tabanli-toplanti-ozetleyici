package poller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.discord-messages}")
    private String discordMessagesTopic;

    @Value("${kafka.topics.discord-voice}")
    private String discordVoiceTopic;

    @Value("${kafka.topics.zoom-meetings}")
    private String zoomMeetingsTopic;

    @PostConstruct
    public void init() {
        log.info("Outbox Poller initialized - Reading from outbox table");
    }

    @Scheduled(fixedRate = 5000)
    public void pollOutbox() {
            try{
                String sql = """
                SELECT * FROM outbox 
                WHERE is_processed = 0 
                ORDER BY created_at ASC 
                LIMIT 100
                """;

                List<Map<String, Object>> events = jdbcTemplate.queryForList(sql);

                if (events.isEmpty()) {
                    log.debug("No new events in outbox");
                    return;
                }

                log.info("Found {} unprocessed events in outbox", events.size());

                for (Map<String, Object> event : events) {
                    Long eventId = ((Number) event.get("id")).longValue();
                    String aggregateType = (String) event.get("aggregate_type");
                    String eventType = (String) event.get("event_type");
                    String payload = (String) event.get("payload");

                    String topic = determineTopicByAggregateType(aggregateType);
                    String key = generateKey(payload, aggregateType);

                    try {
                        kafkaTemplate.send(topic, key, payload)
                                .whenComplete((result, ex) -> {
                                    if (ex == null) {
                                        markAsProcessed(eventId);
                                        log.debug("Event {} sent to topic {}", eventId, topic);
                                    } else {
                                        log.error("Failed to send event {}: {}", eventId, ex.getMessage());
                                    }
                                });
                    } catch (Exception e) {
                        log.error("Error sending event {}: {}", eventId, e.getMessage());
                    }
                }

                log.info("Processed {} events from outbox", events.size());

            }catch(Exception e){
                log.error("Error polling outbox: {}", e.getMessage(), e);
            }
    }

    private String determineTopicByAggregateType(String aggregateType) {
        if (aggregateType == null) {
            return discordMessagesTopic;
        }

        return switch (aggregateType) {
            case "Message" -> discordMessagesTopic;
            case "AudioMessage", "AudioMess" -> discordVoiceTopic;
            case "Meeting" -> zoomMeetingsTopic;
            default -> {
                log.warn("Unknown aggregate type: {}, using default topic", aggregateType);
                yield discordMessagesTopic;
            }
        };
    }

    private String generateKey(String payload, String aggregateType) {
        try {
            if (payload != null && payload.contains("channelId")) {
                int start = payload.indexOf("\"channelId\":\"") + 13;
                int end = payload.indexOf("\"", start);
                if (start > 12 && end > start) {
                    return payload.substring(start, end);
                }
            }
            return aggregateType + "-" + System.currentTimeMillis();
        } catch (Exception e) {
            return "default-key";
        }
    }

    private void markAsProcessed(Long eventId) {
        try {
            String sql = "UPDATE outbox SET is_processed = 1 WHERE id = ?";
            int updated = jdbcTemplate.update(sql, eventId);
            if (updated > 0) {
                log.debug("Marked event {} as processed", eventId);
            }
        } catch (Exception e) {
            log.error("Failed to mark event {} as processed: {}", eventId, e.getMessage());
        }
    }

}
