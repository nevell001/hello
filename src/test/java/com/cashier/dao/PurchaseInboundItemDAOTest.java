package com.cashier.dao;

import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("采购入库明细数据访问对象测试")
class PurchaseInboundItemDAOTest extends DatabaseTestBase {

    @Test
    @DisplayName("按商品聚合加权平均入库成本")
    void testFindAverageUnitCostByProductId() throws SQLException {
        insertInboundItem(1, 1, 10, BigDecimal.valueOf(8));
        insertInboundItem(1, 1, 30, BigDecimal.valueOf(12));
        insertInboundItem(1, 2, 5, BigDecimal.valueOf(20));
        insertInboundItem(1, 3, 0, BigDecimal.valueOf(99));

        var averageCosts = PurchaseInboundItemDAO.findAverageUnitCostByProductId();

        assertEquals(0, BigDecimal.valueOf(11).compareTo(averageCosts.get(1)));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(averageCosts.get(2)));
        assertFalse(averageCosts.containsKey(3));
    }

    private void insertInboundItem(int inboundId, int productId, int quantity, BigDecimal unitPrice) throws SQLException {
        try (Connection conn = getTestConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO purchase_inbound_items " +
                 "(inbound_id, order_item_id, product_id, quantity, unit_price, total_price) " +
                 "VALUES (?, ?, ?, ?, ?, ?)")) {

            pstmt.setInt(1, inboundId);
            pstmt.setInt(2, productId);
            pstmt.setInt(3, productId);
            pstmt.setInt(4, quantity);
            pstmt.setBigDecimal(5, unitPrice);
            pstmt.setBigDecimal(6, unitPrice.multiply(BigDecimal.valueOf(quantity)));
            pstmt.executeUpdate();
        }
    }
}
