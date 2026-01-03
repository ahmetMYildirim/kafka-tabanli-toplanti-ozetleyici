package producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.MeetingMediaEvent;
import model.ProcessedActionItem;
import model.ProcessedSummary;
import model.ProcessedTranscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingMediaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.meeting-media}")
    private String meetingMediaTopic;

    @Value("${kafka.topics.processed-summary}")
    private String processedSummaryTopic;

    @Value("${kafka.topics.processed-transcription}")
    private String processedTranscriptionTopic;

    @Value("${kafka.topics.processed-action-items}")
    private String processedActionItemsTopic;

    public void sendMeetingMediaEvent(MeetingMediaEvent event) {
        String key = event.getPlatform() + "-" + event.getMeetingId();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(meetingMediaTopic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Meeting media event sent: {} [partition={}, offset={}]",
                        event.getEventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send meeting media event: {}", event.getEventId(), ex);
            }
        });
    }

    public void sendProcessedSummary(ProcessedSummary summary) {
        String key = summary.getPlatform() + "-" + summary.getMeetingId();

        kafkaTemplate.send(processedSummaryTopic, key, summary)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Processed summary sent: meetingId={}", summary.getMeetingId());
                    } else {
                        log.error("Failed to send processed summary: meetingId={}", summary.getMeetingId(), ex);
                    }
                });
    }

    public void sendProcessedTranscription(ProcessedTranscription transcription) {
        String key = transcription.getPlatform() + "-" + transcription.getMeetingId();

        kafkaTemplate.send(processedTranscriptionTopic, key, transcription)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Processed transcription sent: meetingId={}", transcription.getMeetingId());
                    } else {
                        log.error("Failed to send processed transcription: meetingId={}", transcription.getMeetingId(), ex);
                    }
                });
    }

    public void sendProcessedActionItems(ProcessedActionItem actionItems) {
        String key = actionItems.getPlatform() + "-" + actionItems.getMeetingId();

        kafkaTemplate.send(processedActionItemsTopic, key, actionItems)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Processed action items sent: meetingId={}", actionItems.getMeetingId());
                    } else {
                        log.error("Failed to send processed action items: meetingId={}", actionItems.getMeetingId(), ex);
                    }
                });
    }
}

