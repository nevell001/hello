package com.cashier.dao;

import com.cashier.model.PurchaseOrder;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseOrderDAOTest extends DatabaseTestBase {

    private PurchaseOrder insertOrder(String orderNo) throws Exception {
        PurchaseOrder order = new PurchaseOrder();
        order.orderNo = orderNo;
        order.supplierId = 1;
        order.purchaseDate = "2026-08-22";
        order.totalAmount = BigDecimal.valueOf(100);
        order.status = "pending";
        order.purchaser = "采购员";
        assertTrue(PurchaseOrderDAO.insert(order));
        return order;
    }

    @Test
    @DisplayName("插入并按单号/ID查询")
    void insertAndFind() throws Exception {
        PurchaseOrder order = insertOrder("PO-DAO-001");

        assertTrue(order.id > 0);
        assertNotNull(PurchaseOrderDAO.findById(order.id));
        assertEquals("PO-DAO-001", PurchaseOrderDAO.findByOrderNo("PO-DAO-001").orderNo);
    }

    @Test
    @DisplayName("按状态与供应商查询")
    void findByStatusAndSupplier() throws Exception {
        insertOrder("PO-DAO-002");

        assertEquals(1, PurchaseOrderDAO.findByStatus("pending").size());
        assertEquals(1, PurchaseOrderDAO.findBySupplier(1).size());
    }

    @Test
    @DisplayName("更新状态")
    void updateStatus() throws Exception {
        PurchaseOrder order = insertOrder("PO-DAO-003");

        assertTrue(PurchaseOrderDAO.updateStatus(order.id, "approved"));
        assertEquals("approved", PurchaseOrderDAO.findById(order.id).status);
    }

    @Test
    @DisplayName("不存在的单号返回null")
    void findMissingReturnsNull() throws Exception {
        assertNull(PurchaseOrderDAO.findByOrderNo("PO-MISSING"));
    }
}
