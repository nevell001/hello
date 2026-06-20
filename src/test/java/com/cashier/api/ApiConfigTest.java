package com.cashier.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiConfigTest {

    @Test
    @DisplayName("API 安全配置要求强密钥和明确 CORS 来源")
    void secureConfigurationRequiresStrongSecretAndRestrictedCors() {
        String strongSecret = "0123456789abcdef0123456789abcdef";

        assertTrue(ApiConfig.isSecurityConfigurationValid(strongSecret, "https://pos.example.com"));
        assertFalse(ApiConfig.isSecurityConfigurationValid("short", "https://pos.example.com"));
        assertFalse(ApiConfig.isSecurityConfigurationValid("default_secret_key", "https://pos.example.com"));
        assertFalse(ApiConfig.isSecurityConfigurationValid(strongSecret, "*"));
        assertFalse(ApiConfig.isSecurityConfigurationValid(strongSecret, "https://*.example.com"));
        assertFalse(ApiConfig.isSecurityConfigurationValid(strongSecret, ""));
        assertFalse(ApiConfig.isSecurityConfigurationValid(null, "https://pos.example.com"));
        assertFalse(ApiConfig.isSecurityConfigurationValid(strongSecret, null));
    }
}
