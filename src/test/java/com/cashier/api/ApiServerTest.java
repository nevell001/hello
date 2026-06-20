package com.cashier.api;

import com.cashier.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiServerTest {

    @Test
    @DisplayName("生成不可预测的 URL 安全 Token")
    void testGenerateSecureToken() {
        ApiServer apiServer = ApiServer.getInstance();
        User user = new User();
        user.id = 42;

        Set<String> generatedTokens = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            String token = apiServer.generateToken(user);

            assertTrue(token.matches("[A-Za-z0-9_-]{43}\\.[A-Za-z0-9_-]{43}"),
                "Token 应为 32 字节随机数加 HMAC 签名的 Base64URL 格式");
            assertFalse(token.startsWith("TK"), "Token 不应使用可预测前缀");
            assertTrue(generatedTokens.add(token), "Token 不应重复");
        }
    }

    @Test
    @DisplayName("空、篡改和已注销 Token 均无效")
    void invalidTokensAreRejected() {
        ApiServer apiServer = ApiServer.getInstance();
        User user = new User();
        user.id = 42;
        String token = apiServer.generateToken(user);

        assertNull(apiServer.validateToken(null));
        assertNull(apiServer.validateToken("  "));
        assertNull(apiServer.validateToken(token + "tampered"));

        apiServer.invalidateToken(token);
        assertNull(apiServer.validateToken(token));
    }

    @Test
    @DisplayName("只有基础健康检查和登录接口公开")
    void publicRouteBoundaryIsMinimal() {
        assertTrue(ApiServer.isPublicApiPath("/api/health"));
        assertTrue(ApiServer.isPublicApiPath("/api/auth/login"));
        assertFalse(ApiServer.isPublicApiPath("/api/health/detail"));
        assertFalse(ApiServer.isPublicApiPath("/api/auth/refresh"));
        assertFalse(ApiServer.isPublicApiPath("/api/products"));
    }
}
