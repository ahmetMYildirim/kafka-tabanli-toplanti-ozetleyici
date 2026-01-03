package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessedActionItem Unit Tests")
class ProcessedActionItemTest {

    @Test
    @DisplayName("Should create action item using builder pattern")
    void shouldCreateActionItemUsingBuilder() {
        Instant now = Instant.now();

        ProcessedActionItem.ActionItem item1 = ProcessedActionItem.ActionItem.builder()
                .id("item-1")
                .title("Complete report")
                .description("Finish the quarterly report")
                .assignee("John Doe")
                .assigneeId("user-123")
                .priority(ProcessedActionItem.Priority.HIGH)
                .status(ProcessedActionItem.Status.PENDING)
                .dueDate(now.plusSeconds(86400))
                .sourceText("John mentioned completing the report by tomorrow")
                .build();

        ProcessedActionItem.ActionItem item2 = ProcessedActionItem.ActionItem.builder()
                .id("item-2")
                .title("Schedule follow-up")
                .assignee("Jane Smith")
                .priority(ProcessedActionItem.Priority.MEDIUM)
                .status(ProcessedActionItem.Status.PENDING)
                .build();

        List<ProcessedActionItem.ActionItem> items = Arrays.asList(item1, item2);

        ProcessedActionItem actionItem = ProcessedActionItem.builder()
                .meetingId("meeting-123")
                .channelId("channel-456")
                .platform("TEAMS")
                .actionItems(items)
                .processedTime(now)
                .build();

        assertEquals("meeting-123", actionItem.getMeetingId());
        assertEquals("TEAMS", actionItem.getPlatform());
        assertEquals(2, actionItem.getActionItems().size());
    }

    @Test
    @DisplayName("Should have all priority levels defined")
    void shouldHaveAllPriorityLevelsDefined() {
        ProcessedActionItem.Priority[] priorities = ProcessedActionItem.Priority.values();

        assertEquals(4, priorities.length);
        assertNotNull(ProcessedActionItem.Priority.valueOf("LOW"));
        assertNotNull(ProcessedActionItem.Priority.valueOf("MEDIUM"));
        assertNotNull(ProcessedActionItem.Priority.valueOf("HIGH"));
        assertNotNull(ProcessedActionItem.Priority.valueOf("URGENT"));
    }

    @Test
    @DisplayName("Should have all status types defined")
    void shouldHaveAllStatusTypesDefined() {
        ProcessedActionItem.Status[] statuses = ProcessedActionItem.Status.values();

        assertEquals(4, statuses.length);
        assertNotNull(ProcessedActionItem.Status.valueOf("PENDING"));
        assertNotNull(ProcessedActionItem.Status.valueOf("IN_PROGRESS"));
        assertNotNull(ProcessedActionItem.Status.valueOf("COMPLETED"));
        assertNotNull(ProcessedActionItem.Status.valueOf("CANCELLED"));
    }

    @Test
    @DisplayName("Should create action item using no-args constructor")
    void shouldCreateActionItemUsingNoArgsConstructor() {
        ProcessedActionItem actionItem = new ProcessedActionItem();

        assertNull(actionItem.getMeetingId());
        assertNull(actionItem.getActionItems());
    }

    @Test
    @DisplayName("Should create inner ActionItem correctly")
    void shouldCreateInnerActionItemCorrectly() {
        ProcessedActionItem.ActionItem item = ProcessedActionItem.ActionItem.builder()
                .id("task-123")
                .title("Review code")
                .description("Review PR #45")
                .assignee("Developer")
                .priority(ProcessedActionItem.Priority.URGENT)
                .status(ProcessedActionItem.Status.IN_PROGRESS)
                .build();

        assertEquals("task-123", item.getId());
        assertEquals("Review code", item.getTitle());
        assertEquals(ProcessedActionItem.Priority.URGENT, item.getPriority());
        assertEquals(ProcessedActionItem.Status.IN_PROGRESS, item.getStatus());
    }

    @Test
    @DisplayName("Should support setter methods")
    void shouldSupportSetterMethods() {
        ProcessedActionItem actionItem = new ProcessedActionItem();

        actionItem.setMeetingId("new-meeting");
        actionItem.setPlatform("WEBEX");

        assertEquals("new-meeting", actionItem.getMeetingId());
        assertEquals("WEBEX", actionItem.getPlatform());
    }

    @Test
    @DisplayName("Should implement equals and hashCode")
    void shouldImplementEqualsAndHashCode() {
        Instant now = Instant.now();

        ProcessedActionItem item1 = ProcessedActionItem.builder()
                .meetingId("meeting-123")
                .platform("ZOOM")
                .processedTime(now)
                .build();

        ProcessedActionItem item2 = ProcessedActionItem.builder()
                .meetingId("meeting-123")
                .platform("ZOOM")
                .processedTime(now)
                .build();

        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString")
    void shouldImplementToString() {
        ProcessedActionItem actionItem = ProcessedActionItem.builder()
                .meetingId("meeting-123")
                .platform("GOOGLE_MEET")
                .build();

        String toString = actionItem.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("meeting-123"));
    }
}

