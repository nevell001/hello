package com.cashier.dao;

import com.cashier.model.Promotion;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 促销数据访问对象
 * 负责促销相关的数据库操作
 */
public class PromotionDAO {

    /**
     * 查询所有促销
     */
    public static List<Promotion> findAll() throws SQLException {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT id, promotion_code, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                promotions.add(mapRowToPromotion(rs));
            }
        }
        return promotions;
    }

    /**
     * 查询最近促销，限制返回数量
     */
    public static List<Promotion> findRecent(int limit) throws SQLException {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT id, promotion_code, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions ORDER BY id DESC LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Math.max(1, limit));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                promotions.add(mapRowToPromotion(rs));
            }
        }
        return promotions;
    }

    /**
     * 根据ID查找促销
     */
    public static Promotion findById(int id) throws SQLException {
        String sql = "SELECT id, promotion_code, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPromotion(rs);
                }
            }
        }
        return null;
    }

    /**
     * 使用指定连接根据ID查找促销（事务内使用）
     */
    public static Promotion findByIdWithConnection(Connection conn, int id) throws SQLException {
        String sql = "SELECT id, promotion_code, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPromotion(rs);
                }
            }
        }
        return null;
    }

    /**
     * 查询启用的促销
     */
    public static List<Promotion> findEnabled() throws SQLException {
        List<Promotion> promotions = new ArrayList<>();
        String sql = "SELECT id, promotion_code, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions " +
                     "WHERE enabled = true ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                promotions.add(mapRowToPromotion(rs));
            }
        }
        return promotions;
    }

    /**
     * 查询当前有效的促销
     */
    public static List<Promotion> findActive() throws SQLException {
        List<Promotion> promotions = new ArrayList<>();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        String sql = "SELECT id, promotion_code, name, type, threshold, discount, description, enabled, " +
                     "start_date, end_date, usage_count, max_usage FROM promotions " +
                     "WHERE enabled = true AND start_date <= ? AND end_date >= ? " +
                     "ORDER BY id DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, now);
            pstmt.setTimestamp(2, now);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    promotions.add(mapRowToPromotion(rs));
                }
            }
        }
        return promotions;
    }

    /**
     * 插入新促销
     */
    public static boolean insert(Promotion promotion) throws SQLException {
        String sql = "INSERT INTO promotions (promotion_code, name, type, threshold, discount, description, " +
                     "enabled, start_date, end_date, usage_count, max_usage) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, promotion.promotionCode);
            pstmt.setString(2, promotion.name);
            pstmt.setString(3, promotion.type);
            pstmt.setBigDecimal(4, promotion.threshold);
            pstmt.setBigDecimal(5, promotion.discount);
            pstmt.setString(6, promotion.description);
            pstmt.setBoolean(7, promotion.enabled);
            pstmt.setTimestamp(8, promotion.startDate != null ? Timestamp.valueOf(promotion.startDate) : null);
            pstmt.setTimestamp(9, promotion.endDate != null ? Timestamp.valueOf(promotion.endDate) : null);
            pstmt.setInt(10, promotion.usageCount);
            pstmt.setInt(11, promotion.maxUsage);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        promotion.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新促销
     */
    public static boolean update(Promotion promotion) throws SQLException {
        String sql = "UPDATE promotions SET promotion_code = ?, name = ?, type = ?, threshold = ?, discount = ?, " +
                     "description = ?, enabled = ?, start_date = ?, end_date = ?, " +
                     "usage_count = ?, max_usage = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, promotion.promotionCode);
            pstmt.setString(2, promotion.name);
            pstmt.setString(3, promotion.type);
            pstmt.setBigDecimal(4, promotion.threshold);
            pstmt.setBigDecimal(5, promotion.discount);
            pstmt.setString(6, promotion.description);
            pstmt.setBoolean(7, promotion.enabled);
            pstmt.setTimestamp(8, promotion.startDate != null ? Timestamp.valueOf(promotion.startDate) : null);
            pstmt.setTimestamp(9, promotion.endDate != null ? Timestamp.valueOf(promotion.endDate) : null);
            pstmt.setInt(10, promotion.usageCount);
            pstmt.setInt(11, promotion.maxUsage);
            pstmt.setInt(12, promotion.id);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除促销
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM promotions WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 增加促销使用次数
     */
    public static boolean incrementUsage(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return incrementUsageWithConnection(conn, id);
        }
    }

    /**
     * 使用指定连接增加促销使用次数
     * @param conn 数据库连接
     * @param id 促销ID
     * @return 更新是否成功
     * @throws SQLException 数据库操作异常
     */
    public static boolean incrementUsageWithConnection(Connection conn, int id) throws SQLException {
        // 加并发保护：max_usage <= 0（含 -1 和 0）表示不限次，否则要求 usage_count < max_usage
        String sql = "UPDATE promotions SET usage_count = usage_count + 1 WHERE id = ? AND (max_usage <= 0 OR usage_count < max_usage)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入促销
     */
    public static void batchInsert(List<Promotion> promotions) throws SQLException {
        String sql = "INSERT INTO promotions (promotion_code, name, type, threshold, discount, description, " +
                     "enabled, start_date, end_date, usage_count, max_usage) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Promotion promotion : promotions) {
                pstmt.setString(1, promotion.promotionCode);
                pstmt.setString(2, promotion.name);
                pstmt.setString(3, promotion.type);
                pstmt.setBigDecimal(4, promotion.threshold);
                pstmt.setBigDecimal(5, promotion.discount);
                pstmt.setString(6, promotion.description);
                pstmt.setBoolean(7, promotion.enabled);
                pstmt.setTimestamp(8, promotion.startDate != null ? Timestamp.valueOf(promotion.startDate) : null);
                pstmt.setTimestamp(9, promotion.endDate != null ? Timestamp.valueOf(promotion.endDate) : null);
                pstmt.setInt(10, promotion.usageCount);
                pstmt.setInt(11, promotion.maxUsage);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 使用指定连接批量插入促销
     * @param conn 数据库连接
     * @param promotions 促销列表
     * @throws SQLException 数据库操作异常
     */
    public static void batchInsertWithConnection(Connection conn, List<Promotion> promotions) throws SQLException {
        if (promotions == null || promotions.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO promotions (promotion_code, name, type, threshold, discount, description, " +
                     "enabled, start_date, end_date, usage_count, max_usage) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Promotion promotion : promotions) {
                pstmt.setString(1, promotion.promotionCode);
                pstmt.setString(2, promotion.name);
                pstmt.setString(3, promotion.type);
                pstmt.setBigDecimal(4, promotion.threshold);
                pstmt.setBigDecimal(5, promotion.discount);
                pstmt.setString(6, promotion.description);
                pstmt.setBoolean(7, promotion.enabled);
                pstmt.setTimestamp(8, promotion.startDate != null ? Timestamp.valueOf(promotion.startDate) : null);
                pstmt.setTimestamp(9, promotion.endDate != null ? Timestamp.valueOf(promotion.endDate) : null);
                pstmt.setInt(10, promotion.usageCount);
                pstmt.setInt(11, promotion.maxUsage);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 Promotion 对象
     */
    private static Promotion mapRowToPromotion(ResultSet rs) throws SQLException {
        Timestamp startDateTs = rs.getTimestamp("start_date");
        Timestamp endDateTs = rs.getTimestamp("end_date");
        return new Promotion(
            rs.getInt("id"),
            rs.getString("promotion_code"),
            rs.getString("name"),
            rs.getString("type"),
            rs.getBigDecimal("threshold"),
            rs.getBigDecimal("discount"),
            rs.getString("description"),
            rs.getBoolean("enabled"),
            startDateTs != null ? startDateTs.toLocalDateTime() : null,
            endDateTs != null ? endDateTs.toLocalDateTime() : null,
            rs.getInt("usage_count"),
            rs.getInt("max_usage")
        );
    }
}
