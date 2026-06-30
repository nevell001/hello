package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WelcomeHelpPolicyTest {

    @Test
    @DisplayName("欢迎页查看帮助必须打开帮助窗口而不是关于窗口")
    void welcomeHelpButtonOpensHelpDialog() throws Exception {
        String mainView = Files.readString(Path.of(
            "src/main/resources/com/cashier/view/MainView.fxml"
        ));

        assertTrue(hasWelcomeHelpAction(mainView, "handleShortcutHelp"),
            "欢迎页查看帮助按钮应绑定快捷键帮助窗口");
        assertFalse(hasWelcomeHelpAction(mainView, "handleAbout"),
            "欢迎页查看帮助按钮不能绑定关于窗口");
    }

    @Test
    @DisplayName("帮助窗口和关于弹窗不应破坏当前页面状态")
    void helpAndAboutDialogsPreserveCurrentPageStatus() throws Exception {
        String mainController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MainController.java"
        ));

        assertTrue(mainController.contains("ThemeUtils.applyCurrentTheme(scene, getClass())"),
            "快捷键帮助窗口应套用当前主题");
        assertTrue(mainController.contains("controller.setStage(dialogStage)"),
            "快捷键帮助窗口应把 Stage 传给控制器用于关闭");
        assertTrue(mainController.contains("showInformationOnlyAlert("),
            "关于弹窗应使用不写状态栏的信息弹窗");

        int aboutStart = mainController.indexOf("public void handleAbout()");
        assertTrue(aboutStart >= 0, "未找到关于入口");
        int aboutEnd = mainController.indexOf("\n    private void showInformationOnlyAlert", aboutStart);
        String aboutBody = mainController.substring(aboutStart, aboutEnd);

        assertFalse(aboutBody.contains("updateStatus("));
        assertFalse(aboutBody.contains("FXUtils.showInfoAlert("));
    }

    @Test
    @DisplayName("快捷键帮助窗口应支持全窗口 ESC 关闭")
    void shortcutHelpEscClosesFromWholeScene() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ShortcutHelpController.java"
        ));

        assertTrue(controller.contains("stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED"));
        assertTrue(controller.contains("event.getCode() == KeyCode.ESCAPE"));
        assertTrue(controller.contains("event.consume()"));
        assertFalse(controller.contains("contentArea.setOnKeyPressed"));
    }

    private boolean hasWelcomeHelpAction(String mainView, String action) {
        return Pattern.compile(
            "<Button\\b(?=[^>]*text=\"%main\\.view_help\")(?=[^>]*onAction=\"#" + action + "\")[^>]*/>",
            Pattern.DOTALL
        ).matcher(mainView).find();
    }
}
