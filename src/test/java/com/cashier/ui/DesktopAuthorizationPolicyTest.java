package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopAuthorizationPolicyTest {

    @Test
    @DisplayName("主界面敏感入口必须进行权限拦截")
    void mainSensitiveEntrypointsRequirePermission() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MainController.java"
        ));

        assertGuarded(source, "handleDataBackup", "PERMISSION_BACKUP_RESTORE");
        assertGuarded(source, "handleDataRestore", "PERMISSION_BACKUP_RESTORE");
        assertGuarded(source, "handlePurchaseApproval", "PERMISSION_MANAGE_PURCHASE");
        assertGuarded(source, "handlePurchaseInbound", "PERMISSION_MANAGE_PURCHASE");
        assertGuarded(source, "handleInventoryCheck", "PERMISSION_MANAGE_INVENTORY");
        assertGuarded(source, "handleUserManagement", "PERMISSION_MANAGE_USERS");
        assertGuarded(source, "handleSettings", "PERMISSION_MANAGE_SETTINGS");
        assertGuarded(source, "handleAuditLogs", "PERMISSION_VIEW_AUDIT");
        assertGuarded(source, "handleReturnApproval", "PERMISSION_APPROVE_RETURNS");
    }

    @Test
    @DisplayName("商品查询页面的写操作必须单独校验库存管理权限")
    void inventoryWriteOperationsRequireManagePermission() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryController.java"
        ));

        assertTrue(source.contains("requireInventoryManagement()"));
        assertTrue(source.contains("User.PERMISSION_MANAGE_INVENTORY"));
        assertTrue(source.contains("button.setVisible(canManage)"));
    }

    private void assertGuarded(String source, String method, String permission) {
        int methodStart = source.indexOf("void " + method + "()");
        assertTrue(methodStart >= 0, "未找到入口: " + method);
        int nextMethod = source.indexOf("\n    @FXML", methodStart + 1);
        String body = source.substring(methodStart, nextMethod >= 0 ? nextMethod : source.length());
        assertTrue(body.contains("requirePermission(User." + permission + ")"),
            method + " 缺少权限校验");
    }
}
