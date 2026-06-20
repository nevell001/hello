package com.cashier.api.middleware;

import com.cashier.api.support.TestContext;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthMiddlewareTest {

    @Test
    @DisplayName("缺少 Bearer Token 时返回 401 并停止请求")
    void missingTokenIsRejected() {
        TestContext ctx = new TestContext();

        AuthMiddleware.authenticate(ctx.context);

        assertEquals(HttpStatus.UNAUTHORIZED, ctx.status);
        assertTrue(ctx.skipped);
    }

    @Test
    @DisplayName("未知 Token 不能通过认证")
    void unknownTokenIsRejected() {
        TestContext ctx = new TestContext()
            .withHeader("Authorization", "Bearer unknown-token");

        AuthMiddleware.authenticate(ctx.context);

        assertEquals(HttpStatus.UNAUTHORIZED, ctx.status);
        assertTrue(ctx.skipped);
    }
}
