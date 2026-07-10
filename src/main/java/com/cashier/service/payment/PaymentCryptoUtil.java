package com.cashier.service.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
            String content = readPathOrInline(pemOrPath);
            String normalized = normalizePem(content, "PRIVATE KEY");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(normalized));
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("读取支付私钥失败", e);
        }
    }

    static PublicKey loadPublicKeyFromPem(String pem) {
        try {
            String normalized = normalizePem(readPathOrInline(pem), "PUBLIC KEY");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(normalized));
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalStateException("读取支付公钥失败", e);
        }
    }

    static PublicKey loadPublicKeyFromCertificateOrPem(String certOrPemOrPath) {
        String content = readPathOrInline(certOrPemOrPath);
        if (content.contains("BEGIN CERTIFICATE")) {
            try {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                    new java.io.ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
                );
                return certificate.getPublicKey();
            } catch (Exception e) {
                throw new IllegalStateException("读取微信支付平台证书失败", e);
            }
        }
        return loadPublicKeyFromPem(content);
    }

    static String decryptAes256Gcm(String apiV3Key, String nonce, String associatedData, String ciphertext) {
        try {
            byte[] keyBytes = apiV3Key.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("微信 API v3 密钥长度必须为 32 字节");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            if (associatedData != null && !associatedData.isBlank()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("微信支付回调解密失败", e);
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

    private static String readPathOrInline(String value) {
        if (value == null) {
            return "";
        }
        try {
            Path path = Path.of(value);
            if (Files.exists(path)) {
                return Files.readString(path);
            }
        } catch (InvalidPathException e) {
            // Multi-line PEM content is not a filesystem path.
        } catch (Exception e) {
            throw new IllegalStateException("读取支付密钥文件失败", e);
        }
        return value;
    }
}
