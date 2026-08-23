package com.cashier.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统设置数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class SystemSettingsDAORefactored extends BaseDAO {

    /**
     * 获取税率设置
     */
    public double getTaxRate() throws SQLException {
        String sql = "SELECT value FROM settings WHERE `key` = 'taxRate'";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("value");
                }
            }
        }
        return 0.0; // 默认税率
    }

    /**
     * 设置税率
     */
    public boolean setTaxRate(double taxRate) throws SQLException {
        return executeUpdate(
            "INSERT INTO settings (`key`, value) VALUES ('taxRate', ?) ON DUPLICATE KEY UPDATE value = ?",
            taxRate, taxRate) > 0;
    }

    /**
     * 获取交易计数
     */
    public int getTransactionCount() throws SQLException {
        String sql = "SELECT value FROM settings WHERE `key` = 'transactionCount'";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("value");
                }
            }
        }
        return 0; // 默认计数
    }

    /**
     * 设置交易计数
     */
    public boolean setTransactionCount(int count) throws SQLException {
        return executeUpdate(
            "INSERT INTO settings (`key`, value) VALUES ('transactionCount', ?) ON DUPLICATE KEY UPDATE value = ?",
            count, count) > 0;
    }

    /**
     * 增加交易计数
     */
    public boolean incrementTransactionCount() throws SQLException {
        String sql = "INSERT INTO settings (`key`, value) VALUES ('transactionCount', 1) " +
            "ON DUPLICATE KEY UPDATE value = CAST(CAST(value AS UNSIGNED) + 1 AS CHAR)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 获取指定设置值
     */
    public String getSetting(String key) throws SQLException {
        return queryOneOrNull("SELECT value FROM settings WHERE `key` = ?",
            (rs, rowNum) -> rs.getString("value"), key);
    }

    /**
     * 设置指定值
     */
    public boolean setSetting(String key, String value) throws SQLException {
        return executeUpdate(
            "INSERT INTO settings (`key`, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value = ?",
            key, value, value) > 0;
    }

    /**
     * 删除指定设置
     */
    public boolean deleteSetting(String key) throws SQLException {
        return executeUpdate("DELETE FROM settings WHERE `key` = ?", key) > 0;
    }

    /**
     * 获取所有设置
     */
    public Map<String, String> getAllSettings() throws SQLException {
        Map<String, String> settings = new HashMap<>();
        String sql = "SELECT `key`, `value` FROM settings";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                settings.put(rs.getString("key"), rs.getString("value"));
            }
        }
        return settings;
    }
}
