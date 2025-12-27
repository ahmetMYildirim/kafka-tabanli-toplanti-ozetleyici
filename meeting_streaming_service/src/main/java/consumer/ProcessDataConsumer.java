package consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.ProcessedMeetingData;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessDataConsumer {

    @KafkaListener(
            topics = "${kafka.topics.processed-messages}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeProcessMessage(
            @Payload ProcessedMeetingData meetingData,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
            ){
        log.info("Received processed message data: sourceType={}, channelId={}, window=[{} - {}]",
                meetingData.getSourceType(),
                meetingData.getChannelId(),
                meetingData.getWindowStart(),
                meetingData.getWindowEnd());

        handleProcessedData(meetingData);
    }

    @KafkaListener(
            topics = "${kafka.topics.processed-voice}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeProcessVoice(
            @Payload ProcessedMeetingData meetingData,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ){
        log.info("Received processed voice data: sourceType={}, speaker={}, audioSize={} bytes",
                meetingData.getSourceType(),
                meetingData.getSpeakerName(),
                meetingData.getAudioData() != null ? meetingData.getAudioData().length : 0);

        handleProcessedData(meetingData);
    }

    private void handleProcessedData(ProcessedMeetingData data) {
        switch (data.getSourceType()) {
            case "DISCORD_MESSAGES" -> saveMessageSummary(data);
            case "DISCORD_VOICE" -> saveVoiceTranscript(data);
            case "ZOOM" -> saveZoomData(data);
            default -> log.warn("Unknown source type: {}", data.getSourceType());
        }
    }

    private void saveMessageSummary(ProcessedMeetingData meetingData){
        log.info("Saving message summary for channel: {}", meetingData.getChannelId());
        log.debug("Raw content length: {} chars",
                meetingData.getRawContent() != null ? meetingData.getRawContent().length() : 0);
    }

    private void saveVoiceTranscript(ProcessedMeetingData meetingData) {
        log.info("Saving voice transcript for speaker: {}", meetingData.getSpeakerName());
    }

    private void saveZoomData(ProcessedMeetingData meetingData) {
        log.info("Saving Zoom meeting data");
    }
}
