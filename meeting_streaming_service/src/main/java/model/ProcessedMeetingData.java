package model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedMeetingData {
    private String sourceType;
    private String channelId;
    private String rawContent;
    private byte[] audioData;
    private String speakerName;
    private String speakerId;
    private Instant windowStart;
    private Instant windowEnd;
    private Instant processedAt;
}