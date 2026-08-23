package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HealthControllerTest extends DatabaseTestBase {

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

    @Test
    @DisplayName("详细健康检查报告数据库与内存状态")
    void detailHealthCheckReportsDatabase() throws Exception {
        initTestDatabase();
        TestContext ctx = new TestContext();

        HealthController.detail(ctx.context);

        Map<?, ?> response = (Map<?, ?>) ctx.json;
        assertEquals("connected", response.get("database"));
        assertNotNull(response.get("memory_used_mb"));
        assertNotNull(response.get("memory_max_mb"));
    }
}
