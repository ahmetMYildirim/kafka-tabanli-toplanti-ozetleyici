package producer;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.DiscordMessageEvent;
import model.ProcessedMeetingData;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Service;

import java.time.Duration;


/**
 * MeetingStreamProcessor - Kafka Streams ile toplantı mesajlarını işleyen servis
 * @author Ahmet
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingStreamProcessor {

    @Value("${kafka.topics.discord-voice}")
    private String discordVoiceTopic;

    @Value("${kafka.topics.discord-messages}")
    private String discordMessageTopic;

    @Value("${kafka.topics.processed-messages}")
    private String processedMessageTopic;

    @Value("${kafka.topics.processed-voice}")
    private String processedVoiceTopic;

    /**
     * Discord mesajları için Kafka Streams topology'sini oluşturur.
     * @param builder Kafka Streams builder (Spring tarafından inject edilir)
     * @return KStream<String, DiscordMessageEvent> (downstream işlemler için)
     */
    @Bean
    public KStream<String, DiscordMessageEvent> discordMessageStream(StreamsBuilder builder) {
        JsonSerde<DiscordMessageEvent> messageSerde = new JsonSerde<>(DiscordMessageEvent.class);
        JsonSerde<ProcessedMeetingData> processedSerde = new JsonSerde<>(ProcessedMeetingData.class);

        KStream<String, DiscordMessageEvent> messageEventKStream = builder.stream(
                discordMessageTopic,
                Consumed.with(Serdes.String(), messageSerde)
        );

        messageEventKStream
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5))) // 5 dakikalık pencere
                .aggregate(
                        () -> "",
                        (key, message, aggregate) -> {
                            return aggregate + message.getAuthorName()
                                    + ": "
                                    + message.getContent()
                                    + "\n";
                        },
                        Materialized.with(Serdes.String(), Serdes.String())
                )
                .toStream()
                .map((windowedKey, aggregatedMessages) -> {
                    log.info("Processing windowed messages: channel={}, windowStart={}, windowEnd={}", 
                            windowedKey.key(), 
                            windowedKey.window().startTime(), 
                            windowedKey.window().endTime());

                    ProcessedMeetingData processed = ProcessedMeetingData.builder()
                            .sourceType("DISCORD_MESSAGES")
                            .channelId(windowedKey.key())
                            .rawContent(aggregatedMessages)
                            .windowStart(windowedKey.window().startTime())
                            .windowEnd(windowedKey.window().endTime())
                            .build();

                    return KeyValue.pair(windowedKey.key(), processed);
                })
                .to(processedMessageTopic, Produced.with(Serdes.String(), processedSerde));

        return messageEventKStream;
    }
}
