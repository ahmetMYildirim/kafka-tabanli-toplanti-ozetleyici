package consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.MeetingMediaEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingMediaConsumer {

    @KafkaListener(
            topics = "${kafka.topics.meeting-media}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMeetingMediaEvent(
            @Payload MeetingMediaEvent mediaEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.info("Received meeting media event: meetingId={}, platform={}, eventType={}, partition={}, offset={}",
                mediaEvent.getMeetingId(),
                mediaEvent.getPlatform(),
                mediaEvent.getEventType(),
                partition,
                offset
        );

        processMeetingMediaEvent(mediaEvent);
    }

    private void processMeetingMediaEvent(MeetingMediaEvent event) {
        if (event.getEventType() == null) {
            log.warn("Event type is null for meeting: {}", event.getMeetingId());
            return;
        }

        switch (event.getEventType()) {
            case MEDIA_UPLOADED -> handleMediaUploaded(event);
            case MEDIA_PROCESSING -> handleMediaProcessing(event);
            case MEDIA_PROCESSED -> handleMediaProcessed(event);
            case MEDIA_FAILED -> handleMediaFailed(event);
            case TRANSCRIPTION_STARTED -> handleTranscriptionStarted(event);
            case TRANSCRIPTION_COMPLETED -> handleTranscriptionCompleted(event);
            case SUMMARY_GENERATED -> handleSummaryGenerated(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void handleMediaUploaded(MeetingMediaEvent event) {
        log.info("Media uploaded: platform={}, meetingId={}, fileKey={}, fileSize={} bytes",
                event.getPlatform(),
                event.getMeetingId(),
                event.getFileKey(),
                event.getFileSize()
        );
    }

    private void handleMediaProcessing(MeetingMediaEvent event) {
        log.info("Media processing started: meetingId={}, fileKey={}",
                event.getMeetingId(),
                event.getFileKey()
        );
    }

    private void handleMediaProcessed(MeetingMediaEvent event) {
        log.info("Media processed successfully: meetingId={}, platform={}",
                event.getMeetingId(),
                event.getPlatform()
        );
    }

    private void handleMediaFailed(MeetingMediaEvent event) {
        log.error("Media processing failed: meetingId={}, fileKey={}",
                event.getMeetingId(),
                event.getFileKey()
        );
    }

    private void handleTranscriptionStarted(MeetingMediaEvent event) {
        log.info("Transcription started: meetingId={}, platform={}",
                event.getMeetingId(),
                event.getPlatform()
        );
    }

    private void handleTranscriptionCompleted(MeetingMediaEvent event) {
        log.info("Transcription completed: meetingId={}, platform={}",
                event.getMeetingId(),
                event.getPlatform()
        );
    }

    private void handleSummaryGenerated(MeetingMediaEvent event) {
        log.info("Summary generated: meetingId={}, title={}",
                event.getMeetingId(),
                event.getMeetingTitle()
        );
    }
}

