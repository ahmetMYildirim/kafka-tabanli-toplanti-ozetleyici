package consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.ZoomMeetingEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class ZoomEventConsumer {

    @KafkaListener(
            topics = "${kafka.topics.zoom-meetings}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeZoomEvent(
            @Payload ZoomMeetingEvent zoomEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key
            ){
        log.info("Received Zoom event: meetingId={}, type={}, participant={}, partition={}, offset={}",
                zoomEvent.getMeetingId(),
                zoomEvent.getEventType(),
                zoomEvent.getParticipantName(),
                partition,
                offset
        );

        processZoomEvent(zoomEvent);
    }

    private void processZoomEvent(ZoomMeetingEvent event){
        switch (event.getEventType()){
            case MEETING_STARTED -> handleMeetingStarted(event);
            case MEETING_ENDED -> handleMeetingEnded(event);
            case PARTICIPANT_JOINED -> handleParticipantJoined(event);
            case PARTICIPANT_LEFT -> handleParticipantLeft(event);
            case TRANSCRIPTION_CHUNK ->  handleTranscriptionChunk(event);
            case AUIDO_CHUNK -> handleAudioChunk(event);
            case CHAT_MESSAGE -> handleChatMessage(event);

            default -> log.warn("Unknown Zoom event type: {}", event.getEventType());
        }
    }

    private void handleMeetingStarted(ZoomMeetingEvent event){
        log.info("Meeting started: topic={}, meetingId={}",
            event.getMeetingTopic(),
            event.getMeetingId()
        );
    }

    private void handleMeetingEnded(ZoomMeetingEvent event){
        log.info("Meeting ended: topic={}, meetingId={}",
                event.getMeetingTopic(),
                event.getMeetingId()
        );
    }

    private void handleParticipantJoined(ZoomMeetingEvent event){
        log.info("Participant joined: participant={}, meetingId={}",
                event.getParticipantName(),
                event.getMeetingId()
        );
    }

    private void handleParticipantLeft(ZoomMeetingEvent event){
        log.info("Participant left: participant={}, meetingId={}",
                event.getParticipantName(),
                event.getMeetingId()
        );
    }

    private void handleTranscriptionChunk(ZoomMeetingEvent event){
        log.info("Transcription chunk received: {}",
                truncate(event.getTranscriptionChunk(), 100)
        );
    }

    private void handleAudioChunk(ZoomMeetingEvent event){
        log.info("Audio chunk received: size={} bytes",
                event.getAudioData() != null ? event.getAudioData().length : 0
        );
    }

    private void handleChatMessage(ZoomMeetingEvent event){
        log.info("Chat message received from {}",
                event.getParticipantName()
        );
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
