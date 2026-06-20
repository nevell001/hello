package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthControllerTest {

    @Test
    @DisplayName("基础健康检查只返回非敏感服务状态")
    void basicHealthCheckIsMinimal() {
        TestContext ctx = new TestContext();

        HealthController.check(ctx.context);

        Map<?, ?> response = (Map<?, ?>) ctx.json;
        assertEquals("ok", response.get("status"));
        assertEquals("cashier-api", response.get("service"));
        assertNotNull(response.get("timestamp"));
        assertEquals(3, response.size());
    }
}
