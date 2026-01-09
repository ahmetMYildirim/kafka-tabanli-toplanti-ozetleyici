package org.example.ai_service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai_service.domain.model.ExtractedTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActionItemProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.output.action-items}")
    private String actionItemsTopic;

    public void send(ExtractedTask extractedTask) {
        log.info("Tasks are being sent: meetingId={}, taskCount={}, topic={}",
                extractedTask.getMeetingId(),
                extractedTask.getTaskItems().size(),
                actionItemsTopic);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(actionItemsTopic, extractedTask.getMeetingId(), extractedTask);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Tasks have been sent: meetingId={}, offset={}",
                        extractedTask.getMeetingId(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Transcript could not be sent: meetingId={}",
                        extractedTask.getMeetingId(), ex);
            }
        });
    }
}
