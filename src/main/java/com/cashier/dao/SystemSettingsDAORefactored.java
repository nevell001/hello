package com.cashier.dao;

import java.sql.Connection;
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
     * 获取指定设置值
     */
    public String getSetting(String key) throws SQLException {
        return queryOneOrNull("SELECT `value` FROM settings WHERE `key` = ?",
            (rs, rowNum) -> rs.getString("value"), key);
    }

    /**
     * 设置指定值
     */
    public boolean setSetting(String key, String value) throws SQLException {
        return executeUpdate(
            "INSERT INTO settings (`key`, `value`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `value` = ?",
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
