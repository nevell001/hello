package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTransactionReturnFeedbackPolicyTest {

    @Test
    @DisplayName("会员页面空选择操作应提示先选择会员")
    void memberActionsWarnWhenNoMemberSelected() throws Exception {
        String memberController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MemberController.java"
        ));

        assertTrue(memberController.contains("showWarning(i18n.get(\"runtime.select_member\"))"));
        assertTrue(memberController.contains("if (selected.isEmpty())"));
        assertTrue(memberController.contains("Member selected = getSelectedItem(memberTable);"));
    }

    @Test
    @DisplayName("交易和退货自定义弹窗应同步状态栏级别")
    void transactionAndReturnAlertsUpdateStatusSeverity() throws Exception {
        List<String> controllerFiles = List.of(
            "src/main/java/com/cashier/controller/TransactionController.java",
            "src/main/java/com/cashier/controller/ReturnOrderController.java",
            "src/main/java/com/cashier/controller/ReturnApprovalController.java",
            "src/main/java/com/cashier/controller/ReturnReportController.java"
        );

        for (String file : controllerFiles) {
            String controller = Files.readString(Path.of(file));
            assertTrue(controller.contains("updateStatusForAlert(Alert.AlertType type, String message)"), file);
            assertTrue(controller.contains("StatusBarManager.updateError(message)"), file);
            assertTrue(controller.contains("StatusBarManager.updateWarning(message)"), file);
            assertTrue(controller.contains("StatusBarManager.updateSuccess(message)"), file);
        }
    }

    @Test
    @DisplayName("会员空选择提示应具备完整国际化文案")
    void memberSelectionWarningIsLocalized() throws Exception {
        List<String> bundleFiles = List.of(
            "src/main/resources/com/cashier/i18n/messages.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_CN.properties",
            "src/main/resources/com/cashier/i18n/messages_en.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_TW.properties"
        );

        for (String file : bundleFiles) {
            String bundle = Files.readString(Path.of(file));
            assertTrue(bundle.contains("runtime.select_member="), file);
        }
    }
}
