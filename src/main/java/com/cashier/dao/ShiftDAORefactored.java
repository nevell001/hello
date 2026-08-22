package com.cashier.dao;

import com.cashier.model.Shift;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 班次数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class ShiftDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "shift_id, operator_username, operator_name, start_time, end_time, " +
        "opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, " +
        "closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, " +
        "alipay_revenue, card_revenue, notes ";

    private static final RowMapper<Shift> SHIFT_MAPPER = new RowMapper<Shift>() {
        @Override
        public Shift mapRow(ResultSet rs, int rowNum) throws SQLException {
            Shift shift = new Shift();
            shift.shiftId = rs.getString("shift_id");
            shift.username = rs.getString("operator_username");
            shift.operatorName = rs.getString("operator_name");

            long startTime = rs.getLong("start_time");
            shift.startTime = rs.wasNull() ? null : java.time.Instant.ofEpochMilli(startTime);
            long endTime = rs.getLong("end_time");
            shift.endTime = rs.wasNull() ? null : java.time.Instant.ofEpochMilli(endTime);

            shift.openingRevenue = rs.getBigDecimal("opening_revenue");
            shift.closingRevenue = rs.getBigDecimal("closing_revenue");
            shift.shiftRevenue = rs.getBigDecimal("shift_revenue");
            shift.openingTransactionCount = rs.getInt("opening_transaction_count");
            shift.closingTransactionCount = rs.getInt("closing_transaction_count");
            shift.shiftTransactionCount = rs.getInt("shift_transaction_count");
            shift.cashRevenue = rs.getBigDecimal("cash_revenue");
            shift.wechatRevenue = rs.getBigDecimal("wechat_revenue");
            shift.alipayRevenue = rs.getBigDecimal("alipay_revenue");
            shift.cardRevenue = rs.getBigDecimal("card_revenue");
            shift.notes = rs.getString("notes");
            return shift;
        }
    };

    public boolean insert(Shift shift) throws SQLException {
        return executeUpdate(
            "INSERT INTO shifts (shift_id, operator_username, operator_name, start_time, end_time, " +
                "opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, " +
                "closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, " +
                "alipay_revenue, card_revenue, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            shift.shiftId, shift.username, shift.operatorName,
            shift.startTime != null ? shift.startTime.toEpochMilli() : 0L,
            shift.endTime != null ? shift.endTime.toEpochMilli() : 0L,
            shift.openingRevenue, shift.closingRevenue, shift.shiftRevenue,
            shift.openingTransactionCount, shift.closingTransactionCount, shift.shiftTransactionCount,
            shift.cashRevenue, shift.wechatRevenue, shift.alipayRevenue, shift.cardRevenue, shift.notes) > 0;
    }

    public boolean update(Shift shift) throws SQLException {
        return executeUpdate(
            "UPDATE shifts SET end_time = ?, closing_revenue = ?, shift_revenue = ?, " +
                "closing_transaction_count = ?, shift_transaction_count = ?, " +
                "cash_revenue = ?, wechat_revenue = ?, alipay_revenue = ?, card_revenue = ?, " +
                "notes = ? WHERE shift_id = ?",
            shift.endTime != null ? shift.endTime.toEpochMilli() : 0L,
            shift.closingRevenue, shift.shiftRevenue,
            shift.closingTransactionCount, shift.shiftTransactionCount,
            shift.cashRevenue, shift.wechatRevenue, shift.alipayRevenue, shift.cardRevenue,
            shift.notes, shift.shiftId) > 0;
    }

    public Shift findById(String shiftId) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM shifts WHERE shift_id = ?", SHIFT_MAPPER, shiftId);
    }

    public List<Shift> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + " FROM shifts ORDER BY start_time DESC", SHIFT_MAPPER);
    }

    public List<Shift> findRecent(int limit) throws SQLException {
        if (limit < 1) {
            return List.of();
        }
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM shifts ORDER BY start_time DESC LIMIT ?", SHIFT_MAPPER, limit);
    }

    public Shift findActiveShift() throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM shifts WHERE end_time = start_time ORDER BY start_time DESC LIMIT 1", SHIFT_MAPPER);
    }

    public boolean hasActiveShift() throws SQLException {
        return queryInt("SELECT COUNT(*) FROM shifts WHERE end_time = start_time") > 0;
    }

    public List<Shift> findByOperator(String username) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM shifts WHERE operator_username = ? ORDER BY start_time DESC", SHIFT_MAPPER, username);
    }

    public boolean delete(String shiftId) throws SQLException {
        return executeUpdate("DELETE FROM shifts WHERE shift_id = ?", shiftId) > 0;
    }

    public void batchInsert(List<Shift> shifts) throws SQLException {
        List<Object[]> params = new ArrayList<>(shifts.size());
        for (Shift shift : shifts) {
            params.add(new Object[]{
                shift.shiftId, shift.username, shift.operatorName,
                shift.startTime != null ? shift.startTime.toEpochMilli() : 0L,
                shift.endTime != null ? shift.endTime.toEpochMilli() : 0L,
                shift.openingRevenue, shift.closingRevenue, shift.shiftRevenue,
                shift.openingTransactionCount, shift.closingTransactionCount, shift.shiftTransactionCount,
                shift.cashRevenue, shift.wechatRevenue, shift.alipayRevenue, shift.cardRevenue, shift.notes});
        }
        batchUpdate(
            "INSERT INTO shifts (shift_id, operator_username, operator_name, start_time, end_time, " +
                "opening_revenue, closing_revenue, shift_revenue, opening_transaction_count, " +
                "closing_transaction_count, shift_transaction_count, cash_revenue, wechat_revenue, " +
                "alipay_revenue, card_revenue, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            params);
    }
}
