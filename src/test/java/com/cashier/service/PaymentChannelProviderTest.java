package com.cashier.service;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;
import com.cashier.service.payment.MockPaymentChannelProvider;
import com.cashier.service.payment.UnavailablePaymentChannelProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentChannelProviderTest {

    @AfterEach
    void restoreDisabledConfig() {
        PaymentService.setConfig(new PaymentService.PaymentConfig());
    }

    @Test
    @DisplayName("电子支付默认保持禁用")
    void electronicPaymentIsDisabledByDefault() {
        PaymentService.setConfig(new PaymentService.PaymentConfig());

        assertFalse(PaymentService.isChannelAvailable(PaymentOrder.PaymentChannel.WECHAT));
        assertFalse(PaymentService.isChannelAvailable(PaymentOrder.PaymentChannel.ALIPAY));
        assertThrows(IllegalStateException.class, () -> PaymentService.createPaymentOrder(
            "T-1", BigDecimal.TEN, PaymentOrder.PaymentChannel.WECHAT, "POS-1"));
    }

    @Test
    @DisplayName("模拟渠道必须同时显式开启模式和渠道")
    void mockProviderRequiresExplicitOptIn() {
        PaymentService.PaymentConfig config = new PaymentService.PaymentConfig();
        config.mode = "mock";
        config.mockEnabled = true;
        config.mockCallbackSecret = "test-secret";
        config.wechatEnabled = true;
        PaymentService.setConfig(config);

        assertTrue(PaymentService.isChannelAvailable(PaymentOrder.PaymentChannel.WECHAT));
        assertFalse(PaymentService.isChannelAvailable(PaymentOrder.PaymentChannel.ALIPAY));
    }

    @Test
    @DisplayName("模拟回调也必须校验测试密钥")
    void mockNotificationRequiresSecret() {
        MockPaymentChannelProvider provider = new MockPaymentChannelProvider(
            PaymentOrder.PaymentChannel.WECHAT, "test-secret");

        assertFalse(provider.verifyNotification(Map.of("mock_signature", "wrong")));
        assertTrue(provider.verifyNotification(Map.of("mock_signature", "test-secret")));
    }

    @Test
    @DisplayName("未配置渠道不会生成二维码或伪造退款成功")
    void unavailableProviderFailsClosed() {
        UnavailablePaymentChannelProvider provider = new UnavailablePaymentChannelProvider(
            PaymentOrder.PaymentChannel.ALIPAY, "未配置");
        PaymentOrder order = PaymentOrder.createScanPayOrder(
            "T-2", BigDecimal.TEN, PaymentOrder.PaymentChannel.ALIPAY, "POS-1");
        RefundRecord refund = RefundRecord.create("P-1", BigDecimal.ONE, "测试", "admin");

        assertThrows(IllegalStateException.class, () -> provider.createOrder(order));
        assertThrows(IllegalStateException.class, () -> provider.refund(order, refund));
        assertEquals(RefundRecord.RefundStatus.APPLYING, refund.status);
    }
}
