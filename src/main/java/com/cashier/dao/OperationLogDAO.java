package com.cashier.dao;

import com.cashier.model.OperationLog;
import com.cashier.util.DatabaseManager;

import java.sql.*;
import java.util.*;

/**
 * 操作日志数据访问对象
 * 负责操作日志相关的数据库操作
 */
public class OperationLogDAO {
    private static final String COLUMNS =
        "id, username, operation, details, ip_address, timestamp, log_level, " +
        "log_category, operation_result, affected_records";

    /**
     * 查询所有操作日志
     */
    public static List<OperationLog> findAll() throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " " +
                     "FROM operation_logs ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                logs.add(mapRowToOperationLog(rs));
            }
        }
        return logs;
    }

    /**
     * 根据ID查找操作日志
     */
    public static OperationLog findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " " +
                     "FROM operation_logs WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRowToOperationLog(rs);
            }
        }
        return null;
    }

    /**
     * 根据用户名查找操作日志
     */
    public static List<OperationLog> findByUsername(String username) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " " +
                     "FROM operation_logs WHERE username = ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapRowToOperationLog(rs));
            }
        }
        return logs;
    }

    /**
     * 根据操作类型查找操作日志
     */
    public static List<OperationLog> findByOperation(String operation) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " " +
                     "FROM operation_logs WHERE operation = ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, operation);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapRowToOperationLog(rs));
            }
        }
        return logs;
    }

    /**
     * 根据日期范围查找操作日志
     */
    public static List<OperationLog> findByDateRange(java.util.Date startDate, java.util.Date endDate) throws SQLException {
        List<OperationLog> logs = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " " +
                     "FROM operation_logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, startDate.getTime());
            pstmt.setLong(2, endDate.getTime());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapRowToOperationLog(rs));
            }
        }
        return logs;
    }

    /**
     * 插入新操作日志
     */
    public static boolean insert(OperationLog log) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return insertWithConnection(conn, log);
        }
    }

    /**
     * 使用指定连接插入操作日志
     * @param conn 数据库连接
     * @param log 操作日志
     * @return 如果插入成功返回true，否则返回false
     * @throws SQLException 数据库操作异常
     */
    public static boolean insertWithConnection(Connection conn, OperationLog log) throws SQLException {
        String sql = "INSERT INTO operation_logs " +
                     "(username, operation, details, ip_address, timestamp, log_level, log_category, operation_result, affected_records) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameters(pstmt, log);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 批量插入操作日志
     */
    public static void batchInsert(List<OperationLog> logs) throws SQLException {
        String sql = "INSERT INTO operation_logs " +
                     "(username, operation, details, ip_address, timestamp, log_level, log_category, operation_result, affected_records) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (OperationLog log : logs) {
                setParameters(pstmt, log);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 删除指定日期之前的日志
     */
    public static boolean deleteBeforeDate(java.util.Date date) throws SQLException {
        String sql = "DELETE FROM operation_logs WHERE timestamp < ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, date.getTime());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 将 ResultSet 映射为 OperationLog 对象
     */
    private static OperationLog mapRowToOperationLog(ResultSet rs) throws SQLException {
        OperationLog log = new OperationLog();
        log.logId = String.valueOf(rs.getInt("id"));
        log.username = rs.getString("username");
        log.operation = rs.getString("operation");
        log.details = rs.getString("details");
        log.timestamp = java.time.Instant.ofEpochMilli(rs.getLong("timestamp"));
        log.ipAddress = rs.getString("ip_address");
        log.logLevel = rs.getString("log_level");
        log.category = rs.getString("log_category");
        log.result = rs.getString("operation_result");
        log.affectedRecords = rs.getInt("affected_records");
        return log;
    }

    private static void setParameters(PreparedStatement pstmt, OperationLog log) throws SQLException {
        if (log.username == null || log.username.isBlank()) {
            pstmt.setNull(1, Types.VARCHAR);
        } else {
            pstmt.setString(1, log.username);
        }
        pstmt.setString(2, log.operation);
        pstmt.setString(3, log.details);
        pstmt.setString(4, log.ipAddress);
        pstmt.setLong(5, log.timestamp.toEpochMilli());
        pstmt.setString(6, log.logLevel);
        pstmt.setString(7, log.category);
        pstmt.setString(8, log.result);
        pstmt.setInt(9, log.affectedRecords);
    }
}
