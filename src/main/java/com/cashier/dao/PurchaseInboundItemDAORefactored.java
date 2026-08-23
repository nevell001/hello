package com.cashier.dao;

import com.cashier.model.PurchaseInboundItem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 采购入库明细数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class PurchaseInboundItemDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "ii.id, ii.inbound_id, ii.order_item_id, ii.product_id, p.name as product_name, ii.quantity, ii.unit_price, ii.total_price ";

    private static final String FROM_JOIN =
        " FROM purchase_inbound_items ii LEFT JOIN products p ON ii.product_id = p.id ";

    private static final RowMapper<PurchaseInboundItem> ITEM_MAPPER = new RowMapper<PurchaseInboundItem>() {
        @Override
        public PurchaseInboundItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            PurchaseInboundItem item = new PurchaseInboundItem();
            item.id = rs.getInt("id");
            item.inboundId = rs.getInt("inbound_id");
            item.orderItemId = rs.getInt("order_item_id");
            item.productId = rs.getInt("product_id");
            item.productName = rs.getString("product_name");
            item.quantity = rs.getInt("quantity");
            item.unitPrice = rs.getBigDecimal("unit_price");
            item.totalPrice = rs.getBigDecimal("total_price");
            return item;
        }
    };

    /**
     * 根据ID查找采购入库明细
     *
     * @param id 明细ID
     * @return 采购入库明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public PurchaseInboundItem findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE ii.id = ?", ITEM_MAPPER, id);
    }

    /**
     * 根据入库ID查找所有明细
     *
     * @param inboundId 入库ID
     * @return 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInboundItem> findByInboundId(int inboundId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE ii.inbound_id = ? ORDER BY ii.id", ITEM_MAPPER, inboundId);
    }

    /**
     * 根据入库ID查找所有明细（别名方法）
     *
     * @param inboundId 入库ID
     * @return 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInboundItem> findByInbound(int inboundId) throws SQLException {
        return findByInboundId(inboundId);
    }

    /**
     * 根据多个入库ID批量查找明细，避免报表逐单查询造成 N+1 数据库访问。
     *
     * @param inboundIds 入库ID集合
     * @return 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInboundItem> findByInboundIds(Collection<Integer> inboundIds) throws SQLException {
        if (inboundIds == null || inboundIds.isEmpty()) {
            return List.of();
        }

        List<Integer> ids = inboundIds.stream()
            .filter(id -> id != null && id > 0)
            .distinct()
            .toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < ids.size(); i++) {
            placeholders.add("?");
        }

        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE ii.inbound_id IN (" + placeholders + ") ORDER BY ii.inbound_id, ii.id",
            ITEM_MAPPER, ids.toArray());
    }

    /**
     * 按商品聚合采购入库明细，计算加权平均入库成本。
     *
     * @return 商品ID -> 加权平均入库成本
     * @throws SQLException 数据库操作异常
     */
    public Map<Integer, BigDecimal> findAverageUnitCostByProductId() throws SQLException {
        Map<Integer, BigDecimal> averageCosts = new HashMap<>();
        String sql = "SELECT product_id, SUM(unit_price * quantity) / SUM(quantity) AS avg_unit_cost " +
            "FROM purchase_inbound_items " +
            "WHERE product_id > 0 AND quantity > 0 " +
            "GROUP BY product_id";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                BigDecimal averageCost = rs.getBigDecimal("avg_unit_cost");
                if (averageCost != null) {
                    averageCosts.put(rs.getInt("product_id"), averageCost);
                }
            }
        }
        return averageCosts;
    }

    /**
     * 根据订单明细ID查找入库明细
     *
     * @param orderItemId 订单明细ID
     * @return 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInboundItem> findByOrderItemId(int orderItemId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE ii.order_item_id = ? ORDER BY ii.id", ITEM_MAPPER, orderItemId);
    }

    /**
     * 根据商品ID查找入库明细
     *
     * @param productId 商品ID
     * @return 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public List<PurchaseInboundItem> findByProductId(int productId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE ii.product_id = ? ORDER BY ii.id DESC", ITEM_MAPPER, productId);
    }

    /**
     * 根据入库ID和商品ID查找明细
     *
     * @param inboundId 入库ID
     * @param productId 商品ID
     * @return 采购入库明细对象，如果未找到返回null
     * @throws SQLException 数据库操作异常
     */
    public PurchaseInboundItem findByInboundAndProduct(int inboundId, int productId) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS + FROM_JOIN +
            " WHERE ii.inbound_id = ? AND ii.product_id = ?", ITEM_MAPPER, inboundId, productId);
    }

    /**
     * 插入新采购入库明细
     *
     * @param item 采购入库明细对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insert(PurchaseInboundItem item) throws SQLException {
        long id = executeInsertReturnId(
            "INSERT INTO purchase_inbound_items (inbound_id, order_item_id, product_id, quantity, unit_price, total_price) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            item.inboundId, item.orderItemId, item.productId, item.quantity, item.unitPrice, item.totalPrice);
        item.id = (int) id;
        return id > 0;
    }

    /**
     * 在指定事务连接上插入采购入库明细
     *
     * @param conn 事务连接
     * @param item 采购入库明细对象
     * @return 是否插入成功
     * @throws SQLException 数据库操作异常
     */
    public boolean insertWithConnection(Connection conn, PurchaseInboundItem item) throws SQLException {
        String sql = "INSERT INTO purchase_inbound_items (inbound_id, order_item_id, product_id, quantity, " +
            "unit_price, total_price) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.inboundId);
            pstmt.setInt(2, item.orderItemId);
            pstmt.setInt(3, item.productId);
            pstmt.setInt(4, item.quantity);
            pstmt.setBigDecimal(5, item.unitPrice);
            pstmt.setBigDecimal(6, item.totalPrice);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 更新采购入库明细
     *
     * @param item 采购入库明细对象
     * @return 是否更新成功
     * @throws SQLException 数据库操作异常
     */
    public boolean update(PurchaseInboundItem item) throws SQLException {
        return executeUpdate(
            "UPDATE purchase_inbound_items SET quantity = ?, unit_price = ?, total_price = ? WHERE id = ?",
            item.quantity, item.unitPrice, item.totalPrice, item.id) > 0;
    }

    /**
     * 删除采购入库明细
     *
     * @param id 明细ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM purchase_inbound_items WHERE id = ?", id) > 0;
    }

    /**
     * 根据入库ID删除所有明细
     *
     * @param inboundId 入库ID
     * @return 是否删除成功
     * @throws SQLException 数据库操作异常
     */
    public boolean deleteByInboundId(int inboundId) throws SQLException {
        return executeUpdate("DELETE FROM purchase_inbound_items WHERE inbound_id = ?", inboundId) > 0;
    }

    /**
     * 批量插入采购入库明细
     *
     * @param items 采购入库明细列表
     * @throws SQLException 数据库操作异常
     */
    public void batchInsert(List<PurchaseInboundItem> items) throws SQLException {
        batchUpdate(
            "INSERT INTO purchase_inbound_items (inbound_id, order_item_id, product_id, quantity, unit_price, total_price) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
            items.stream()
                .map(item -> new Object[]{
                    item.inboundId, item.orderItemId, item.productId,
                    item.quantity, item.unitPrice, item.totalPrice})
                .toList());
    }

}
