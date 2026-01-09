package com.toplanti.dashboard.model;

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
public class User {
    private String id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Role role;
    private String department;
    private String managerId;
    private List<String> subordinateIds;
    private Instant createdAt;
    private Instant lastLoginAt;
    private boolean active;

    public enum Role {
        CEO("CEO", 1),
        DIRECTOR("Direktör", 2),
        MANAGER("Yönetici", 3),
        TEAM_LEAD("Takım Lideri", 4),
        SENIOR("Kıdemli Çalışan", 5),
        EMPLOYEE("Çalışan", 6),
        INTERN("Stajyer", 7);

        private final String displayName;
        private final int hierarchyLevel;

        Role(String displayName, int hierarchyLevel) {
            this.displayName = displayName;
            this.hierarchyLevel = hierarchyLevel;
        }

        public String getDisplayName() { return displayName; }
        public int getHierarchyLevel() { return hierarchyLevel; }

        public boolean canAssignTaskTo(Role targetRole) {
            return this.hierarchyLevel < targetRole.hierarchyLevel;
        }

        public boolean canViewTasksOf(Role targetRole) {
            return this.hierarchyLevel <= targetRole.hierarchyLevel;
        }
    }
}

