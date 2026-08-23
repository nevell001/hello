package com.cashier.dao;

import com.cashier.model.PurchaseApproval;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 采购审批记录数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class PurchaseApprovalDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "id, order_id, approver, action, remark, approval_time ";

    private static final RowMapper<PurchaseApproval> APPROVAL_MAPPER = new RowMapper<PurchaseApproval>() {
        @Override
        public PurchaseApproval mapRow(ResultSet rs, int rowNum) throws SQLException {
            PurchaseApproval approval = new PurchaseApproval();
            approval.id = rs.getInt("id");
            approval.orderId = rs.getInt("order_id");
            approval.approver = rs.getString("approver");
            approval.action = rs.getString("action");
            approval.remark = rs.getString("remark");
            approval.approvalTime = rs.getTimestamp("approval_time");
            return approval;
        }
    };

    /**
     * 根据ID查找采购审批记录
     *
     * @param id 记录ID
     * @return 采购审批记录对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public PurchaseApproval findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM purchase_approvals WHERE id = ?", APPROVAL_MAPPER, id);
    }

    /**
     * 查询所有采购审批记录
     *
     * @return 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseApproval> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM purchase_approvals ORDER BY approval_time DESC", APPROVAL_MAPPER);
    }

    /**
     * 根据订单ID查找所有审批记录
     *
     * @param orderId 订单ID
     * @return 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseApproval> findByOrderId(int orderId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM purchase_approvals WHERE order_id = ? ORDER BY approval_time DESC", APPROVAL_MAPPER, orderId);
    }

    /**
     * 根据审批人查找审批记录
     *
     * @param approver 审批人
     * @return 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseApproval> findByApprover(String approver) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM purchase_approvals WHERE approver = ? ORDER BY approval_time DESC", APPROVAL_MAPPER, approver);
    }

    /**
     * 根据审批动作查找审批记录
     *
     * @param action 审批动作（approve-通过，reject-拒绝）
     * @return 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseApproval> findByAction(String action) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM purchase_approvals WHERE action = ? ORDER BY approval_time DESC", APPROVAL_MAPPER, action);
    }

    /**
     * 根据订单ID和审批人查找审批记录
     *
     * @param orderId  订单ID
     * @param approver 审批人
     * @return 采购审批记录对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public PurchaseApproval findByOrderAndApprover(int orderId, String approver) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM purchase_approvals WHERE order_id = ? AND approver = ?",
            APPROVAL_MAPPER, orderId, approver);
    }

    /**
     * 插入新采购审批记录
     *
     * @param approval 采购审批记录对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insert(PurchaseApproval approval) throws SQLException {
        long id = executeInsertReturnId(
            "INSERT INTO purchase_approvals (order_id, approver, action, remark, approval_time) " +
                "VALUES (?, ?, ?, ?, ?)",
            approval.orderId, approval.approver, approval.action, approval.remark, approval.approvalTime);
        approval.id = (int) id;
        return id > 0;
    }

    /**
     * 在指定事务连接上插入采购审批记录
     *
     * @param conn     事务连接
     * @param approval 采购审批记录对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insertWithConnection(Connection conn, PurchaseApproval approval) throws SQLException {
        String sql = "INSERT INTO purchase_approvals (order_id, approver, action, remark, approval_time) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, approval.orderId);
            pstmt.setString(2, approval.approver);
            pstmt.setString(3, approval.action);
            pstmt.setString(4, approval.remark);
            pstmt.setTimestamp(5, approval.approvalTime);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        approval.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 删除采购审批记录
     *
     * @param id 记录ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM purchase_approvals WHERE id = ?", id) > 0;
    }

    /**
     * 根据订单ID删除所有审批记录
     *
     * @param orderId 订单ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public boolean deleteByOrderId(int orderId) throws SQLException {
        return executeUpdate("DELETE FROM purchase_approvals WHERE order_id = ?", orderId) > 0;
    }

    /**
     * 批量插入采购审批记录
     *
     * @param approvals 采购审批记录列表
     * @throws SQLException 数据库操作异常
     */
    public void batchInsert(List<PurchaseApproval> approvals) throws SQLException {
        batchUpdate(
            "INSERT INTO purchase_approvals (order_id, approver, action, remark, approval_time) " +
                "VALUES (?, ?, ?, ?, ?)",
            approvals.stream()
                .map(approval -> new Object[]{
                    approval.orderId, approval.approver, approval.action, approval.remark, approval.approvalTime})
                .toList());
    }

    /**
     * 统计审批记录数量
     *
     * @param action 审批动作（可为null）
     * @return 记录数量
     * @throws SQLException 数据库操作异常
     */
    public int countByAction(String action) throws SQLException {
        if (action == null || action.isEmpty()) {
            return queryInt("SELECT COUNT(*) FROM purchase_approvals");
        }
        return queryInt("SELECT COUNT(*) FROM purchase_approvals WHERE action = ?", action);
    }
}
