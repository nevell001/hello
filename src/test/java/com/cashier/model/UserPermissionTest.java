package com.cashier.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPermissionTest {

    @Test
    @DisplayName("管理员拥有全部桌面业务权限")
    void adminHasAllBusinessPermissions() {
        User admin = user("admin");

        assertTrue(admin.hasPermission(User.PERMISSION_CHECKOUT));
        assertTrue(admin.hasPermission(User.PERMISSION_MANAGE_PURCHASE));
        assertTrue(admin.hasPermission(User.PERMISSION_MANAGE_INVENTORY));
        assertTrue(admin.hasPermission(User.PERMISSION_MANAGE_USERS));
        assertTrue(admin.hasPermission(User.PERMISSION_VIEW_AUDIT));
        assertTrue(admin.hasPermission(User.PERMISSION_BACKUP_RESTORE));
    }

    @Test
    @DisplayName("收银员仅能处理前台收银相关业务")
    void cashierIsLimitedToFrontDeskOperations() {
        User cashier = user("cashier");

        assertTrue(cashier.hasPermission(User.PERMISSION_CHECKOUT));
        assertTrue(cashier.hasPermission(User.PERMISSION_VIEW_INVENTORY));
        assertTrue(cashier.hasPermission(User.PERMISSION_VIEW_TRANSACTIONS));
        assertTrue(cashier.hasPermission(User.PERMISSION_MANAGE_MEMBERS));
        assertTrue(cashier.hasPermission(User.PERMISSION_MANAGE_RETURNS));
        assertTrue(cashier.hasPermission(User.PERMISSION_MANAGE_SHIFT));
        assertFalse(cashier.hasPermission(User.PERMISSION_MANAGE_INVENTORY));
        assertFalse(cashier.hasPermission(User.PERMISSION_MANAGE_PURCHASE));
        assertFalse(cashier.hasPermission(User.PERMISSION_APPROVE_RETURNS));
        assertFalse(cashier.hasPermission(User.PERMISSION_MANAGE_SETTINGS));
    }

    @Test
    @DisplayName("财务仅能查询交易报表并导出")
    void financeIsLimitedToFinancialReadOperations() {
        User finance = user("finance");

        assertTrue(finance.hasPermission(User.PERMISSION_VIEW_TRANSACTIONS));
        assertTrue(finance.hasPermission(User.PERMISSION_VIEW_REPORTS));
        assertTrue(finance.hasPermission(User.PERMISSION_EXPORT_DATA));
        assertFalse(finance.hasPermission(User.PERMISSION_CHECKOUT));
        assertFalse(finance.hasPermission(User.PERMISSION_MANAGE_MEMBERS));
        assertFalse(finance.hasPermission(User.PERMISSION_MANAGE_PURCHASE));
        assertFalse(finance.hasPermission(User.PERMISSION_BACKUP_RESTORE));
    }

    @Test
    @DisplayName("停用账号不具备任何权限")
    void inactiveUserHasNoPermissions() {
        User admin = user("admin");
        admin.active = false;

        assertFalse(admin.hasPermission(User.PERMISSION_CHECKOUT));
        assertFalse(admin.hasPermission(User.PERMISSION_MANAGE_USERS));
    }

    private User user(String role) {
        User user = new User("tester", "", "测试用户", role);
        user.active = true;
        return user;
    }
}
