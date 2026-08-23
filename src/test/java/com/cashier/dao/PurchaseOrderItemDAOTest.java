package com.cashier.dao;

import com.cashier.model.PurchaseOrderItem;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseOrderItemDAOTest extends DatabaseTestBase {

    private final PurchaseOrderItemDAORefactored purchaseOrderItemDAO = DAOFactory.getInstance().getPurchaseOrderItemDAO();

    @Test
    @DisplayName("插入并按订单查询明细")
    void insertAndFindByOrder() throws Exception {
        PurchaseOrderItem item = new PurchaseOrderItem(1, 1, "测试商品", 2, BigDecimal.valueOf(10));
        item.totalPrice = BigDecimal.valueOf(20);

        assertTrue(purchaseOrderItemDAO.insert(item));
        assertTrue(item.id > 0);
        assertEquals(1, purchaseOrderItemDAO.findByOrderId(1).size());
        assertEquals(1, purchaseOrderItemDAO.findByOrder(1).size());
    }

    @Test
    @DisplayName("按商品与订单组合查询")
    void findByOrderAndProduct() throws Exception {
        PurchaseOrderItem item = new PurchaseOrderItem(2, 7, "商品B", 1, BigDecimal.valueOf(5));
        item.totalPrice = BigDecimal.valueOf(5);
        purchaseOrderItemDAO.insert(item);

        assertTrue(purchaseOrderItemDAO.findByOrderAndProduct(2, 7) != null);
        assertEquals(1, purchaseOrderItemDAO.findByProductId(7).size());
    }
}
