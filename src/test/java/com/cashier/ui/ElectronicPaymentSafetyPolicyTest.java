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

    @Test
    @DisplayName("系统设置应提供微信支付宝接入配置入口")
    void settingsExposeElectronicPaymentConfiguration() throws Exception {
        String settingsView = Files.readString(Path.of(
            "src/main/resources/com/cashier/view/SettingsView.fxml"
        ));
        String settingsController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/SettingsController.java"
        ));

        assertTrue(settingsView.contains("<Tab text=\"%settings.tab.payment\">"));
        assertTrue(settingsView.contains("fx:id=\"wechatEnabledCheckBox\""));
        assertTrue(settingsView.contains("fx:id=\"wechatAppIdField\""));
        assertTrue(settingsView.contains("fx:id=\"wechatMchIdField\""));
        assertTrue(settingsView.contains("fx:id=\"wechatApiKeyField\""));
        assertTrue(settingsView.contains("fx:id=\"wechatPrivateKeyPathField\""));
        assertTrue(settingsView.contains("fx:id=\"wechatMerchantSerialNoField\""));
        assertTrue(settingsView.contains("fx:id=\"alipayEnabledCheckBox\""));
        assertTrue(settingsView.contains("fx:id=\"alipayAppIdField\""));
        assertTrue(settingsView.contains("fx:id=\"alipayPrivateKeyArea\""));
        assertTrue(settingsView.contains("fx:id=\"alipayPublicKeyArea\""));
        assertTrue(settingsView.contains("fx:id=\"alipayGatewayField\""));
        assertTrue(settingsController.contains("PaymentService.saveConfig(paymentConfig)"));
    }

    @Test
    @DisplayName("API 支付配置更新也必须持久化到配置文件")
    void paymentApiConfigUpdatePersistsConfig() throws Exception {
        String apiController = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/PaymentApiController.java"
        ));

        assertTrue(apiController.contains("PaymentService.saveConfig(config)"));
        assertFalse(apiController.contains("PaymentService.setConfig(config);"));
    }

    @Test
    @DisplayName("支付查单和回调应保留真实渠道详情")
    void paymentStatusAndNotifyKeepProviderDetails() throws Exception {
        String paymentService = Files.readString(Path.of(
            "src/main/java/com/cashier/service/PaymentService.java"
        ));
        String apiController = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/PaymentApiController.java"
        ));
        String wechatProvider = Files.readString(Path.of(
            "src/main/java/com/cashier/service/payment/WechatNativePaymentProvider.java"
        ));

        assertTrue(paymentService.contains("PaymentDAO.updatePaymentSuccess("));
        assertTrue(paymentService.contains("order.channelTransactionId"));
        assertTrue(paymentService.contains("order.channelUserId"));
        assertTrue(apiController.contains("params.forEach("));
        assertTrue(wechatProvider.contains("decryptAes256Gcm("));
        assertTrue(wechatProvider.contains("loadPublicKeyFromCertificateOrPem("));
        assertTrue(apiController.contains("\"code\", \"SUCCESS\""));
        assertFalse(apiController.contains("extractXmlValue("));
        assertFalse(apiController.contains("total_fee"));
        assertFalse(apiController.contains("<xml><return_code>"));
    }
}
