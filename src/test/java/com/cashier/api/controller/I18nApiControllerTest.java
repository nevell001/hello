package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.i18n.I18nManager;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nApiControllerTest {

    @AfterEach
    void restoreLocale() {
        I18nManager.getInstance().setLocale("zh-CN");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("空请求体设置语言返回 400")
    void setLocaleWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/i18n/locale");
        I18nApiController.setLocale(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("缺少 locale 参数返回 400")
    void setLocaleMissingParamReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/i18n/locale")
            .withBody(Map.of("other", "x"));
        I18nApiController.setLocale(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("设置有效语言返回 200")
    void setLocaleValid() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/i18n/locale")
            .withBody(Map.of("locale", "zh-TW"));
        I18nApiController.setLocale(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }
}
