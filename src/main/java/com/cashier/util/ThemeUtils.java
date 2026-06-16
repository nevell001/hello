package com.cashier.util;

import com.cashier.service.DataService;
import javafx.scene.Scene;

import java.net.URL;

/**
 * Applies the shared application stylesheets to dynamically-created scenes.
 */
public final class ThemeUtils {
    private ThemeUtils() {
    }

    public static void applyCurrentTheme(Scene scene, Class<?> resourceClass) {
        if (scene == null || resourceClass == null) {
            return;
        }

        addStylesheet(scene, resourceClass, "/css/styles.css");
        String themeName = DataService.loadThemePreference();
        addStylesheet(scene, resourceClass, "/css/" + themeName + "-theme.css");
    }

    private static void addStylesheet(Scene scene, Class<?> resourceClass, String path) {
        URL url = resourceClass.getResource(path);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }
}
