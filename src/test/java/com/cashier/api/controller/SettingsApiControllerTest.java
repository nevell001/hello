package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsApiControllerTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("获取全部设置返回成功")
    void listReturnsSuccess() {
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/settings");
        SettingsApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("不存在的设置项返回 404")
    void getMissingKeyReturns404() {
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/settings/x")
            .withPathParam("key", "definitely-not-exists");
        SettingsApiController.get(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("写入并读取设置项")
    void setAndGetRoundTrip() {
        TestContext setCtx = new TestContext().withRequest(HandlerType.PUT, "/api/settings/t.theme")
            .withPathParam("key", "t.theme")
            .withBody(Map.of("value", "dark"));
        SettingsApiController.set(setCtx.context);

        assertEquals(HttpStatus.OK, setCtx.status);
        assertEquals("dark", response(setCtx).get("value"));

        TestContext getCtx = new TestContext().withRequest(HandlerType.GET, "/api/settings/t.theme")
            .withPathParam("key", "t.theme");
        SettingsApiController.get(getCtx.context);

        assertEquals(HttpStatus.OK, getCtx.status);
        assertEquals("dark", response(getCtx).get("value"));
    }

    @Test
    @DisplayName("缺少 value 参数返回 400")
    void setMissingValueReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/settings/t.k")
            .withPathParam("key", "t.k")
            .withBody(Map.of("other", "x"));
        SettingsApiController.set(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体设置返回 400")
    void setNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/settings/t.k2")
            .withPathParam("key", "t.k2");
        SettingsApiController.set(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("删除已存在的设置项")
    void deleteExistingKey() {
        TestContext setCtx = new TestContext().withRequest(HandlerType.PUT, "/api/settings/t.del")
            .withPathParam("key", "t.del")
            .withBody(Map.of("value", "v"));
        SettingsApiController.set(setCtx.context);

        TestContext delCtx = new TestContext().withRequest(HandlerType.DELETE, "/api/settings/t.del")
            .withPathParam("key", "t.del");
        SettingsApiController.delete(delCtx.context);

        assertEquals(HttpStatus.OK, delCtx.status);
        assertTrue((Boolean) response(delCtx).get("success"));
    }
}
