package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedTranscription {
    private String meetingId;
    private String channelId;
    private String platform;
    private String fullTranscription;
    private List<TranscriptionSegment> segments;
    private String language;
    private Double confidence;
    private Long durationSeconds;
    private Instant processedTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptionSegment {
        private String speakerName;
        private String speakerId;
        private String text;
        private Long startTimeMs;
        private Long endTimeMs;
        private Double confidence;
    }
}

