package com.toplanti.dashboard.util;

import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ThemeManager {

    private static final Logger log = LoggerFactory.getLogger(ThemeManager.class);
    private static ThemeManager instance;
    private Theme currentTheme;
    private Scene currentScene;
    private String mainCssUrl;
    private String darkCssUrl;

    public enum Theme {
        LIGHT("light", "Açık Tema", "Light Theme"),
        DARK("dark", "Koyu Tema", "Dark Theme");

        private final String id;
        private final String displayNameTr;
        private final String displayNameEn;

        Theme(String id, String displayNameTr, String displayNameEn) {
            this.id = id;
            this.displayNameTr = displayNameTr;
            this.displayNameEn = displayNameEn;
        }

        public String getId() { return id; }
        public String getDisplayName(boolean turkish) {
            return turkish ? displayNameTr : displayNameEn;
        }
    }

    private ThemeManager() {
        this.currentTheme = Theme.LIGHT;
        try {
            this.mainCssUrl = Objects.requireNonNull(getClass().getResource("/css/main.css")).toExternalForm();
            this.darkCssUrl = Objects.requireNonNull(getClass().getResource("/css/dark-theme.css")).toExternalForm();
        } catch (Exception e) {
            log.error("CSS dosyaları yüklenemedi", e);
        }
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void setScene(Scene scene) {
        this.currentScene = scene;
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        applyTheme();
    }

    public void toggleTheme() {
        currentTheme = currentTheme == Theme.LIGHT ? Theme.DARK : Theme.LIGHT;
        applyTheme();
    }

    private void applyTheme() {
        if (currentScene == null) return;

        log.info("Tema değiştiriliyor: {}", currentTheme);

        currentScene.getStylesheets().clear();

        if (currentTheme == Theme.DARK) {
            if (darkCssUrl != null) {
                currentScene.getStylesheets().add(darkCssUrl);
                log.info("Dark tema yüklendi: {}", darkCssUrl);
            }
        } else {
            if (mainCssUrl != null) {
                currentScene.getStylesheets().add(mainCssUrl);
                log.info("Light tema yüklendi: {}", mainCssUrl);
            }
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }
}
