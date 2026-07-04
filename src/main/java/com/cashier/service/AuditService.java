package com.cashier.service;

import com.cashier.dao.OperationLogDAO;
import com.cashier.model.OperationLog;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * 集中记录关键业务操作。审计写入失败不会中断主营业务。
 */
public final class AuditService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(AuditService.class);
    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
        "(?i)(password|token|secret|authorization|dbPassword)\\s*[:=]\\s*([^,;\\s]+)"
    );
    private static final int MAX_DETAILS_LENGTH = 2000;

    private AuditService() {
    }

    public static void success(String username, String category, String operation,
                               String details, int affectedRecords) {
        record(username, category, operation, "SUCCESS", "INFO", details, affectedRecords);
    }

    public static void failure(String username, String category, String operation, String details) {
        record(username, category, operation, "FAILURE", "WARN", details, 0);
    }

    public static void record(String username, String category, String operation, String result,
                              String level, String details, int affectedRecords) {
        OperationLog audit = new OperationLog();
        audit.username = normalizeUsername(username);
        audit.category = defaultValue(category, "SYSTEM");
        audit.operation = defaultValue(operation, "UNKNOWN");
        audit.result = defaultValue(result, "SUCCESS");
        audit.logLevel = defaultValue(level, "INFO");
        audit.details = sanitize(details);
        audit.affectedRecords = Math.max(affectedRecords, 0);
        audit.timestamp = Instant.now();
        audit.ipAddress = "local";

        try {
            OperationLogDAO.insert(audit);
        } catch (Exception e) {
            logger.warn("审计日志写入失败: category={}, operation={}, result={}",
                audit.category, audit.operation, audit.result, e);
        }
    }

    static String sanitize(String details) {
        if (details == null) {
            return "";
        }
        String sanitized = SENSITIVE_VALUE.matcher(details).replaceAll("$1=[REDACTED]");
        return sanitized.length() <= MAX_DETAILS_LENGTH
            ? sanitized
            : sanitized.substring(0, MAX_DETAILS_LENGTH);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String normalizeUsername(String value) {
        String username = blankToNull(value);
        return username != null && username.length() <= 50 ? username : null;
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
