package com.cashier.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 字号偏好数据访问对象（重构版）
 * 负责字号偏好相关的数据库操作，通过 DAOFactory 获取。
 */
public class FontSizePreferenceDAORefactored extends BaseDAO {

    /**
     * 获取字号偏好
     */
    public String getFontSizePreference(String username) throws SQLException {
        String sql = "SELECT font_size FROM font_size_preferences WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("font_size");
                }
            }
        }
        return "medium"; // 默认中等字号
    }

    /**
     * 设置字号偏好
     */
    public boolean setFontSizePreference(String username, String fontSize) throws SQLException {
        String sql = "INSERT INTO font_size_preferences (username, font_size, updated_at) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE font_size = ?, updated_at = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            pstmt.setString(1, username);
            pstmt.setString(2, fontSize);
            pstmt.setLong(3, now);
            pstmt.setString(4, fontSize);
            pstmt.setLong(5, now);
            return pstmt.executeUpdate() > 0;
        }
    }
}
