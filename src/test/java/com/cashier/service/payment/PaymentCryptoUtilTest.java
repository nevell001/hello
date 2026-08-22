package com.cashier.service.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCryptoUtilTest {

    @Test
    @DisplayName("空白判断与URL编码")
    void blankAndUrlEncode() {
        assertTrue(PaymentCryptoUtil.isBlank(null));
        assertTrue(PaymentCryptoUtil.isBlank("   "));
        assertFalse(PaymentCryptoUtil.isBlank("abc"));

        assertEquals("a%2Bb", PaymentCryptoUtil.urlEncode("a+b"));
        assertEquals("", PaymentCryptoUtil.urlEncode(null));
    }

    @Test
    @DisplayName("RSA 签名验签往返")
    void signAndVerifyRoundTrip() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        String content = "timestamp\nnonce\nbody";
        String signature = PaymentCryptoUtil.signSha256WithRsa(content, privateKey);

        assertTrue(PaymentCryptoUtil.verifySha256WithRsa(content, signature, publicKey));
    }

    @Test
    @DisplayName("篡改内容或签名必须验签失败")
    void tamperedSignatureRejected() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        String content = "timestamp\nnonce\nbody";
        String signature = PaymentCryptoUtil.signSha256WithRsa(content, keyPair.getPrivate());

        assertFalse(PaymentCryptoUtil.verifySha256WithRsa(content + "x", signature, keyPair.getPublic()));
        assertFalse(PaymentCryptoUtil.verifySha256WithRsa(content, "bm90LWEtdmFsaWQtc2lnbmF0dXJl", keyPair.getPublic()));
    }

    @Test
    @DisplayName("无效签名文本返回 false 而非抛异常")
    void invalidSignatureTextReturnsFalse() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        assertFalse(PaymentCryptoUtil.verifySha256WithRsa(
            "content", "!!!not-base64!!!", keyPair.getPublic()));
    }

}
