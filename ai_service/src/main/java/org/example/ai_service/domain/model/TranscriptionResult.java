package org.example.ai_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionResult {
    private String meetingId;
    private String channelId;
    private String platform;
    private String fullTranscription;
    private List<TranscriptionSegment> segments;
    private String language;
    private Double confidence;
    private Long durationSeconds;
    private String processedTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptionSegment{
        private String speakerName;
        private String speakerId;
        private String text;
        private Long startTimeMs;
        private Long endTimeMs;
        private Double confidence;
    }
}
