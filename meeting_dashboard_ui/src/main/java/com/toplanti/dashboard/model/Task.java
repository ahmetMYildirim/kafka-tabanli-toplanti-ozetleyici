package com.toplanti.dashboard.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String id;
    private String meetingId;
    private String title;
    private String description;
    private String assignerId;
    private String assignerName;
    private String assigneeId;
    private String assigneeName;
    private Priority priority;
    private Status status;
    private String dueDate;
    private String context;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public enum Priority {
        CRITICAL("Kritik", "#dc2626", 1),
        HIGH("Yüksek", "#ea580c", 2),
        MEDIUM("Orta", "#ca8a04", 3),
        LOW("Düşük", "#16a34a", 4);

        private final String displayName;
        private final String color;
        private final int level;

        Priority(String displayName, String color, int level) {
            this.displayName = displayName;
            this.color = color;
            this.level = level;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
        public int getLevel() { return level; }
    }

    public enum Status {
        PENDING("Beklemede"),
        IN_PROGRESS("Devam Ediyor"),
        REVIEW("İncelemede"),
        COMPLETED("Tamamlandı"),
        CANCELLED("İptal Edildi");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }
}

