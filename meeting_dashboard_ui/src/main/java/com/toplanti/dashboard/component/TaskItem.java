package com.toplanti.dashboard.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class TaskItem extends HBox {

    private final CheckBox checkBox;
    private final String task;
    private final String priority;
    private final String assignee;
    private boolean completed = false;

    public TaskItem(String task, String priority, String assignee) {
        this.task = task;
        this.priority = priority;
        this.assignee = assignee;
        this.checkBox = new CheckBox();

        buildComponent();
    }

    private void buildComponent() {
        getStyleClass().add("task-item");
        setPadding(new Insets(10));
        setSpacing(10);
        setAlignment(Pos.TOP_LEFT);

        checkBox.setOnAction(e -> {
            completed = checkBox.isSelected();
            updateCompletedStyle();
        });

        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);

        Label taskLabel = new Label(task);
        taskLabel.getStyleClass().add("task-text");
        taskLabel.setWrapText(true);
        taskLabel.setMaxWidth(180);

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label priorityLabel = new Label(getPriorityText());
        priorityLabel.getStyleClass().addAll("priority-badge", "priority-" + priority.toLowerCase());

        if (assignee != null && !assignee.isEmpty() && !assignee.equals("Atanmadı")) {
            Label assigneeLabel = new Label("👤 " + assignee);
            assigneeLabel.getStyleClass().add("task-assignee");
            meta.getChildren().addAll(priorityLabel, assigneeLabel);
        } else {
            meta.getChildren().add(priorityLabel);
        }

        content.getChildren().addAll(taskLabel, meta);
        getChildren().addAll(checkBox, content);
    }

    private String getPriorityText() {
        return switch (priority.toUpperCase()) {
            case "HIGH" -> "🔴 Yüksek";
            case "MEDIUM" -> "🟡 Orta";
            case "LOW" -> "🟢 Düşük";
            default -> priority;
        };
    }

    private void updateCompletedStyle() {
        if (completed) {
            getStyleClass().add("task-completed");
        } else {
            getStyleClass().remove("task-completed");
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        checkBox.setSelected(completed);
        updateCompletedStyle();
    }

    public String getTask() {
        return task;
    }

    public String getPriority() {
        return priority;
    }

    public String getAssignee() {
        return assignee;
    }
}
