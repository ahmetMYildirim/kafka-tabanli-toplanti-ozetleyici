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
public class DiscordMessageEvent {
    private String messageId;
    private String guildId;
    private String channelId;
    private String channelName;
    private String authorId;
    private String authorName;
    private String content;
    private List<String> attachmentUrls;
    private List<String> mentionedUserIds;
    private Instant timestamp;
    private boolean isEdited;
}
