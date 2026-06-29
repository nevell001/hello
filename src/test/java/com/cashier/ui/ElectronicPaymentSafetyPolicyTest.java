package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectronicPaymentSafetyPolicyTest {

    @Test
    @DisplayName("桌面收银不得把微信支付宝直接记为支付成功")
    void desktopCheckoutDoesNotDirectlySettleElectronicPayment() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));

        assertFalse(source.contains("handlePayment(\"微信\")"));
        assertFalse(source.contains("handlePayment(\"支付宝\")"));
        assertTrue(source.contains("startElectronicPayment(PaymentOrder.PaymentChannel.WECHAT"));
        assertTrue(source.contains("startElectronicPayment(PaymentOrder.PaymentChannel.ALIPAY"));
        assertTrue(source.contains("PaymentService.queryPaymentStatus("));
        assertTrue(source.contains("latest.status == PaymentOrder.PaymentStatus.SUCCESS"));
        assertTrue(source.contains("PaymentService.cancelPaymentOrder("));
    }

    @Test
    @DisplayName("默认支付配置不得包含模拟商户凭据")
    void defaultPaymentConfigIsFailClosed() throws Exception {
        String config = Files.readString(Path.of("config/payment.properties"));

        assertTrue(config.contains("payment.mode=disabled"));
        assertTrue(config.contains("payment.mock.enabled=false"));
        assertTrue(config.contains("wechat.enabled=false"));
        assertTrue(config.contains("alipay.enabled=false"));
        assertFalse(config.contains("wxmock"));
    }
}
