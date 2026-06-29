package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsBackupRestoreFeedbackPolicyTest {

    @Test
    @DisplayName("备份路径创建失败应给出明确错误")
    void backupPathCreateFailureShowsExplicitError() throws Exception {
        String settingsController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/SettingsController.java"
        ));

        assertTrue(settingsController.contains("!backupDir.exists() && !backupDir.mkdirs()"));
        assertTrue(settingsController.contains("runtime.backup_path_create_failed"));
        assertTrue(settingsController.contains("showError(I18nManager.getInstance().get(\"runtime.backup_path_create_failed\""));
    }

    @Test
    @DisplayName("恢复备份取消选择或取消确认应同步状态栏")
    void restoreCancelUpdatesStatusBar() throws Exception {
        String settingsController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/SettingsController.java"
        ));

        assertTrue(settingsController.contains("java.util.Optional<String> selectedBackup = dialog.showAndWait()"));
        assertTrue(settingsController.contains("if (selectedBackup.isEmpty())"));
        assertTrue(settingsController.contains("StatusBarManager.updateWarning"));
        assertTrue(settingsController.contains("I18nManager.getInstance().get(\"status.cancelled\")"));
        assertTrue(settingsController.contains("} else {\n                    com.cashier.util.StatusBarManager.updateWarning"));
    }

    @Test
    @DisplayName("备份路径创建失败文案应具备完整国际化")
    void backupPathCreateFailureIsLocalized() throws Exception {
        List<String> bundleFiles = List.of(
            "src/main/resources/com/cashier/i18n/messages.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_CN.properties",
            "src/main/resources/com/cashier/i18n/messages_en.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_TW.properties"
        );

        for (String file : bundleFiles) {
            String bundle = Files.readString(Path.of(file));
            assertTrue(bundle.contains("runtime.backup_path_create_failed="), file);
        }
    }
}
