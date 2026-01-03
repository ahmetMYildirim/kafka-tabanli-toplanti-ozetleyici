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
public class ProcessedSummary {
    private String meetingId;
    private String channelId;
    private String platform;
    private String title;
    private String summary;
    private List<String> keyPoints;
    private List<String> participants;
    private Instant meetingStartTime;
    private Instant meetingEndTime;
    private Instant processedTime;
}

