package com.cashier.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditServiceTest {

    @Test
    void removesSensitiveValuesFromAuditDetails() {
        String sanitized = AuditService.sanitize(
            "username=admin password=secret token:abc123 Authorization=BearerValue"
        );

        assertFalse(sanitized.contains("secret"));
        assertFalse(sanitized.contains("abc123"));
        assertFalse(sanitized.contains("BearerValue"));
        assertTrue(sanitized.contains("[REDACTED]"));
    }
}
