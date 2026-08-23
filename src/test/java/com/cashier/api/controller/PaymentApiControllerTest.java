package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.service.PaymentService;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentApiControllerTest extends DatabaseTestBase {

    @AfterEach
    void restoreDisabledConfig() {
        PaymentService.setConfig(new PaymentService.PaymentConfig());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("缺少必填参数返回 400")
    void createPaymentMissingParamsReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/payment/create")
            .withBody(Map.of("channel", "WECHAT"));

        PaymentApiController.createPayment(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体创建支付返回 400")
    void createPaymentWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/payment/create");

        PaymentApiController.createPayment(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("支付默认禁用时创建订单返回 500 且不泄露内部细节")
    void createPaymentWhenDisabledFails() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/payment/create")
            .withBody(Map.of("transactionId", "T-1", "amount", 100.0, "channel", "WECHAT", "terminalId", "POS-1"));

        PaymentApiController.createPayment(ctx.context);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ctx.status);
        assertTrue(((String) response(ctx).get("error")).contains("创建失败"));
    }

    @Test
    @DisplayName("显式开启 mock 模式后创建订单成功")
    void createPaymentInMockModeSucceeds() {
        try {
            DAOFactory.getInstance().getPaymentDAO().createTable();
        } catch (Exception e) {
            throw new IllegalStateException("初始化支付表失败", e);
        }
        PaymentService.PaymentConfig config = new PaymentService.PaymentConfig();
        config.mode = "mock";
        config.mockEnabled = true;
        config.mockCallbackSecret = "test-secret";
        config.wechatEnabled = true;
        PaymentService.setConfig(config);

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/payment/create")
            .withBody(Map.of("transactionId", "T-2", "amount", 88.0, "channel", "WECHAT", "terminalId", "POS-1"));

        PaymentApiController.createPayment(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }
}
