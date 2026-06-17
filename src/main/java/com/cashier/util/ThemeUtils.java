package com.cashier.util;

import com.cashier.CashierSystemFXApplication;
import com.cashier.model.User;
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
        String username = "default";
        CashierSystemFXApplication app = CashierSystemFXApplication.getInstance();
        User currentUser = app != null ? app.getCurrentUser() : null;
        if (currentUser != null) {
            username = currentUser.username;
        }
        String themeName = DataService.loadThemePreference(username);
        addStylesheet(scene, resourceClass, "/css/" + themeName + "-theme.css");
    }

    private static void addStylesheet(Scene scene, Class<?> resourceClass, String path) {
        URL url = resourceClass.getResource(path);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }
}
