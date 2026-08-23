package com.cashier.dao;

import com.cashier.model.PurchaseInbound;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 采购入库记录数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class PurchaseInboundDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "pi.id, pi.inbound_no, pi.order_id, po.order_no, pi.inbound_date, pi.total_quantity, pi.total_amount, pi.operator, pi.remark, pi.create_time ";

    private static final String FROM_JOIN =
        " FROM purchase_inbound pi LEFT JOIN purchase_orders po ON pi.order_id = po.id ";

    private static final RowMapper<PurchaseInbound> INBOUND_MAPPER = new RowMapper<PurchaseInbound>() {
        @Override
        public PurchaseInbound mapRow(ResultSet rs, int rowNum) throws SQLException {
            PurchaseInbound inbound = new PurchaseInbound();
            inbound.id = rs.getInt("id");
            inbound.inboundNo = rs.getString("inbound_no");
            inbound.orderId = rs.getInt("order_id");
            inbound.orderNo = rs.getString("order_no");
            inbound.inboundDate = rs.getString("inbound_date");
            inbound.totalQuantity = rs.getInt("total_quantity");
            inbound.totalAmount = rs.getBigDecimal("total_amount");
            inbound.operator = rs.getString("operator");
            inbound.remark = rs.getString("remark");
            inbound.createTime = rs.getTimestamp("create_time");
            return inbound;
        }
    };

    /**
     * 根据ID查找采购入库记录
     *
     * @param id 入库ID
     * @return 采购入库记录对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public PurchaseInbound findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE pi.id = ?", INBOUND_MAPPER, id);
    }

    /**
     * 查询所有采购入库记录
     *
     * @return 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInbound> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " ORDER BY pi.create_time DESC", INBOUND_MAPPER);
    }

    /**
     * 查询最近采购入库记录，避免历史弹窗默认加载全部记录。
     *
     * @param limit 最大返回数量
     * @return 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInbound> findRecent(int limit) throws SQLException {
        if (limit < 1) {
            return List.of();
        }
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " ORDER BY pi.create_time DESC LIMIT ?", INBOUND_MAPPER, limit);
    }

    /**
     * 根据入库单号查找采购入库记录
     *
     * @param inboundNo 入库单号
     * @return 采购入库记录对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public PurchaseInbound findByInboundNo(String inboundNo) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE pi.inbound_no = ?", INBOUND_MAPPER, inboundNo);
    }

    /**
     * 根据订单ID查找采购入库记录
     *
     * @param orderId 订单ID
     * @return 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInbound> findByOrderId(int orderId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE pi.order_id = ? ORDER BY pi.create_time DESC", INBOUND_MAPPER, orderId);
    }

    /**
     * 根据操作人查找采购入库记录
     *
     * @param operator 操作人
     * @return 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInbound> findByOperator(String operator) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE pi.operator = ? ORDER BY pi.create_time DESC", INBOUND_MAPPER, operator);
    }

    /**
     * 根据日期范围查找采购入库记录
     *
     * @param startDate 开始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInbound> findByDateRange(String startDate, String endDate) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE pi.inbound_date BETWEEN ? AND ? ORDER BY pi.create_time DESC",
            INBOUND_MAPPER, startDate, endDate);
    }

    /**
     * 插入新采购入库记录
     *
     * @param inbound 采购入库记录对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insert(PurchaseInbound inbound) throws SQLException {
        long id = executeInsertReturnId(
            "INSERT INTO purchase_inbound (inbound_no, order_id, inbound_date, total_quantity, total_amount, operator, remark, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            inbound.inboundNo, inbound.orderId, inbound.inboundDate, inbound.totalQuantity,
            inbound.totalAmount, inbound.operator, inbound.remark, inbound.createTime);
        inbound.id = (int) id;
        return id > 0;
    }

    /**
     * 在指定事务连接上插入采购入库记录
     *
     * @param conn    事务连接
     * @param inbound 采购入库记录对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insertWithConnection(Connection conn, PurchaseInbound inbound) throws SQLException {
        String sql = "INSERT INTO purchase_inbound (inbound_no, order_id, inbound_date, total_quantity, " +
            "total_amount, operator, remark, create_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, inbound.inboundNo);
            pstmt.setInt(2, inbound.orderId);
            pstmt.setString(3, inbound.inboundDate);
            pstmt.setInt(4, inbound.totalQuantity);
            pstmt.setBigDecimal(5, inbound.totalAmount);
            pstmt.setString(6, inbound.operator);
            pstmt.setString(7, inbound.remark);
            pstmt.setTimestamp(8, inbound.createTime);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        inbound.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新采购入库记录
     *
     * @param inbound 采购入库记录对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public boolean update(PurchaseInbound inbound) throws SQLException {
        return executeUpdate(
            "UPDATE purchase_inbound SET inbound_no = ?, order_id = ?, inbound_date = ?, " +
                "total_quantity = ?, total_amount = ?, operator = ?, remark = ? WHERE id = ?",
            inbound.inboundNo, inbound.orderId, inbound.inboundDate, inbound.totalQuantity,
            inbound.totalAmount, inbound.operator, inbound.remark, inbound.id) > 0;
    }

    /**
     * 删除采购入库记录
     *
     * @param id 入库ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM purchase_inbound WHERE id = ?", id) > 0;
    }

    /**
     * 批量插入采购入库记录
     *
     * @param inboundList 采购入库记录列表
     * @throws SQLException 数据库操作异常
     */
    public void batchInsert(List<PurchaseInbound> inboundList) throws SQLException {
        batchUpdate(
            "INSERT INTO purchase_inbound (inbound_no, order_id, inbound_date, total_quantity, total_amount, operator, remark, create_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            inboundList.stream()
                .map(inbound -> new Object[]{
                    inbound.inboundNo, inbound.orderId, inbound.inboundDate, inbound.totalQuantity,
                    inbound.totalAmount, inbound.operator, inbound.remark, inbound.createTime})
                .toList());
    }

}
