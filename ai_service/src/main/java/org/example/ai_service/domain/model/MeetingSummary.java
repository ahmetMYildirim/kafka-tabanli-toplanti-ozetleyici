package org.example.ai_service.domain.model;

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
public class MeetingSummary {
    private String meetingId;
    private String channelId;
    private String platform;
    private String title;
    private String summary;
    private List<String> keyPoints;
    private List<String> decisions;
    private List<String> participants;
    private Long durationMinutes;
    private Instant meetingDate;
    private Instant processedTime;
}
