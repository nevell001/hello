package com.cashier.dao;

import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.*;
import org.slf4j.Logger;

/**
 * 语言偏好数据访问对象
 * 负责语言偏好和货币偏好相关的数据库操作
 */
public class LanguagePreferenceDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(LanguagePreferenceDAO.class);

    /**
     * 获取语言偏好
     * 优先返回用户特定偏好，若不存在则返回全局默认（"default"用户），最后返回系统默认
     */
    public static String getLanguagePreference(String username) throws SQLException {
        String sql = "SELECT language_tag FROM language_preferences WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 首先尝试获取用户特定偏好
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String userLang = rs.getString("language_tag");
                logger.debug("用户 {} 的语言偏好: {}", username, userLang);
                return userLang;
            }

            // 如果用户没有特定偏好，尝试获取全局默认（"default"用户）
            pstmt.setString(1, "default");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                String globalDefault = rs.getString("language_tag");
                if (globalDefault != null && !globalDefault.isEmpty()) {
                    logger.debug("用户 {} 使用全局默认语言: {}", username, globalDefault);
                    return globalDefault;
                }
            }
        }
        logger.debug("用户 {} 使用系统默认语言: zh-CN", username);
        return "zh-CN"; // 系统默认简体中文
    }

    /**
     * 设置语言偏好
     */
    public static boolean setLanguagePreference(String username, String languageTag) throws SQLException {
        String sql = "INSERT INTO language_preferences (username, language_tag, updated_at) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE language_tag = ?, updated_at = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            long now = System.currentTimeMillis();
            pstmt.setString(1, username);
            pstmt.setString(2, languageTag);
            pstmt.setLong(3, now);
            pstmt.setString(4, languageTag);
            pstmt.setLong(5, now);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 获取货币偏好
     */
    public static String getCurrencyPreference(String username) throws SQLException {
        String sql = "SELECT currency_code FROM language_preferences WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String currencyCode = rs.getString("currency_code");
                return currencyCode != null && !currencyCode.isEmpty() ? currencyCode : "CNY";
            }
        } catch (SQLException e) {
            // 如果字段不存在（旧版本数据库），返回默认值
            if (isMissingCurrencyColumn(e)) {
                logger.debug("currency_code 字段不存在，返回默认值 CNY", e);
                return "CNY";
            }
            throw e;
        }
        return "CNY"; // 默认人民币
    }

    /**
     * 设置货币偏好
     */
    public static boolean setCurrencyPreference(String username, String currencyCode) throws SQLException {
        boolean columnEnsured = false;
        while (true) {
            String sql = "INSERT INTO language_preferences (username, language_tag, currency_code, updated_at) VALUES (?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE currency_code = ?, updated_at = ?";

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                long now = System.currentTimeMillis();
                // 获取当前语言偏好
                String currentLanguage = getLanguagePreference(username);

                pstmt.setString(1, username);
                pstmt.setString(2, currentLanguage);
                pstmt.setString(3, currencyCode);
                pstmt.setLong(4, now);
                pstmt.setString(5, currencyCode);
                pstmt.setLong(6, now);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                // 如果字段不存在（旧版本数据库），先执行升级再重试一次
                if (!columnEnsured && isMissingCurrencyColumn(e)) {
                    logger.warn("currency_code 字段不存在，执行 ALTER TABLE 升级", e);
                    ensureCurrencyColumn();
                    columnEnsured = true;
                    continue; // 重试一次，不再递归
                }
                throw e;
            }
        }
    }

    /**
     * 检测异常是否为 currency_code 字段缺失
     * 使用 SQLState + 空安全消息检查，避免依赖 getMessage() 字符串匹配
     */
    private static boolean isMissingCurrencyColumn(SQLException e) {
        if (e == null) return false;
        // SQLState 42S22 = MySQL "Unknown column", 42122 = H2 "Column not found"
        // 均以 "42" 开头（SQL标准：语法错误或访问规则违反）
        String sqlState = e.getSQLState();
        String message = e.getMessage();
        if (sqlState != null && sqlState.startsWith("42")) {
            return message != null && message.contains("currency_code");
        }
        // SQLState 为 null 时的兜底（部分驱动不设置 SQLState）
        return message != null && message.contains("currency_code");
    }

    /**
     * 确保 currency_code 字段存在（ALTER TABLE 升级）
     */
    private static void ensureCurrencyColumn() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE language_preferences ADD COLUMN currency_code VARCHAR(10) DEFAULT 'CNY'");
            logger.info("已成功添加 currency_code 字段");
        } catch (SQLException ex) {
            // 字段可能已存在（并发场景），记录后继续
            logger.debug("添加 currency_code 字段失败，可能已存在", ex);
        }
    }
}
