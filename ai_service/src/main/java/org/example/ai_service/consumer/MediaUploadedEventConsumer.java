package org.example.ai_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.ai_service.domain.model.AudioEvent;
import org.example.ai_service.service.AudioProcessingOrchestrator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class MediaUploadedEventConsumer {

    private final ObjectMapper objectMapper;
    private final AudioProcessingOrchestrator orchestrator;

    @KafkaListener(
            topics = "${kafka.topics.input.media-uploaded:media-uploaded-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMediaUploadedEvent(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Media uploaded event received: key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());

        try {
            String rawValue = record.value();
            JsonNode event;
            
            if (rawValue == null || rawValue.trim().isEmpty()) {
                log.warn("Raw value is null or empty");
                ack.acknowledge();
                return;
            }
            
            try {
                String trimmed = rawValue.trim();
                if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                    String unescaped = objectMapper.readValue(trimmed, String.class);
                    log.info("Unescaped JSON string, length: {}", unescaped.length());
                    event = objectMapper.readTree(unescaped);
                } else {
                    event = objectMapper.readTree(trimmed);
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.error("Failed to parse JSON as string, trying direct parse: {}", e.getMessage());
                try {
                    event = objectMapper.readTree(rawValue);
                } catch (Exception e2) {
                    log.error("Failed to parse JSON completely: {}", e2.getMessage());
                    ack.acknowledge();
                    return;
                }
            } catch (Exception e) {
                log.error("Unexpected error parsing JSON: {}", e.getMessage(), e);
                ack.acknowledge();
                return;
            }

            String audioUrl = null;
            JsonNode storagePathNode = event.get("storagePath");
            JsonNode audioUrlNode = event.get("audioUrl");
            JsonNode audioFilePathNode = event.get("audioFilePath");

            if (storagePathNode != null && !storagePathNode.isNull()) {
                String path = storagePathNode.asText();
                if (path != null && !path.trim().isEmpty()) {
                    audioUrl = path;
                }
            }
            
            if (audioUrl == null && audioUrlNode != null && !audioUrlNode.isNull()) {
                String path = audioUrlNode.asText();
                if (path != null && !path.trim().isEmpty()) {
                    audioUrl = path;
                }
            }
            
            if (audioUrl == null && audioFilePathNode != null && !audioFilePathNode.isNull()) {
                String path = audioFilePathNode.asText();
                if (path != null && !path.trim().isEmpty()) {
                    audioUrl = path;
                }
            }

            if (audioUrl == null || audioUrl.trim().isEmpty()) {
                log.warn("Skipping media uploaded event: audioUrl is null or empty. storagePath={}, audioUrl={}, audioFilePath={}", 
                        storagePathNode != null ? storagePathNode.asText() : "null",
                        audioUrlNode != null ? audioUrlNode.asText() : "null",
                        audioFilePathNode != null ? audioFilePathNode.asText() : "null");
                log.debug("Available fields: {}", event.fieldNames());
                ack.acknowledge();
                return;
            }
            
            log.debug("Parsed audioUrl: {}", audioUrl);

            JsonNode meetingIdNode = event.get("meetingId");
            JsonNode platformNode = event.get("platform");
            JsonNode channelIdNode = event.get("channelId");
            JsonNode hostNameNode = event.get("hostName");
            JsonNode uploadedByNode = event.get("uploadedBy");
            JsonNode eventIdNode = event.get("eventId");
            JsonNode timestampNode = event.get("timestamp");

            String meetingId = (meetingIdNode != null && !meetingIdNode.isNull()) ? meetingIdNode.asText() : null;
            String platform = (platformNode != null && !platformNode.isNull()) ? platformNode.asText() : null;
            String channelId = (channelIdNode != null && !channelIdNode.isNull()) ? channelIdNode.asText() : null;
            String author = null;
            if (hostNameNode != null && !hostNameNode.isNull()) {
                author = hostNameNode.asText();
            } else if (uploadedByNode != null && !uploadedByNode.isNull()) {
                author = uploadedByNode.asText();
            }
            String voiceSessionId = (eventIdNode != null && !eventIdNode.isNull()) ? eventIdNode.asText() : null;

            LocalDateTime timestamp = LocalDateTime.now();
            if (timestampNode != null && !timestampNode.isNull()) {
                try {
                    long timestampMs = timestampNode.asLong();
                    timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampMs), ZoneId.systemDefault());
                } catch (Exception e) {
                    log.warn("Failed to parse timestamp, using current time", e);
                }
            }

            AudioEvent audioEvent = AudioEvent.builder()
                    .meetingId(meetingId)
                    .platform(platform)
                    .channelId(channelId)
                    .author(author)
                    .audioUrl(audioUrl)
                    .voiceSessionId(voiceSessionId)
                    .timestamp(timestamp)
                    .build();

            log.info("MediaUploadedEvent mapped: meetingId={}, platform={}, audioUrl={}",
                    audioEvent.getMeetingId(), audioEvent.getPlatform(), audioEvent.getAudioUrl());

            orchestrator.processAudioEvent(audioEvent);

            ack.acknowledge();
            log.info("Media uploaded event processed: meetingId={}", audioEvent.getMeetingId());

        } catch (Exception e) {
            log.error("Media uploaded event parse error: {}", record.value(), e);
            ack.acknowledge();
        }
    }
}
