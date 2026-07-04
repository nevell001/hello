package com.cashier.model;

import java.time.Instant;

public class OperationLog {
    public String logId;         // 日志ID
    public String username;      // 用户名
    public String operation;     // 操作类型
    public String details;       // 操作详情
    public Instant timestamp;       // 操作时间
    public String ipAddress;     // IP地址（预留）
    public String logLevel;      // INFO/WARN/ERROR
    public String category;      // AUTH/TRANSACTION/INVENTORY等
    public String result;        // SUCCESS/FAILURE
    public int affectedRecords;  // 影响记录数

    public OperationLog() {
        this.logId = "";
        this.username = "";
        this.operation = "";
        this.details = "";
        this.timestamp = Instant.now();
        this.ipAddress = "";
        this.logLevel = "INFO";
        this.category = "SYSTEM";
        this.result = "SUCCESS";
        this.affectedRecords = 0;
    }

    public OperationLog(String logId, String username, String operation, String details) {
        this.logId = logId;
        this.username = username;
        this.operation = operation;
        this.details = details;
        this.timestamp = Instant.now();
        this.ipAddress = "";
    }

    // Getter方法
    public String getLogId() {
        return logId;
    }

    public String getUsername() {
        return username;
    }

    public String getOperation() {
        return operation;
    }

    public String getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public String getCategory() {
        return category;
    }

    public String getResult() {
        return result;
    }

    public int getAffectedRecords() {
        return affectedRecords;
    }
}
