package com.cashier.api.middleware;

import com.cashier.model.User;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Map;

/**
 * REST API 角色授权中间件。
 *
 * 认证只证明调用者是谁；这里集中限制会改变系统配置、备份或资金状态的接口，
 * 避免各控制器自行检查时出现遗漏。
 */
public final class AuthorizationMiddleware {
    private static final String ADMIN = "管理员";
    private static final String FINANCE = "财务";

    private AuthorizationMiddleware() {
    }

    public static void authorize(Context ctx) {
        User user = ctx.attribute("currentUser");
        if (user == null || !isAllowed(user.role, ctx.method().name(), ctx.path())) {
            ctx.status(HttpStatus.FORBIDDEN)
                .json(Map.of("success", false, "message", "权限不足"));
            ctx.skipRemainingHandlers();
        }
    }

    static boolean isAllowed(String role, String method, String path) {
        if (ADMIN.equals(role)) {
            return true;
        }

        if (isAdminOnlyPath(method, path)) {
            return false;
        }

        if (isFinanceOrAdminPath(method, path)) {
            return FINANCE.equals(role);
        }

        return true;
    }

    private static boolean isAdminOnlyPath(String method, String path) {
        return path.startsWith("/api/users")
            || path.startsWith("/api/settings")
            || path.startsWith("/api/backup")
            || path.equals("/api/payment/config")
            || (path.startsWith("/api/products") && isMutating(method))
            || (path.startsWith("/api/inventory") && isMutating(method))
            || (path.startsWith("/api/printers/") && isMutating(method)
                && !path.matches("/api/printers/[^/]+/(receipt|invoice/[^/]+)"));
    }

    private static boolean isFinanceOrAdminPath(String method, String path) {
        return path.matches("/api/transactions/[^/]+/refund")
            || path.matches("/api/payment/[^/]+/refund")
            || path.matches("/api/invoices/[^/]+/void")
            || path.equals("/api/invoices/manual")
            || (path.startsWith("/api/reports/") && "GET".equals(method));
    }

    private static boolean isMutating(String method) {
        return "POST".equals(method) || "PUT".equals(method)
            || "PATCH".equals(method) || "DELETE".equals(method);
    }
}
