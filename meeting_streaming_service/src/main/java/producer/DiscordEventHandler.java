package producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.DiscordMessageEvent;
import model.DiscordVoiceEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordEventHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.discord-voice}")
    private String voiceTopic;

    @Value("${kafka.topics.discord-messages}")
    private String messageTopic;

    public void sendVoiceEvent(DiscordVoiceEvent event) {
        String key = event.getGuildId() + "-" + event.getChannelId();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(voiceTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Voice event sent: {} [partition={}, offset={}]",
                        event.getEventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send voice event: {}", event.getEventId(), ex);
            }
        });
    }

    public void sendMessageEvent(DiscordMessageEvent event) {
        String key = event.getGuildId() + "-" + event.getChannelId();

        kafkaTemplate.send(messageTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Message event sent: {}", event.getMessageId());
                    } else {
                        log.error("Failed to send message event: {}", event.getMessageId(), ex);
                    }
                });
    }
}
