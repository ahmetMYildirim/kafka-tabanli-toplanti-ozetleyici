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
public class DiscordVoiceEvent {
    private String eventId;
    private String guildId;
    private String channelId;
    private String userId;
    private String userName;
    private  byte[] audioData;
    private String audioFormat;
    private int sampleRate;
    private Instant timestamp;
    private EventType eventType;

    public enum EventType {
        VOICE_CHUNK,
        JOIN,
        LEAVE,
        MUTE,
        UNMUTE
    }
}
