package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseAndInventoryCheckSelectionFeedbackPolicyTest {

    @Test
    @DisplayName("采购页面空选择操作应提示先选择采购订单")
    void purchaseActionsWarnWhenNoOrderSelected() throws Exception {
        List<String> purchaseControllers = List.of(
            "src/main/java/com/cashier/controller/PurchaseOrderController.java",
            "src/main/java/com/cashier/controller/PurchaseApprovalController.java",
            "src/main/java/com/cashier/controller/PurchaseInboundController.java"
        );

        for (String file : purchaseControllers) {
            String controller = Files.readString(Path.of(file));
            assertTrue(controller.contains("showWarning(I18nManager.getInstance().get(\"runtime.select_purchase_order\"))"),
                file + " 的空选择操作应提示先选择采购订单");
            assertTrue(controller.contains("FXUtils.showWarningAlert"),
                file + " 应使用警告弹窗同步状态栏");
        }
    }

    @Test
    @DisplayName("库存盘点空选择操作应提示先选择盘点单")
    void inventoryCheckActionsWarnWhenNoCheckSelected() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryCheckController.java"
        ));

        assertTrue(controller.contains("showWarning(I18nManager.getInstance().get(\"runtime.select_inventory_check\"))"));
        assertTrue(controller.contains("FXUtils.showWarningAlert"));
    }

    @Test
    @DisplayName("采购和盘点空选择提示应具备完整国际化文案")
    void selectionWarningMessagesAreLocalized() throws Exception {
        List<String> bundleFiles = List.of(
            "src/main/resources/com/cashier/i18n/messages.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_CN.properties",
            "src/main/resources/com/cashier/i18n/messages_en.properties",
            "src/main/resources/com/cashier/i18n/messages_zh_TW.properties"
        );

        for (String file : bundleFiles) {
            String bundle = Files.readString(Path.of(file));
            assertTrue(bundle.contains("runtime.select_purchase_order="), file);
            assertTrue(bundle.contains("runtime.select_inventory_check="), file);
        }
    }

    @Test
    @DisplayName("入库历史详情关闭按钮只关闭详情窗口")
    void inboundHistoryDetailCloseButtonDoesNotCloseHistoryWindow() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseInboundController.java"
        ));

        int methodStart = controller.indexOf("private void showInboundDetailDialog(PurchaseInbound inbound, Stage parentStage)");
        assertTrue(methodStart >= 0, "未找到入库详情窗口方法");
        int methodEnd = controller.indexOf("\n    private void showError", methodStart);
        String methodBody = controller.substring(methodStart, methodEnd);

        assertTrue(methodBody.contains("detailStage.initOwner(parentStage)"));
        assertTrue(methodBody.contains("closeButton.setOnAction(e -> detailStage.close())"));
        assertFalse(methodBody.contains("closeButton.setOnAction(e -> parentStage.close())"));
    }

    @Test
    @DisplayName("采购订单状态筛选应使用状态码并按当前语言显示")
    void purchaseOrderStatusFilterUsesLocalizedCodes() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseOrderController.java"
        ));

        assertTrue(controller.contains("statusFilterCombo.getItems().addAll(\"all\", \"pending\", \"approved\", \"rejected\", \"completed\")"));
        assertTrue(controller.contains("I18nUiUtils.configureComboBox(statusFilterCombo"));
        assertTrue(controller.contains("I18nUiUtils.purchaseStatus(value)"));
        assertTrue(controller.contains("statusFilterCombo.setValue(\"all\")"));
        assertTrue(controller.contains("return statusFilter != null && statusFilter.equals(order.status);"));

        int initializeStart = controller.indexOf("private void initialize()");
        int setupTableStart = controller.indexOf("private void setupTableColumns()", initializeStart);
        String initializeBody = controller.substring(initializeStart, setupTableStart);
        int filterStart = controller.indexOf("private void filterOrders()");
        int countStart = controller.indexOf("private void updateCountLabel()", filterStart);
        String filterBody = controller.substring(filterStart, countStart);

        assertFalse(initializeBody.contains("statusFilterCombo.getItems().addAll(\"全部\""));
        assertFalse(filterBody.contains("case \"待审批\""));
    }
}
