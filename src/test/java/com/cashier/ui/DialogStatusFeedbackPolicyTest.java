package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogStatusFeedbackPolicyTest {

    @Test
    @DisplayName("通用信息弹窗也应同步状态栏")
    void infoDialogsUpdateStatusBar() throws Exception {
        String fxUtils = Files.readString(Path.of(
            "src/main/java/com/cashier/util/FXUtils.java"
        ));

        assertTrue(fxUtils.contains("StatusBarManager.updateStatus(message)"));
        assertTrue(fxUtils.contains("Alert.AlertType.INFORMATION"));
    }

    @Test
    @DisplayName("密码重置页内嵌提示应同步成功和错误状态")
    void passwordResetInlineMessagesUpdateStatusBar() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PasswordResetController.java"
        ));

        assertTrue(controller.contains("StatusBarManager.updateError(message)"));
        assertTrue(controller.contains("StatusBarManager.updateSuccess(message)"));
    }

    @Test
    @DisplayName("创建退货订单弹窗应按提示类型同步状态栏")
    void createReturnOrderDialogAlertsUpdateStatusSeverity() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CreateReturnOrderDialogController.java"
        ));

        assertTrue(controller.contains("updateStatusForAlert(Alert.AlertType type, String message)"));
        assertTrue(controller.contains("StatusBarManager.updateError(message)"));
        assertTrue(controller.contains("StatusBarManager.updateWarning(message)"));
        assertTrue(controller.contains("StatusBarManager.updateSuccess(message)"));
    }

    @Test
    @DisplayName("POS 模式退出前的购物车警告应同步状态栏")
    void posModeCartExitWarningUpdatesStatusBar() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PosModeController.java"
        ));

        assertTrue(controller.contains("StatusBarManager.updateWarning(message)"));
        assertTrue(controller.contains("runtime.cart_exit_confirm"));
    }
}
