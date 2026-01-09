package com.toplanti.dashboard.component;

import com.toplanti.dashboard.model.MeetingSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MeetingCard extends VBox {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", new Locale("tr", "TR"))
                    .withZone(ZoneId.systemDefault());

    private final MeetingSummary meeting;

    public MeetingCard(MeetingSummary meeting) {
        this.meeting = meeting;
        buildCard();
    }

    private void buildCard() {
        getStyleClass().add("meeting-card");
        setPadding(new Insets(15));
        setSpacing(8);
        setPrefWidth(280);
        setMaxWidth(300);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label platformIcon = new Label(getPlatformIcon());
        platformIcon.getStyleClass().add("platform-icon");
        platformIcon.setStyle("-fx-font-size: 20px;");

        String titleText = meeting.getTitle() != null ? meeting.getTitle() : "Başlıksız Toplantı";
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        header.getChildren().addAll(platformIcon, title);

        Label platformLabel = new Label(meeting.getPlatform());
        platformLabel.getStyleClass().addAll("platform-badge",
                "platform-" + (meeting.getPlatform() != null ? meeting.getPlatform().toLowerCase() : "unknown"));

        Label date = new Label(formatDate(meeting.getMeetingStartTime()));
        date.getStyleClass().add("card-date");

        String summaryText = truncateSummary(meeting.getSummary(), 120);
        Label summaryPreview = new Label(summaryText);
        summaryPreview.getStyleClass().add("card-summary");
        summaryPreview.setWrapText(true);
        summaryPreview.setMaxHeight(60);

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);

        int participantCount = meeting.getParticipants() != null ? meeting.getParticipants().size() : 0;
        Label participants = new Label(participantCount + " katılımcı");
        participants.getStyleClass().add("card-participants");

        int keyPointCount = meeting.getKeyPoints() != null ? meeting.getKeyPoints().size() : 0;
        Label keyPoints = new Label("" + keyPointCount + " nokta");
        keyPoints.getStyleClass().add("card-keypoints");

        footer.getChildren().addAll(participants, keyPoints);

        getChildren().addAll(header, platformLabel, date, summaryPreview, footer);

        setStyle("-fx-cursor: hand;");
    }

    private String getPlatformIcon() {
        if (meeting.getPlatform() == null) {
            return "📋";
        }
        return switch (meeting.getPlatform().toUpperCase()) {
            case "DISCORD" -> "🎮";
            case "ZOOM" -> "📹";
            case "TEAMS" -> "💼";
            case "GOOGLE_MEET" -> "🎥";
            default -> "📋";
        };
    }

    private String formatDate(Instant instant) {
        if (instant == null) {
            return "Tarih bilinmiyor";
        }
        return DATE_FORMATTER.format(instant);
    }

    private String truncateSummary(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return "Özet mevcut değil";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    public String getMeetingId() {
        return meeting.getMeetingId();
    }

    public MeetingSummary getMeeting() {
        return meeting;
    }
}
