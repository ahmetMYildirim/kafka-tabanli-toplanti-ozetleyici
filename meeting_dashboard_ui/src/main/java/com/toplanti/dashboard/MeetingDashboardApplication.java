package com.toplanti.dashboard;

import com.toplanti.dashboard.service.AuthService;
import com.toplanti.dashboard.util.LanguageManager;
import com.toplanti.dashboard.util.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Objects;

/**
 * MeetingDashboardApplication - JavaFX tabanlı masaüstü dashboard uygulaması
 * @author Ömer
 * @version 1.0
 */
public class MeetingDashboardApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(MeetingDashboardApplication.class);
    private final LanguageManager lang = LanguageManager.getInstance();

    /**
     * JavaFX uygulama başlangıç metodu.
     * @param primaryStage JavaFX primary stage (main window)
     * @throws RuntimeException FXML yükleme veya scene oluşturma hatası durumunda
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            log.info("Meeting Dashboard Application starting...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 500, 700);
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/main.css")).toExternalForm()
            );

            ThemeManager.getInstance().setScene(scene);
            setupKeyboardShortcuts(scene, primaryStage);

            primaryStage.setTitle(lang.get("app.title"));
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(450);
            primaryStage.setMinHeight(600);
            primaryStage.setResizable(false);

            try {
                InputStream iconStream = getClass().getResourceAsStream("/images/app-icon.png");
                if (iconStream != null) {
                    primaryStage.getIcons().add(new Image(iconStream));
                }
            } catch (Exception e) {
                log.warn("Application icon could not be loaded: {}", e.getMessage());
            }

            primaryStage.centerOnScreen();
            primaryStage.show();
            log.info("Login screen started successfully (centered on screen)");

        } catch (Exception e) {
            log.error("Error starting application", e);
            throw new RuntimeException("Application could not be started", e);
        }
    }

    /**
     * Klavye kısayollarını setup eder.
     * @param scene Keyboard event'lerinin dinleneceği scene
     * @param stage Kapatma işlemi için stage referansı
     */
    private void setupKeyboardShortcuts(Scene scene, Stage stage) {
        scene.setOnKeyPressed(event -> {
            if (new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN).match(event)) {
                log.info("Application closing via Ctrl+Q");
                stage.close();
            }
            if (new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN).match(event)) {
                log.info("Theme toggling via Ctrl+D");
                ThemeManager.getInstance().toggleTheme();
            }
            if (new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN).match(event)) {
                log.info("Language switching via Ctrl+L");
                lang.setLanguage(lang.isTurkish() ? "en" : "tr");
            }
        });
    }

    /**
     * Uygulama kapatılırken çağrılır.
     */
    @Override
    public void stop() {
        log.info("Application closing...");
        AuthService.getInstance().logout();
    }

    /**
     * Uygulama giriş noktası.
     * @param args Komut satırı argümanları
     */
    public static void main(String[] args) {
        launch(args);
    }
}
