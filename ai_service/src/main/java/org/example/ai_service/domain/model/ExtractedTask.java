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
public class ExtractedTask {
    private String meetingId;
    private String channelId;
    private String platform;
    private List<TaskItem> taskItems;
    private Instant processedTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskItem {
        private String id;
        private String title;
        private String description;
        private String assignee;
        private String assigneeId;
        private Priority priority;
        private Status status;
        private Instant dueDate;
        private String sourceText;
        private Double confidenceScore;
        private String assignmentReason;
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
}
