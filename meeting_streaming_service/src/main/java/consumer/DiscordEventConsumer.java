package consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.DiscordMessageEvent;
import model.DiscordVoiceEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordEventConsumer {

    @KafkaListener(
            topics = "${kafka.topics.discord-voice}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeVoiceEvent(
            @Payload DiscordVoiceEvent discordEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key
    ){
        log.info("Received voice event: eventId={}, user={}, channel={}, partition={}, offset={}",
                discordEvent.getEventId(),
                discordEvent.getUserId(),
                discordEvent.getChannelId(),
                partition,
                offset);
        processVoiceEvent(discordEvent);
    }

    @KafkaListener(
            topics = "${kafka.topics.discord-messages}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMessageEvent(
            @Payload DiscordMessageEvent discordEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ){
        log.info("Received message event: messageId={}, author={}, channel={}, partition={}, offset={}",
                discordEvent.getMessageId(),
                discordEvent.getAuthorName(),
                discordEvent.getChannelId(),
                partition,
                offset);

        processMessageEvent(discordEvent);
    }

    @KafkaListener(
            topics = "${kafka.topics.discord-voice}",
            groupId = "voice-batch-group",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatchVoiceEvents(@Payload List<DiscordVoiceEvent> events) {
        log.info("Received batch of {} voice events", events.size());
        events.forEach(this::processVoiceEvent);
    }

    private void processVoiceEvent(DiscordVoiceEvent event){
        switch(event.getEventType()){
            case VOICE_CHUNK -> handleVoiceChunk(event);
            case JOIN -> handleUserJoin(event);
            case LEAVE -> handleUserLeave(event);
            case MUTE -> handleUserMute(event);
            case UNMUTE -> handleUserUnmute(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void processMessageEvent(DiscordMessageEvent event){
        log.debug("Processing message from {}: {}",
                event.getAuthorName(),
                truncateContent(event.getContent(), 50));
    }

    private void handleVoiceChunk(DiscordVoiceEvent event){
        log.debug("Voice chunk received from user: {}, size:{} bytes",
                event.getUserId(),
                event.getAudioData() != null ? event.getAudioData().length : 0);
    }

    private void handleUserJoin(DiscordVoiceEvent event){
        log.info("User {} joined channel {}", event.getUserId(), event.getChannelId());
    }

    private void handleUserLeave(DiscordVoiceEvent event){
        log.info("User {} left channel {}", event.getUserId(), event.getChannelId());
    }

    private void handleUserMute(DiscordVoiceEvent event){
        log.info("User {} muted in channel {}", event.getUserId(), event.getChannelId());
    }

    private void handleUserUnmute(DiscordVoiceEvent event){
        log.info("User {} unmuted in channel {}", event.getUserId(), event.getChannelId());
    }

    private String truncateContent(String content, int maxLength){
        if(content == null) return "";
        return content.length() > maxLength ? content.substring(0, maxLength) + ".." : content;
    }
}
