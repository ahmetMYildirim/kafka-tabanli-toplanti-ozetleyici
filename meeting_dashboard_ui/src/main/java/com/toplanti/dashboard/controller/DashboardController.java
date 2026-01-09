package com.toplanti.dashboard.controller;

import com.toplanti.dashboard.component.MeetingCard;
import com.toplanti.dashboard.component.TaskItem;
import com.toplanti.dashboard.model.*;
import com.toplanti.dashboard.service.ApiService;
import com.toplanti.dashboard.service.AuthService;
import com.toplanti.dashboard.service.WebSocketService;
import com.toplanti.dashboard.util.LanguageManager;
import com.toplanti.dashboard.util.ThemeManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private final LanguageManager lang = LanguageManager.getInstance();
    private final AuthService authService = AuthService.getInstance();

    @FXML private VBox taskList;
    @FXML private FlowPane meetingCards;
    @FXML private ComboBox<String> platformFilter;
    @FXML private VBox summaryContent;
    @FXML private TextArea transcriptionText;
    @FXML private VBox actionItemsList;
    @FXML private VBox detailPanel;
    @FXML private Label statsLabel;
    @FXML private Label connectionStatus;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> languageCombo;
    @FXML private Button themeToggleButton;
    @FXML private Button logoutButton;
    @FXML private Label tasksPanelTitle;
    @FXML private Label meetingsPanelTitle;
    @FXML private Button exportButton;
    @FXML private MenuButton settingsMenu;

    private final ApiService apiService;
    private final WebSocketService webSocketService;
    private String selectedMeetingId;
    private MeetingSummary selectedMeeting;

    public DashboardController() {
        this.apiService = new ApiService();
        this.webSocketService = new WebSocketService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("Dashboard başlatılıyor...");

        setupUserInfo();
        setupLanguageSelector();
        setupPlatformFilter();
        setupSearch();
        setupKeyboardShortcuts();

        if (detailPanel != null) {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
        }

        loadDashboardStats();
        loadMeetings();
        loadUserTasks();
        setupWebSocket();
        updateTexts();

        Platform.runLater(() -> {
            if (meetingCards != null && meetingCards.getScene() != null) {
                ThemeManager.getInstance().setScene(meetingCards.getScene());
            }
        });

        log.info("Dashboard başlatıldı");
    }

    private void setupUserInfo() {
        authService.getCurrentUser().ifPresent(user -> {
            if (userNameLabel != null) {
                userNameLabel.setText(user.getFullName());
            }
            if (userRoleLabel != null) {
                userRoleLabel.setText(user.getRole().getDisplayName());
                userRoleLabel.getStyleClass().add("role-" + user.getRole().name().toLowerCase());
            }
        });
    }

    private void setupLanguageSelector() {
        if (languageCombo != null) {
            languageCombo.getItems().addAll("Türkçe", "English");
            languageCombo.setValue(lang.isTurkish() ? "Türkçe" : "English");
            languageCombo.setOnAction(e -> {
                lang.setLanguage(languageCombo.getValue().equals("Türkçe") ? "tr" : "en");
                updateTexts();
                loadMeetings();
            });
        }
    }

    private void setupPlatformFilter() {
        platformFilter.setItems(FXCollections.observableArrayList(
            lang.get("meetings.filter.all"), "DISCORD", "ZOOM"
        ));
        platformFilter.setValue(lang.get("meetings.filter.all"));
        platformFilter.setOnAction(e -> refreshMeetings());
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.setPromptText(lang.get("search.placeholder"));
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filterMeetings(newVal);
            });
        }
    }

    private void setupKeyboardShortcuts() {
        Platform.runLater(() -> {
            if (searchField != null && searchField.getScene() != null) {
                searchField.getScene().setOnKeyPressed(event -> {
                    if (new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN).match(event)) {
                        searchField.requestFocus();
                    }
                    if (event.getCode() == KeyCode.F5) {
                        refreshMeetings();
                    }
                    if (new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN).match(event)) {
                        toggleTheme();
                    }
                    if (new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN).match(event)) {
                        showSettings();
                    }
                });
            }
        });
    }

    private void updateTexts() {
        if (tasksPanelTitle != null) tasksPanelTitle.setText(lang.get("tasks.title"));
        if (meetingsPanelTitle != null) meetingsPanelTitle.setText(lang.get("meetings.title"));
        if (connectionStatus != null) {
            connectionStatus.setText(webSocketService.isConnected() ?
                lang.get("dashboard.connected") : lang.get("dashboard.disconnected"));
        }
        if (searchField != null) searchField.setPromptText(lang.get("search.placeholder"));
    }

    private void loadDashboardStats() {
        apiService.getDashboardStats()
                .thenAccept(stats -> Platform.runLater(() -> {
                    if (statsLabel != null && stats != null) {
                        Object totalMeetings = stats.get("totalMeetings");
                        Object totalActionItems = stats.get("totalActionItems");
                        statsLabel.setText(lang.get("dashboard.stats",
                                totalMeetings != null ? totalMeetings : "0",
                                totalActionItems != null ? totalActionItems : "0"));
                    }
                }))
                .exceptionally(ex -> {
                    log.error("İstatistikler yüklenirken hata", ex);
                    Platform.runLater(() -> {
                        
                        if (statsLabel != null) {
                            statsLabel.setText(lang.isTurkish() ? "İstatistikler yüklenemedi" : "Stats unavailable");
                        }
                    });
                    return null;
                });
    }

    private void loadMeetings() {
        String platform = platformFilter.getValue();
        if (lang.get("meetings.filter.all").equals(platform)) {
            platform = null;
        }

        log.debug("Toplantılar yükleniyor. Platform: {}", platform);

        apiService.getMeetings(platform, 20)
                .thenAccept(meetings -> Platform.runLater(() -> {
                    meetingCards.getChildren().clear();

                    if (meetings == null || meetings.isEmpty()) {
                        Label noDataLabel = new Label(lang.get("meetings.empty"));
                        noDataLabel.getStyleClass().add("no-data-label");
                        meetingCards.getChildren().add(noDataLabel);
                        return;
                    }

                    for (MeetingSummary meeting : meetings) {
                        MeetingCard card = new MeetingCard(meeting);
                        card.setOnMouseClicked(e -> showMeetingDetail(meeting.getMeetingId(), meeting));
                        meetingCards.getChildren().add(card);
                    }

                    log.debug("{} toplantı yüklendi", meetings.size());
                }))
                .exceptionally(ex -> {
                    log.error("Toplantılar yüklenirken hata", ex);
                    Platform.runLater(() -> {
                        String errorMsg = ex.getMessage();
                        if (ex.getCause() != null) {
                            errorMsg = ex.getCause().getMessage();
                        }
                        
                        String userMessage;
                        if (errorMsg != null && errorMsg.contains("Connection refused") || 
                            (errorMsg != null && errorMsg.contains("8084"))) {
                            userMessage = lang.isTurkish()
                                ? "Gateway API'ye bağlanılamadı.\n\nLütfen:\n1. Docker servislerinin çalıştığından emin olun\n2. Port 8084'ün açık olduğunu kontrol edin"
                                : "Cannot connect to Gateway API.\n\nPlease:\n1. Make sure Docker services are running\n2. Check if port 8084 is open";
                        } else {
                            userMessage = lang.get("error.loading");
                        }
                        showError(userMessage);
                    });
                    return null;
                });
    }

    private void filterMeetings(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            meetingCards.getChildren().forEach(node -> {
                node.setVisible(true);
                node.setManaged(true);
            });
            return;
        }

        String lowerSearch = searchText.toLowerCase();
        meetingCards.getChildren().forEach(node -> {
            if (node instanceof MeetingCard card) {
                MeetingSummary meeting = card.getMeeting();
                boolean matches = (meeting.getTitle() != null && meeting.getTitle().toLowerCase().contains(lowerSearch)) ||
                        (meeting.getSummary() != null && meeting.getSummary().toLowerCase().contains(lowerSearch)) ||
                        (meeting.getPlatform() != null && meeting.getPlatform().toLowerCase().contains(lowerSearch));
                node.setVisible(matches);
                node.setManaged(matches);
            }
        });
    }

    private void loadUserTasks() {
        log.debug("Görevler yükleniyor...");
        taskList.getChildren().clear();

        apiService.getAllActionItems()
                .thenAccept(actionItems -> {
                    if (actionItems == null) return;

                    Platform.runLater(() -> {
                        for (ActionItem item : actionItems) {
                            if (item.getActionItems() != null) {
                                for (String task : item.getActionItems()) {
                                    String priority = extractPriority(task);
                                    String assignee = extractAssignee(task);
                                    TaskItem taskItem = new TaskItem(task, priority, assignee);
                                    taskList.getChildren().add(taskItem);
                                }
                            }
                        }
                    });
                })
                .exceptionally(ex -> {
                    log.error("Görevler yüklenirken hata", ex);
                    return null;
                });
    }

    private String extractPriority(String task) {
        String lowerTask = task.toLowerCase();
        if (lowerTask.contains("acil") || lowerTask.contains("urgent") || lowerTask.contains("kritik") || lowerTask.contains("critical")) {
            return "HIGH";
        } else if (lowerTask.contains("önemli") || lowerTask.contains("important")) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String extractAssignee(String task) {
        if (task.contains("@")) {
            int start = task.indexOf("@");
            int end = task.indexOf(" ", start);
            if (end == -1) end = task.length();
            return task.substring(start + 1, Math.min(end, start + 20));
        }
        return lang.isTurkish() ? "Atanmadı" : "Unassigned";
    }

    private void showMeetingDetail(String meetingId, MeetingSummary meeting) {
        this.selectedMeetingId = meetingId;
        this.selectedMeeting = meeting;

        log.debug("Toplantı detayı gösteriliyor: {}", meetingId);

        if (detailPanel != null) {
            detailPanel.setVisible(true);
            detailPanel.setManaged(true);
        }

        apiService.getMeetingSummary(meetingId)
                .thenAccept(summary -> Platform.runLater(() -> {
                    if (summaryContent == null) return;
                    summaryContent.getChildren().clear();

                    if (summary == null) {
                        summaryContent.getChildren().add(new Label(lang.get("detail.summary.notfound")));
                        return;
                    }

                    Label title = new Label(summary.getTitle() != null ? summary.getTitle() : lang.isTurkish() ? "Başlıksız" : "Untitled");
                    title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

                    Label meta = new Label(String.format("📌 %s | " + lang.get("meetings.participants",
                            summary.getParticipants() != null ? summary.getParticipants().size() : 0),
                            summary.getPlatform()));
                    meta.setStyle("-fx-text-fill: #666;");

                    Label summaryText = new Label(summary.getSummary() != null ? summary.getSummary() : lang.get("detail.summary.notfound"));
                    summaryText.setWrapText(true);
                    summaryText.setStyle("-fx-padding: 10 0;");

                    summaryContent.getChildren().addAll(title, meta, summaryText);

                    if (summary.getKeyPoints() != null && !summary.getKeyPoints().isEmpty()) {
                        Label keyPointsTitle = new Label(lang.get("detail.keypoints"));
                        keyPointsTitle.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
                        summaryContent.getChildren().add(keyPointsTitle);

                        for (String point : summary.getKeyPoints()) {
                            Label pointLabel = new Label("• " + point);
                            pointLabel.setWrapText(true);
                            summaryContent.getChildren().add(pointLabel);
                        }
                    }

                    if (summary.getParticipants() != null && !summary.getParticipants().isEmpty()) {
                        Label participantsTitle = new Label(lang.get("detail.participants.title"));
                        participantsTitle.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0;");
                        summaryContent.getChildren().add(participantsTitle);

                        Label participantsList = new Label(String.join(", ", summary.getParticipants()));
                        participantsList.setWrapText(true);
                        summaryContent.getChildren().add(participantsList);
                    }
                }))
                .exceptionally(ex -> {
                    log.error("Özet yüklenirken hata", ex);
                    Platform.runLater(() -> {
                        if (summaryContent != null) {
                            summaryContent.getChildren().clear();
                            summaryContent.getChildren().add(new Label(lang.get("error.loading")));
                        }
                    });
                    return null;
                });

        apiService.getTranscription(meetingId)
                .thenAccept(transcription -> Platform.runLater(() -> {
                    if (transcriptionText == null) return;

                    if (transcription != null && transcription.getFullTranscription() != null) {
                        transcriptionText.setText(transcription.getFullTranscription());
                    } else {
                        transcriptionText.setText(lang.get("detail.transcription.notfound"));
                    }
                }))
                .exceptionally(ex -> {
                    log.debug("Transkript bulunamadı: {}", meetingId);
                    Platform.runLater(() -> {
                        if (transcriptionText != null) {
                            transcriptionText.setText(lang.get("detail.transcription.notfound"));
                        }
                    });
                    return null;
                });

        apiService.getActionItems(meetingId)
                .thenAccept(actionItem -> Platform.runLater(() -> {
                    if (actionItemsList == null) return;
                    actionItemsList.getChildren().clear();

                    if (actionItem != null && actionItem.getActionItems() != null) {
                        for (String task : actionItem.getActionItems()) {
                            String priority = extractPriority(task);
                            String assignee = extractAssignee(task);
                            actionItemsList.getChildren().add(new TaskItem(task, priority, assignee));
                        }
                    } else {
                        actionItemsList.getChildren().add(new Label(lang.get("detail.tasks.empty")));
                    }
                }))
                .exceptionally(ex -> {
                    log.debug("Görevler bulunamadı: {}", meetingId);
                    Platform.runLater(() -> {
                        if (actionItemsList != null) {
                            actionItemsList.getChildren().clear();
                            actionItemsList.getChildren().add(new Label(lang.get("error.loading")));
                        }
                    });
                    return null;
                });
    }

    @FXML
    private void refreshMeetings() {
        log.info("Toplantı listesi yenileniyor...");
        loadMeetings();
        loadDashboardStats();
    }

    @FXML
    private void toggleTheme() {
        if (meetingCards != null && meetingCards.getScene() != null) {
            ThemeManager.getInstance().setScene(meetingCards.getScene());
        }
        ThemeManager.getInstance().toggleTheme();
        if (themeToggleButton != null) {
            themeToggleButton.setText(ThemeManager.getInstance().isDarkMode() ? "☀️" : "🌙");
        }
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(lang.get("confirm.title"));
        alert.setHeaderText(null);
        alert.setContentText(lang.get("confirm.logout"));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            authService.logout();
            openLoginScreen();
        }
    }

    @FXML
    private void uploadAudioFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(lang.isTurkish() ? "Ses/Video Dosyası Seç" : "Select Audio/Video File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.ogg", "*.m4a", "*.webm"),
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mov", "*.avi", "*.webm"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(meetingCards.getScene().getWindow());
        if (file != null) {
            
            TextInputDialog dialog = new TextInputDialog(file.getName().replaceAll("\\.[^.]+$", ""));
            dialog.setTitle(lang.isTurkish() ? "Toplantı Adı" : "Meeting Title");
            dialog.setHeaderText(lang.isTurkish() ? "Bu toplantı için bir ad girin:" : "Enter a title for this meeting:");
            dialog.setContentText(lang.isTurkish() ? "Toplantı Adı:" : "Meeting Title:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(meetingTitle -> {
                if (meetingTitle.trim().isEmpty()) {
                    meetingTitle = file.getName();
                }

                
                Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
                progressAlert.setTitle(lang.isTurkish() ? "Yükleniyor..." : "Uploading...");
                progressAlert.setHeaderText(lang.isTurkish() 
                        ? "Dosya işleniyor..." 
                        : "Processing file...");
                progressAlert.setContentText(lang.isTurkish()
                        ? "Dosya yükleniyor ve AI tarafından analiz ediliyor.\nBu işlem 3-5 dakika sürebilir.\n\nLütfen bekleyin..."
                        : "File is being uploaded and analyzed by AI.\nThis may take 3-5 minutes.\n\nPlease wait...");
                progressAlert.show();

                final String finalTitle = meetingTitle;
                
                
                apiService.uploadMedia(file, finalTitle)
                        .thenAccept(response -> Platform.runLater(() -> {
                            progressAlert.close();
                            showInfo(lang.isTurkish()
                                    ? "Dosya başarıyla yüklendi!\n\nToplantı analizi başlatıldı.\nSonuçlar birkaç dakika içinde görünecek."
                                    : "File uploaded successfully!\n\nMeeting analysis started.\nResults will appear in a few minutes.");
                            
                            
                            new Thread(() -> {
                                try {
                                    Thread.sleep(5000);
                                    Platform.runLater(this::refreshMeetings);
                                } catch (InterruptedException e) {
                                    log.error("Sleep interrupted", e);
                                }
                            }).start();
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> {
                                progressAlert.close();
                                log.error("Dosya yükleme hatası", ex);
                                
                                String errorMessage = ex.getMessage();
                                if (ex.getCause() != null) {
                                    errorMessage = ex.getCause().getMessage();
                                }
                                
                                
                                String userMessage;
                                if (errorMessage != null && errorMessage.contains("Connection refused")) {
                                    userMessage = lang.isTurkish()
                                        ? "Collector Service'e bağlanılamadı.\n\nLütfen:\n1. Docker servislerinin çalıştığından emin olun\n2. Port 8081'in açık olduğunu kontrol edin"
                                        : "Cannot connect to Collector Service.\n\nPlease:\n1. Make sure Docker services are running\n2. Check if port 8081 is open";
                                } else if (errorMessage != null && errorMessage.contains("timeout")) {
                                    userMessage = lang.isTurkish()
                                        ? "Dosya yükleme zaman aşımına uğradı.\n\nDosya çok büyük olabilir veya ağ bağlantısı yavaş olabilir."
                                        : "File upload timed out.\n\nThe file may be too large or the network connection may be slow.";
                                } else {
                                    userMessage = lang.isTurkish()
                                        ? "Dosya yüklenemedi: " + (errorMessage != null ? errorMessage : "Bilinmeyen hata")
                                        : "File upload failed: " + (errorMessage != null ? errorMessage : "Unknown error");
                                }
                                
                                showError(userMessage);
                            });
                            return null;
                        });
            });
        }
    }

    @FXML
    private void exportToPdf() {
        if (selectedMeeting == null) {
            showInfo(lang.isTurkish() ? "Lütfen bir toplantı seçin" : "Please select a meeting");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(lang.get("detail.export.pdf"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        fileChooser.setInitialFileName(selectedMeeting.getTitle() + ".txt");

        File file = fileChooser.showSaveDialog(meetingCards.getScene().getWindow());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("=== " + selectedMeeting.getTitle() + " ===\n\n");
                writer.write("Platform: " + selectedMeeting.getPlatform() + "\n");
                writer.write("Date: " + selectedMeeting.getMeetingStartTime() + "\n\n");
                writer.write("Summary:\n" + selectedMeeting.getSummary() + "\n\n");
                if (selectedMeeting.getKeyPoints() != null) {
                    writer.write("Key Points:\n");
                    for (String point : selectedMeeting.getKeyPoints()) {
                        writer.write("• " + point + "\n");
                    }
                }
                showInfo(lang.get("success.exported"));
            } catch (Exception e) {
                log.error("Export failed", e);
                showError(lang.get("error.loading"));
            }
        }
    }

    @FXML
    private void showSettings() {
        log.info("Settings açılıyor...");
    }

    private void openLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) meetingCards.getScene().getWindow();
            Scene scene = new Scene(root, 500, 700);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle(lang.get("app.title"));
            stage.setResizable(false);

        } catch (Exception e) {
            log.error("Failed to open login screen", e);
        }
    }

    private void setupWebSocket() {
        webSocketService.setOnConnected(v -> {
            log.info("WebSocket bağlantısı kuruldu");
            Platform.runLater(() -> {
                if (connectionStatus != null) {
                    connectionStatus.setText(lang.get("dashboard.connected"));
                    connectionStatus.setStyle("-fx-text-fill: #27ae60;");
                }
            });
        });

        webSocketService.setOnDisconnected(reason -> {
            log.info("WebSocket bağlantısı kesildi: {}", reason);
            Platform.runLater(() -> {
                if (connectionStatus != null) {
                    connectionStatus.setText(lang.get("dashboard.disconnected"));
                    connectionStatus.setStyle("-fx-text-fill: #e74c3c;");
                }
            });
        });

        webSocketService.setOnNewSummary(message -> {
            log.debug("Yeni özet bildirimi alındı");
            loadMeetings();
            showNotification(lang.get("notification.newmeeting"), "info");
        });

        webSocketService.setOnNewTranscription(message -> {
            log.debug("Yeni transkript bildirimi alındı");
            if (selectedMeetingId != null) {
                showMeetingDetail(selectedMeetingId, selectedMeeting);
            }
        });

        webSocketService.setOnNewActionItem(message -> {
            log.debug("Yeni görev bildirimi alındı");
            loadUserTasks();
            showNotification(lang.get("notification.newtask"), "info");
        });
    }

    private void showNotification(String message, String type) {
        Platform.runLater(() -> {
            log.info("Notification: {} - {}", type, message);
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(lang.get("error.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(lang.get("success.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
