package com.cashier.api.middleware;

import com.cashier.api.support.TestContext;
import com.cashier.model.User;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationMiddlewareTest {

    @Test
    @DisplayName("管理员可以访问所有 API")
    void adminCanAccessAllApis() {
        assertTrue(AuthorizationMiddleware.isAllowed("管理员", "POST", "/api/backup/restore"));
        assertTrue(AuthorizationMiddleware.isAllowed("管理员", "PUT", "/api/settings/theme"));
    }

    @Test
    @DisplayName("收银员不能操作系统、备份和资金审核接口")
    void cashierCannotAccessPrivilegedApis() {
        assertFalse(AuthorizationMiddleware.isAllowed("收银员", "PUT", "/api/settings/theme"));
        assertFalse(AuthorizationMiddleware.isAllowed("收银员", "POST", "/api/backup/execute"));
        assertFalse(AuthorizationMiddleware.isAllowed("收银员", "PUT", "/api/payment/config"));
        assertFalse(AuthorizationMiddleware.isAllowed("收银员", "POST", "/api/transactions/T1/refund"));
        assertFalse(AuthorizationMiddleware.isAllowed("收银员", "POST", "/api/payment/P1/refund"));
        assertFalse(AuthorizationMiddleware.isAllowed("收银员", "POST", "/api/printers/P1/cashdrawer"));
        assertFalse(AuthorizationMiddleware.isAllowed("收银员", "DELETE", "/api/products/10"));
        assertFalse(AuthorizationMiddleware.isAllowed("收银员", "PUT", "/api/inventory/10"));
        assertFalse(AuthorizationMiddleware.isAllowed("收银员", "POST", "/api/invoices/manual"));
    }

    @Test
    @DisplayName("财务可以审核资金操作但不能修改系统配置")
    void financeHasFinancialPermissionsOnly() {
        assertTrue(AuthorizationMiddleware.isAllowed("财务", "POST", "/api/transactions/T1/refund"));
        assertTrue(AuthorizationMiddleware.isAllowed("财务", "POST", "/api/payment/P1/refund"));
        assertTrue(AuthorizationMiddleware.isAllowed("财务", "POST", "/api/invoices/manual"));
        assertTrue(AuthorizationMiddleware.isAllowed("财务", "GET", "/api/reports/daily"));
        assertFalse(AuthorizationMiddleware.isAllowed("财务", "PUT", "/api/settings/theme"));
        assertFalse(AuthorizationMiddleware.isAllowed("财务", "POST", "/api/backup/execute"));
    }

    @Test
    @DisplayName("收银员保留日常收银和打印小票能力")
    void cashierRetainsPosPermissions() {
        assertTrue(AuthorizationMiddleware.isAllowed("收银员", "POST", "/api/transactions"));
        assertTrue(AuthorizationMiddleware.isAllowed("收银员", "POST", "/api/payment/create"));
        assertTrue(AuthorizationMiddleware.isAllowed("收银员", "POST", "/api/printers/P1/receipt"));
        assertTrue(AuthorizationMiddleware.isAllowed("收银员", "GET", "/api/products"));
        assertTrue(AuthorizationMiddleware.isAllowed("收银员", "GET", "/api/inventory"));
    }

    @Test
    @DisplayName("中间件拒绝越权请求并停止后续处理")
    void middlewareRejectsUnauthorizedRequest() {
        User cashier = new User();
        cashier.role = "收银员";
        TestContext ctx = new TestContext()
            .withAttribute("currentUser", cashier)
            .withRequest(HandlerType.DELETE, "/api/products/10");

        AuthorizationMiddleware.authorize(ctx.context);

        assertEquals(HttpStatus.FORBIDDEN, ctx.status);
        assertTrue(ctx.skipped);
    }
}
