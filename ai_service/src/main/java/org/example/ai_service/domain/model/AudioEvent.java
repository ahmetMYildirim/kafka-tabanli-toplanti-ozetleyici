package org.example.ai_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioEvent {
    private Long id;
    private String platform;
    private String channelId;
    private String author;
    private String audioUrl;
    private String transcription;
    private LocalDateTime timestamp;
    private String voiceSessionId;
    private String meetingId;
}
