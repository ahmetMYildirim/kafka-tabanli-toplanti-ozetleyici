package org.example.ai_service.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ai_service.domain.model.TranscriptionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.output.transcription}")
    private String transcriptionTopic;

    public void send(TranscriptionResult transcription) {
        log.info("Transcript is being sent: meetingId={}, topic={}",
                transcription.getMeetingId(), transcriptionTopic);

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(transcriptionTopic, transcription.getMeetingId(), transcription);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transcript sent: meetingId={}, offset={}",
                        transcription.getMeetingId(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Transcript could not be sent: meetingId={}",
                        transcription.getMeetingId(), ex);
            }
        });
    }
}
