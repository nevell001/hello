package com.cashier.dao;

import com.cashier.model.OperationLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class OperationLogDAORefactored extends BaseDAO {

    private static final String COLUMNS =
        "id, username, operation, details, ip_address, timestamp, log_level, " +
        "log_category, operation_result, affected_records";

    private static final RowMapper<OperationLog> LOG_MAPPER = new RowMapper<OperationLog>() {
        @Override
        public OperationLog mapRow(ResultSet rs, int rowNum) throws SQLException {
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
    };

    public List<OperationLog> findAll() throws SQLException {
        return queryList("SELECT " + COLUMNS + " FROM operation_logs ORDER BY timestamp DESC", LOG_MAPPER);
    }

    public List<OperationLog> findRecent(int limit) throws SQLException {
        if (limit < 1) {
            return List.of();
        }
        return queryList("SELECT " + COLUMNS +
            " FROM operation_logs ORDER BY timestamp DESC LIMIT ?", LOG_MAPPER, limit);
    }

    public OperationLog findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + COLUMNS +
            " FROM operation_logs WHERE id = ?", LOG_MAPPER, id);
    }

    public List<OperationLog> findByUsername(String username) throws SQLException {
        return queryList("SELECT " + COLUMNS +
            " FROM operation_logs WHERE username = ? ORDER BY timestamp DESC", LOG_MAPPER, username);
    }

    public List<OperationLog> findByOperation(String operation) throws SQLException {
        return queryList("SELECT " + COLUMNS +
            " FROM operation_logs WHERE operation = ? ORDER BY timestamp DESC", LOG_MAPPER, operation);
    }

    public List<OperationLog> findByDateRange(java.util.Date startDate, java.util.Date endDate) throws SQLException {
        return queryList("SELECT " + COLUMNS +
            " FROM operation_logs WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC",
            LOG_MAPPER, startDate.getTime(), endDate.getTime());
    }

    public boolean insert(OperationLog log) throws SQLException {
        try (Connection conn = getConnection()) {
            return insertWithConnection(conn, log);
        }
    }

    public boolean insertWithConnection(Connection conn, OperationLog log) throws SQLException {
        String sql = "INSERT INTO operation_logs " +
            "(username, operation, details, ip_address, timestamp, log_level, log_category, operation_result, affected_records) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            setParameters(pstmt, log);
            return pstmt.executeUpdate() > 0;
        }
    }

    public void batchInsert(List<OperationLog> logs) throws SQLException {
        List<Object[]> params = new ArrayList<>(logs.size());
        for (OperationLog log : logs) {
            params.add(new Object[]{
                log.username == null || log.username.isBlank() ? null : log.username,
                log.operation, log.details, log.ipAddress, log.timestamp.toEpochMilli(),
                log.logLevel, log.category, log.result, log.affectedRecords});
        }
        batchUpdate(
            "INSERT INTO operation_logs " +
                "(username, operation, details, ip_address, timestamp, log_level, log_category, operation_result, affected_records) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", params);
    }

    public boolean deleteBeforeDate(java.util.Date date) throws SQLException {
        return executeUpdate("DELETE FROM operation_logs WHERE timestamp < ?", date.getTime()) > 0;
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
