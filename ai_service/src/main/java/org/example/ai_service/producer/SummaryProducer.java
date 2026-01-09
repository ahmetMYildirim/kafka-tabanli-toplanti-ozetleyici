package org.example.ai_service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai_service.domain.model.MeetingSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.output.summary}")
    private String summaryTopic;

    public void send(MeetingSummary summary) {
        log.info("Summary is being sent: meetingId={}, topic={}",
                summary.getMeetingId(), summaryTopic);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(summaryTopic, summary.getMeetingId(), summary);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Summary sent: meetingId={}, offset={}",
                        summary.getMeetingId(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Abstract could not be sent: meetingId={}",
                        summary.getMeetingId(), ex);
            }
        });
    }
}
