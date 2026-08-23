package com.cashier.dao;

import com.cashier.model.PurchaseOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购订单数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class PurchaseOrderDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "po.id, po.order_no, po.supplier_id, s.name as supplier_name, po.purchase_date, po.expected_date, " +
        "po.total_amount, po.status, po.purchaser, po.approver, po.approval_time, po.approval_remark, po.remark, po.create_time, po.update_time ";

    private static final RowMapper<PurchaseOrder> ORDER_MAPPER = new RowMapper<PurchaseOrder>() {
        @Override
        public PurchaseOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
            PurchaseOrder order = new PurchaseOrder();
            order.id = rs.getInt("id");
            order.orderNo = rs.getString("order_no");
            order.supplierId = rs.getInt("supplier_id");
            order.supplierName = rs.getString("supplier_name");
            order.purchaseDate = rs.getString("purchase_date");
            order.expectedDate = rs.getString("expected_date");
            order.totalAmount = rs.getBigDecimal("total_amount");
            order.status = rs.getString("status");
            order.purchaser = rs.getString("purchaser");
            order.approver = rs.getString("approver");
            order.approvalTime = rs.getTimestamp("approval_time");
            order.approvalRemark = rs.getString("approval_remark");
            order.remark = rs.getString("remark");
            order.createTime = rs.getTimestamp("create_time");
            order.updateTime = rs.getTimestamp("update_time");
            return order;
        }
    };

    private static final String FROM_JOIN =
        " FROM purchase_orders po LEFT JOIN suppliers s ON po.supplier_id = s.id ";

    public PurchaseOrder findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE po.id = ?", ORDER_MAPPER, id);
    }

    public List<PurchaseOrder> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " ORDER BY po.create_time DESC", ORDER_MAPPER);
    }

    public List<PurchaseOrder> findRecent(int limit) throws SQLException {
        if (limit < 1) {
            return List.of();
        }
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " ORDER BY po.create_time DESC LIMIT ?", ORDER_MAPPER, limit);
    }

    public PurchaseOrder findByOrderNo(String orderNo) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE po.order_no = ?", ORDER_MAPPER, orderNo);
    }

    public List<PurchaseOrder> findBySupplier(int supplierId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE po.supplier_id = ? ORDER BY po.create_time DESC", ORDER_MAPPER, supplierId);
    }

    public List<PurchaseOrder> findByStatus(String status) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE po.status = ? ORDER BY po.create_time DESC", ORDER_MAPPER, status);
    }

    public List<PurchaseOrder> findByDateRange(String startDate, String endDate) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE po.purchase_date BETWEEN ? AND ? ORDER BY po.create_time DESC",
            ORDER_MAPPER, startDate, endDate);
    }

    public List<PurchaseOrder> findByPurchaser(String purchaser) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE po.purchaser = ? ORDER BY po.create_time DESC", ORDER_MAPPER, purchaser);
    }

    public boolean insert(PurchaseOrder order) throws SQLException {
        long id = executeInsertReturnId(
            "INSERT INTO purchase_orders (order_no, supplier_id, purchase_date, expected_date, " +
                "total_amount, status, purchaser, approver, approval_time, approval_remark, remark, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            order.orderNo, order.supplierId,
            order.purchaseDate != null && !order.purchaseDate.isEmpty() ? order.purchaseDate : null,
            order.expectedDate != null && !order.expectedDate.isEmpty() ? order.expectedDate : null,
            order.totalAmount, order.status, order.purchaser, order.approver,
            order.approvalTime, order.approvalRemark, order.remark, order.createTime, order.updateTime);
        order.id = (int) id;
        return id > 0;
    }

    public boolean update(PurchaseOrder order) throws SQLException {
        return executeUpdate(
            "UPDATE purchase_orders SET supplier_id = ?, purchase_date = ?, expected_date = ?, " +
                "total_amount = ?, status = ?, purchaser = ?, approver = ?, approval_time = ?, approval_remark = ?, " +
                "remark = ?, update_time = ? WHERE id = ?",
            order.supplierId, order.purchaseDate, order.expectedDate, order.totalAmount, order.status,
            order.purchaser, order.approver, order.approvalTime, order.approvalRemark, order.remark,
            new Timestamp(System.currentTimeMillis()), order.id) > 0;
    }

    public boolean updateStatus(int id, String status) throws SQLException {
        return executeUpdate(
            "UPDATE purchase_orders SET status = ?, update_time = ? WHERE id = ?",
            status, new Timestamp(System.currentTimeMillis()), id) > 0;
    }

    public boolean updateStatusWithConnection(Connection conn, int id, String status) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "UPDATE purchase_orders SET status = ?, update_time = ? WHERE id = ?")) {
            pstmt.setString(1, status);
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(3, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean approve(int id, String approver, String approvalRemark, String status) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return executeUpdate(
            "UPDATE purchase_orders SET status = ?, approver = ?, approval_time = ?, approval_remark = ?, update_time = ? WHERE id = ?",
            status, approver, now, approvalRemark, now, id) > 0;
    }

    public boolean approvePendingWithConnection(Connection conn, int id, String approver,
                                                String approvalRemark, String status) throws SQLException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try (PreparedStatement pstmt = conn.prepareStatement(
            "UPDATE purchase_orders SET status = ?, approver = ?, approval_time = ?, " +
                "approval_remark = ?, update_time = ? WHERE id = ? AND status = 'pending'")) {
            pstmt.setString(1, status);
            pstmt.setString(2, approver);
            pstmt.setTimestamp(3, now);
            pstmt.setString(4, approvalRemark);
            pstmt.setTimestamp(5, now);
            pstmt.setInt(6, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public String findStatusForUpdate(Connection conn, int id) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT status FROM purchase_orders WHERE id = ? FOR UPDATE")) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }

    public boolean delete(int id) throws SQLException {
        if (hasInboundRecords(id)) {
            throw new SQLException("该采购订单存在入库记录，无法删除。请先删除相关入库记录。");
        }
        return executeUpdate("DELETE FROM purchase_orders WHERE id = ?", id) > 0;
    }

    public boolean hasInboundRecords(int orderId) throws SQLException {
        return queryInt("SELECT COUNT(*) FROM purchase_inbound WHERE order_id = ?", orderId) > 0;
    }

    public int countByStatus(String status) throws SQLException {
        if (status == null || status.isEmpty()) {
            return queryInt("SELECT COUNT(*) as count FROM purchase_orders");
        }
        return queryInt("SELECT COUNT(*) as count FROM purchase_orders WHERE status = ?", status);
    }

    public void batchInsert(List<PurchaseOrder> orders) throws SQLException {
        List<Object[]> params = new ArrayList<>(orders.size());
        for (PurchaseOrder order : orders) {
            params.add(new Object[]{
                order.orderNo, order.supplierId, order.purchaseDate, order.expectedDate,
                order.totalAmount, order.status, order.purchaser, order.approver,
                order.approvalTime, order.approvalRemark, order.remark, order.createTime, order.updateTime});
        }
        batchUpdate(
            "INSERT INTO purchase_orders (order_no, supplier_id, purchase_date, expected_date, " +
                "total_amount, status, purchaser, approver, approval_time, approval_remark, remark, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", params);
    }
}
