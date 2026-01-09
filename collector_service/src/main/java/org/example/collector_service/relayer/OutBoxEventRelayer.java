package org.example.collector_service.relayer;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.collector_service.domain.model.OutBoxEvent;
import org.example.collector_service.repository.OutBoxEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class OutBoxEventRelayer {

    private final OutBoxEventRepository outBoxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String RAW_AUDIO_TOPIC = "raw-audio-events";
    private static final String MEETING_TOPIC = "meeting-events";
    private static final String VOICE_SESSION_TOPIC = "voice-session-events";
    private static final String TEXT_MESSAGE_TOPIC = "text-message-events";
    private static final String MEDIA_UPLOADED_TOPIC = "media-uploaded-events";

    public OutBoxEventRelayer(OutBoxEventRepository outBoxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outBoxEventRepository = outBoxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void relayEvents(){
        List<OutBoxEvent> outBoxEvents = outBoxEventRepository.find100Unprocessed(PageRequest.of(0,100));

        for(OutBoxEvent event : outBoxEvents){
            try{
                String topic = determineTopicByAggregateType(event.getAggregateType());

                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());
                event.setProcessed(true);
                outBoxEventRepository.save(event);

                log.debug("Event relayed: type={}, id={}, topic={}",
                    event.getAggregateType(), event.getAggregateId(), topic);

            }catch (Exception e){
                log.error("Could not relay event: {}", event.getId(), e);
            }
        }
    }

    private String determineTopicByAggregateType(String aggregateType) {
        if (aggregateType == null) return RAW_AUDIO_TOPIC;

        return switch (aggregateType) {
            case "AudioMessage" -> RAW_AUDIO_TOPIC;
            case "Meeting" -> MEETING_TOPIC;
            case "VoiceSession" -> VOICE_SESSION_TOPIC;
            case "Message" -> TEXT_MESSAGE_TOPIC;
            case "MeetingMedia" -> MEDIA_UPLOADED_TOPIC;
            default -> {
                log.warn("Unknown aggregate type: {}, using default topic", aggregateType);
                yield RAW_AUDIO_TOPIC;
            }
        };
    }
}
