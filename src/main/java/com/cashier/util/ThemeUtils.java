package com.cashier.util;

import com.cashier.CashierSystemFXApplication;
import com.cashier.model.User;
import com.cashier.service.DataService;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.net.URL;

/**
 * Applies the shared application stylesheets to dynamically-created scenes.
 */
public final class ThemeUtils {
    private ThemeUtils() {
    }

    /**
     * 将主场景的主题样式（含字号偏好）应用到对话框，保证弹窗与主界面视觉一致。
     */
    public static void applyDialogTheme(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }
        CashierSystemFXApplication app = CashierSystemFXApplication.getInstance();
        if (app == null) {
            return;
        }
        Scene source = app.getPrimaryScene();
        if (source == null) {
            return;
        }
        dialogPane.getStylesheets().addAll(source.getStylesheets());
        // 同步字号偏好类（如 font-size-medium），使弹窗继承用户字号设置
        source.getRoot().getStyleClass().stream()
            .filter(cssClass -> cssClass.startsWith("font-size-"))
            .forEach(dialogPane.getStyleClass()::add);
    }

    /**
     * 将源场景的主题样式（含字号偏好）复制到目标场景。
     */
    public static void copyThemeToScene(Scene target, Scene source) {
        if (target == null || source == null) {
            return;
        }
        target.getStylesheets().addAll(source.getStylesheets());
        if (source.getRoot() != null && target.getRoot() != null) {
            source.getRoot().getStyleClass().stream()
                .filter(cssClass -> cssClass.startsWith("font-size-"))
                .forEach(target.getRoot().getStyleClass()::add);
        }
    }

    /**
     * 全局弹窗主题化：监听所有新窗口（Dialog/Alert/Stage），
     * 在显示前自动复制主场景主题样式，避免弹窗残留默认浅色。
     * 必须在 JavaFX 应用线程调用（start() 中调用一次即可）。
     */
    public static void installGlobalDialogTheming() {
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                for (Window added : change.getAddedSubList()) {
                    themeWindowWhenReady(added);
                }
            }
        });
    }

    private static void themeWindowWhenReady(Window window) {
        Runnable apply = () -> {
            CashierSystemFXApplication app = CashierSystemFXApplication.getInstance();
            Scene source = app != null ? app.getPrimaryScene() : null;
            copyThemeToScene(window.getScene(), source);
        };
        // 覆盖窗口加入列表后 scene 尚未就绪、以及就绪后的多个时机
        window.addEventHandler(WindowEvent.WINDOW_SHOWING, event -> apply.run());
        window.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> apply.run());
        if (window.getScene() != null) {
            apply.run();
        }
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

        String fontSize = DataService.loadFontSizePreference(username);
        if (!java.util.Set.of("small", "medium", "large", "extra-large").contains(fontSize)) {
            fontSize = "medium";
        }
        scene.getRoot().getStyleClass().removeAll(
            "font-size-small", "font-size-medium", "font-size-large", "font-size-extra-large");
        scene.getRoot().getStyleClass().add("font-size-" + fontSize);
    }

    private static void addStylesheet(Scene scene, Class<?> resourceClass, String path) {
        URL url = resourceClass.getResource(path);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }
}
