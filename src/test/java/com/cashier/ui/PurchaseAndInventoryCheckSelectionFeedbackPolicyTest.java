package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
}
