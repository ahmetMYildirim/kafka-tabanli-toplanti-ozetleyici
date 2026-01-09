package com.toplanti.dashboard.controller;

import com.toplanti.dashboard.service.AuthService;
import com.toplanti.dashboard.util.LanguageManager;
import com.toplanti.dashboard.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private final LanguageManager lang = LanguageManager.getInstance();
    private final AuthService authService = AuthService.getInstance();

    @FXML private VBox loginPane;
    @FXML private VBox registerPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regConfirmPasswordField;
    @FXML private TextField regEmailField;
    @FXML private TextField regFullNameField;
    @FXML private ComboBox<String> regDepartmentCombo;
    @FXML private Label errorLabel;
    @FXML private Label regErrorLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Hyperlink registerLink;
    @FXML private Hyperlink loginLink;
    @FXML private ComboBox<String> languageCombo;
    @FXML private Label titleLabel;
    @FXML private Label loginTitleLabel;
    @FXML private Label registerTitleLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        log.info("LoginController initializing...");

        languageCombo.getItems().addAll("Türkçe", "English");
        languageCombo.setValue(lang.isTurkish() ? "Türkçe" : "English");
        languageCombo.setOnAction(e -> {
            lang.setLanguage(languageCombo.getValue().equals("Türkçe") ? "tr" : "en");
            updateTexts();
        });

        regDepartmentCombo.getItems().addAll(
            "Engineering", "Marketing", "Sales", "HR", "Finance", "Operations", "IT"
        );
        regDepartmentCombo.setValue("Engineering");

        registerPane.setVisible(false);
        registerPane.setManaged(false);

        usernameField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) passwordField.requestFocus(); });
        passwordField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) handleLogin(); });

        updateTexts();
    }

    private void updateTexts() {
        if (titleLabel != null) titleLabel.setText(lang.get("app.title"));
        if (loginTitleLabel != null) loginTitleLabel.setText(lang.get("login.title"));
        if (registerTitleLabel != null) registerTitleLabel.setText(lang.get("register.title"));
        if (usernameField != null) usernameField.setPromptText(lang.get("login.username"));
        if (passwordField != null) passwordField.setPromptText(lang.get("login.password"));
        if (loginButton != null) loginButton.setText(lang.get("login.button"));
        if (registerButton != null) registerButton.setText(lang.get("register.button"));
        if (registerLink != null) registerLink.setText(lang.get("login.register"));
        if (loginLink != null) loginLink.setText(lang.get("register.login"));
        if (regFullNameField != null) regFullNameField.setPromptText(lang.get("register.fullname"));
        if (regEmailField != null) regEmailField.setPromptText(lang.get("register.email"));
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError(lang.get("error.validation"));
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setVisible(false);

        authService.login(username, password)
                .thenAccept(success -> Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    if (success) {
                        log.info("Login successful, opening dashboard");
                        openDashboard();
                    } else {
                        showError(lang.get("login.error"));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        loginButton.setDisable(false);
                        showError(lang.get("error.connection"));
                    });
                    return null;
                });
    }

    @FXML
    private void handleRegister() {
        String username = regUsernameField.getText().trim();
        String password = regPasswordField.getText();
        String confirmPassword = regConfirmPasswordField.getText();
        String email = regEmailField.getText().trim();
        String fullName = regFullNameField.getText().trim();
        String department = regDepartmentCombo.getValue();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || fullName.isEmpty()) {
            showRegError(lang.get("error.validation"));
            return;
        }

        if (!password.equals(confirmPassword)) {
            showRegError("Passwords do not match");
            return;
        }

        registerButton.setDisable(true);
        regErrorLabel.setVisible(false);

        authService.register(username, password, email, fullName, department)
                .thenAccept(success -> Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    if (success) {
                        log.info("Registration successful, opening dashboard");
                        openDashboard();
                    } else {
                        showRegError(lang.get("error.connection"));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        registerButton.setDisable(false);
                        showRegError(lang.get("error.connection"));
                    });
                    return null;
                });
    }

    @FXML
    private void showRegisterForm() {
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        registerPane.setVisible(true);
        registerPane.setManaged(true);
        regUsernameField.requestFocus();
    }

    @FXML
    private void showLoginForm() {
        registerPane.setVisible(false);
        registerPane.setManaged(false);
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        usernameField.requestFocus();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void showRegError(String message) {
        regErrorLabel.setText(message);
        regErrorLabel.setVisible(true);
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1400, 900);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

            ThemeManager.getInstance().setScene(scene);

            stage.setScene(scene);
            stage.setTitle(lang.get("app.title"));
            stage.setMinWidth(1200);
            stage.setMinHeight(800);
            stage.setResizable(true);
            stage.centerOnScreen();

        } catch (Exception e) {
            log.error("Failed to open dashboard", e);
            showError(lang.get("error.loading"));
        }
    }
}

