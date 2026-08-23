package com.cashier.dao;

import com.cashier.model.PurchaseOrderItem;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

/**
 * 采购订单明细数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class PurchaseOrderItemDAORefactored extends BaseDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PurchaseOrderItemDAORefactored.class);

    private static final String SELECT_COLUMNS =
        "id, order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity ";

    private static final RowMapper<PurchaseOrderItem> ITEM_MAPPER = new RowMapper<PurchaseOrderItem>() {
        @Override
        public PurchaseOrderItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.id = rs.getInt("id");
            item.orderId = rs.getInt("order_id");
            item.productId = rs.getInt("product_id");
            item.productName = rs.getString("product_name");
            item.quantity = rs.getInt("quantity");
            item.unitPrice = rs.getBigDecimal("unit_price");
            item.totalPrice = rs.getBigDecimal("total_price");
            item.inboundQuantity = rs.getInt("inbound_quantity");
            return item;
        }
    };

    public PurchaseOrderItem findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM purchase_order_items WHERE id = ?", ITEM_MAPPER, id);
    }

    public List<PurchaseOrderItem> findByOrderId(int orderId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM purchase_order_items WHERE order_id = ? ORDER BY id", ITEM_MAPPER, orderId);
    }

    public List<PurchaseOrderItem> findByOrder(int orderId) throws SQLException {
        return findByOrderId(orderId);
    }

    public List<PurchaseOrderItem> findByOrderIds(Collection<Integer> orderIds) throws SQLException {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        List<Integer> ids = orderIds.stream()
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
        Object[] params = ids.toArray();
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM purchase_order_items WHERE order_id IN (" + placeholders + ") ORDER BY order_id, id",
            ITEM_MAPPER, params);
    }

    public List<PurchaseOrderItem> findByProductId(int productId) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM purchase_order_items WHERE product_id = ? ORDER BY id DESC", ITEM_MAPPER, productId);
    }

    public PurchaseOrderItem findByOrderAndProduct(int orderId, int productId) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM purchase_order_items WHERE order_id = ? AND product_id = ?",
            ITEM_MAPPER, orderId, productId);
    }

    public boolean insert(PurchaseOrderItem item) throws SQLException {
        long id = executeInsertReturnId(
            "INSERT INTO purchase_order_items (order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            item.orderId, item.productId, item.productName, item.quantity,
            item.unitPrice, item.totalPrice, item.inboundQuantity);
        item.id = (int) id;
        return id > 0;
    }

    public boolean update(PurchaseOrderItem item) throws SQLException {
        return executeUpdate(
            "UPDATE purchase_order_items SET product_name = ?, quantity = ?, unit_price = ?, total_price = ?, inbound_quantity = ? " +
                "WHERE id = ?",
            item.productName, item.quantity, item.unitPrice, item.totalPrice, item.inboundQuantity, item.id) > 0;
    }

    public boolean updateInboundQuantity(int id, int inboundQuantity) throws SQLException {
        return executeUpdate(
            "UPDATE purchase_order_items SET inbound_quantity = ? WHERE id = ?", inboundQuantity, id) > 0;
    }

    public boolean increaseInboundQuantity(int id, int delta) throws SQLException {
        return executeUpdate(
            "UPDATE purchase_order_items SET inbound_quantity = inbound_quantity + ? WHERE id = ?", delta, id) > 0;
    }

    public boolean increaseInboundQuantityWithConnection(Connection conn, int id, int delta) throws SQLException {
        String sql = "UPDATE purchase_order_items SET inbound_quantity = inbound_quantity + ? " +
            "WHERE id = ? AND ? > 0 AND inbound_quantity + ? <= quantity";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, delta);
            pstmt.setInt(2, id);
            pstmt.setInt(3, delta);
            pstmt.setInt(4, delta);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean areAllInboundWithConnection(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM purchase_order_items WHERE order_id = ? AND inbound_quantity < quantity";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) == 0;
            }
        }
    }

    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM purchase_order_items WHERE id = ?", id) > 0;
    }

    public boolean deleteByOrderId(int orderId) throws SQLException {
        return executeUpdate("DELETE FROM purchase_order_items WHERE order_id = ?", orderId) > 0;
    }

    public void batchInsert(List<PurchaseOrderItem> items) throws SQLException {
        List<Object[]> params = new ArrayList<>(items.size());
        for (PurchaseOrderItem item : items) {
            params.add(new Object[]{
                item.orderId, item.productId, item.productName, item.quantity,
                item.unitPrice, item.totalPrice, item.inboundQuantity});
        }
        batchUpdate(
            "INSERT INTO purchase_order_items (order_id, product_id, product_name, quantity, unit_price, total_price, inbound_quantity) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)", params);
    }

    public Object[] calculateOrderTotal(int orderId) throws SQLException {
        String sql = "SELECT SUM(quantity) as total_quantity, SUM(total_price) as total_amount " +
            "FROM purchase_order_items WHERE order_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Object[]{rs.getInt("total_quantity"), rs.getBigDecimal("total_amount")};
                }
            }
        }
        return new Object[]{0, BigDecimal.ZERO};
    }
}
