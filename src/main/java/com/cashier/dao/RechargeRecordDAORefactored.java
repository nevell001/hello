package com.cashier.dao;

import com.cashier.model.RechargeRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 充值记录数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class RechargeRecordDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "record_id, member_phone, member_name, amount, payment_method, timestamp, operator ";

    private static final RowMapper<RechargeRecord> RECORD_MAPPER = new RowMapper<RechargeRecord>() {
        @Override
        public RechargeRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            RechargeRecord record = new RechargeRecord();
            record.recordId = rs.getString("record_id");
            record.memberPhone = rs.getString("member_phone");
            record.memberName = rs.getString("member_name");
            record.amount = rs.getBigDecimal("amount");
            record.paymentMethod = rs.getString("payment_method");
            record.timestamp = rs.getTimestamp("timestamp");
            record.operator = rs.getString("operator");
            return record;
        }
    };

    public List<RechargeRecord> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM recharge_records ORDER BY timestamp DESC", RECORD_MAPPER);
    }

    public List<RechargeRecord> findRecent(int limit) throws SQLException {
        if (limit < 1) {
            return List.of();
        }
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM recharge_records ORDER BY timestamp DESC LIMIT ?", RECORD_MAPPER, limit);
    }

    public RechargeRecord findById(String recordId) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM recharge_records WHERE record_id = ?", RECORD_MAPPER, recordId);
    }

    public List<RechargeRecord> findByMemberPhone(String memberPhone) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM recharge_records WHERE member_phone = ? ORDER BY timestamp DESC", RECORD_MAPPER, memberPhone);
    }

    public List<RechargeRecord> findRecentByMemberPhone(String memberPhone, int limit) throws SQLException {
        if (memberPhone == null || memberPhone.isBlank() || limit < 1) {
            return List.of();
        }
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM recharge_records WHERE member_phone = ? ORDER BY timestamp DESC LIMIT ?",
            RECORD_MAPPER, memberPhone, limit);
    }

    public List<RechargeRecord> findByDateRange(java.util.Date startDate, java.util.Date endDate) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM recharge_records WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC",
            RECORD_MAPPER, new Timestamp(startDate.getTime()), new Timestamp(endDate.getTime()));
    }

    public boolean insert(RechargeRecord record) throws SQLException {
        return executeUpdate(
            "INSERT INTO recharge_records (record_id, member_phone, member_name, amount, payment_method, timestamp, operator) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            record.recordId, record.memberPhone, record.memberName, record.amount,
            record.paymentMethod, new Timestamp(record.timestamp.getTime()), record.operator) > 0;
    }

    public boolean insertWithConnection(Connection conn, RechargeRecord record) throws SQLException {
        String sql = "INSERT INTO recharge_records (record_id, member_phone, member_name, " +
            "amount, payment_method, timestamp, operator) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.recordId);
            pstmt.setString(2, record.memberPhone);
            pstmt.setString(3, record.memberName);
            pstmt.setBigDecimal(4, record.amount);
            pstmt.setString(5, record.paymentMethod);
            pstmt.setTimestamp(6, new Timestamp(record.timestamp.getTime()));
            pstmt.setString(7, record.operator);
            return pstmt.executeUpdate() > 0;
        }
    }

    public void batchInsert(List<RechargeRecord> records) throws SQLException {
        List<Object[]> params = new ArrayList<>(records.size());
        for (RechargeRecord record : records) {
            params.add(new Object[]{
                record.recordId, record.memberPhone, record.memberName, record.amount,
                record.paymentMethod, new Timestamp(record.timestamp.getTime()), record.operator});
        }
        batchUpdate(
            "INSERT INTO recharge_records (record_id, member_phone, member_name, amount, payment_method, timestamp, operator) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)", params);
    }
}
