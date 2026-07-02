package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    @DisplayName("退货订单页创建退货说明不应写入底部状态栏")
    void returnOrderCreateHelpDoesNotUpdateStatusBar() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ReturnOrderController.java"
        ));

        int methodStart = controller.indexOf("public void handleCreateReturn()");
        assertTrue(methodStart >= 0, "未找到创建退货入口");
        int methodEnd = controller.indexOf("\n    @FXML", methodStart + 1);
        String methodBody = controller.substring(methodStart, methodEnd);

        assertTrue(methodBody.contains("showInformationOnlyAlert("));
        assertTrue(methodBody.contains("runtime.return_help"));
        assertFalse(methodBody.contains("showAlert("));
        assertFalse(methodBody.contains("updateStatusForAlert("));
    }

    @Test
    @DisplayName("退货订单页查看原交易详情不应写入底部状态栏")
    void returnOrderOriginalTransactionDetailsDoNotUpdateStatusBar() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ReturnOrderController.java"
        ));

        int methodStart = controller.indexOf("public void handleViewOriginalTransaction()");
        assertTrue(methodStart >= 0, "未找到查看原交易入口");
        int methodEnd = controller.indexOf("\n    @FXML", methodStart + 1);
        String methodBody = controller.substring(methodStart, methodEnd);

        assertTrue(methodBody.contains("showInformationOnlyAlert("));
        assertTrue(methodBody.contains("runtime.original_transaction_details"));
        assertFalse(methodBody.contains("showAlert(Alert.AlertType.INFORMATION"));
    }

    @Test
    @DisplayName("创建退货订单页面只保留整单退货原因入口")
    void createReturnOrderDialogKeepsSingleReturnReasonInput() throws Exception {
        String view = Files.readString(Path.of(
            "src/main/resources/com/cashier/view/CreateReturnOrderDialog.fxml"
        ));
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CreateReturnOrderDialogController.java"
        ));

        assertTrue(view.contains("fx:id=\"conditionColumn\" text=\"%return_order.condition\" minWidth=\"130\" prefWidth=\"140\""));
        assertTrue(view.contains("text=\"%return_order.return_reason_label\""));
        assertFalse(view.contains("fx:id=\"reasonColumn\""));
        assertFalse(view.contains("return_order.item_reason"));
        assertFalse(controller.contains("reasonColumn"));
        assertTrue(controller.contains("returnItem.reason = returnReason"));
        assertTrue(controller.contains("comboBox.setMinWidth(118)"));
        assertTrue(controller.contains("comboBox.setPrefWidth(128)"));
    }

    @Test
    @DisplayName("退货审批表格空状态提示应使用页面专属国际化文案")
    void returnApprovalTablePlaceholdersAreLocalized() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ReturnApprovalController.java"
        ));

        assertTrue(controller.contains("return_approval.pending_no_data"));
        assertTrue(controller.contains("return_approval.items_no_data"));
        assertFalse(controller.contains("pendingOrderTable.setPlaceholder(new Label(I18nManager.getInstance().get(\"message.data.empty\")))"));
        assertFalse(controller.contains("itemTable.setPlaceholder(new Label(I18nManager.getInstance().get(\"message.data.empty\")))"));

        List<String> bundleFiles = List.of(
            "src/main/resources/com/cashier/i18n/messages.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_CN.properties",
            "src/main/resources/com/cashier/i18n/messages_en.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_TW.properties"
        );

        for (String file : bundleFiles) {
            String bundle = Files.readString(Path.of(file));
            assertTrue(bundle.contains("return_approval.pending_no_data="), file);
            assertTrue(bundle.contains("return_approval.items_no_data="), file);
        }
    }
}
