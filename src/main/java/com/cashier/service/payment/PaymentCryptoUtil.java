package com.cashier.service.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

final class PaymentCryptoUtil {
    private PaymentCryptoUtil() {
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    static String signSha256WithRsa(String content, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("支付签名失败", e);
        }
    }

    static boolean verifySha256WithRsa(String content, String signatureText, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureText));
        } catch (Exception e) {
            return false;
        }
    }

    static PrivateKey loadPrivateKeyFromPem(String pemOrPath) {
        try {
            String content = Files.exists(Path.of(pemOrPath)) ? Files.readString(Path.of(pemOrPath)) : pemOrPath;
            String normalized = normalizePem(content, "PRIVATE KEY");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(normalized));
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("读取支付私钥失败", e);
        }
    }

    static PublicKey loadPublicKeyFromPem(String pem) {
        try {
            String normalized = normalizePem(pem, "PUBLIC KEY");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(normalized));
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("读取支付公钥失败", e);
        }
    }

    static String buildAlipaySignContent(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        StringBuilder content = new StringBuilder();
        sorted.forEach((key, value) -> {
            if (!"sign".equals(key) && !"sign_type".equals(key) && value != null && !value.isBlank()) {
                if (content.length() > 0) {
                    content.append('&');
                }
                content.append(key).append('=').append(value);
            }
        });
        return content.toString();
    }

    private static String normalizePem(String pem, String label) {
        return pem
            .replace("-----BEGIN " + label + "-----", "")
            .replace("-----END " + label + "-----", "")
            .replaceAll("\\s", "");
    }
}
