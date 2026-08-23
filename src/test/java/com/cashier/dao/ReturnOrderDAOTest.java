package com.cashier.dao;

import com.cashier.model.ReturnOrder;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReturnOrderDAOTest extends DatabaseTestBase {

    private final ReturnOrderDAORefactored returnOrderDAO = DAOFactory.getInstance().getReturnOrderDAO();

    private ReturnOrder insertReturnOrder(String returnOrderId) {
        ReturnOrder order = new ReturnOrder();
        order.returnOrderId = returnOrderId;
        order.originalTransactionId = "T-1";
        order.returnDate = Instant.now();
        order.totalAmount = BigDecimal.valueOf(50);
        order.status = "PENDING";
        order.operatorName = "操作员";
        assertTrue(returnOrderDAO.insert(order));
        return order;
    }

    @Test
    @DisplayName("插入并按退货单号查询")
    void insertAndFindByReturnOrderId() {
        insertReturnOrder("R-DAO-001");

        ReturnOrder found = returnOrderDAO.findByReturnOrderId("R-DAO-001");
        assertNotNull(found);
        assertEquals("PENDING", found.status);
    }

    @Test
    @DisplayName("按状态查询")
    void findByStatus() {
        insertReturnOrder("R-DAO-002");

        assertEquals(1, returnOrderDAO.findByStatus("PENDING").size());
    }

    @Test
    @DisplayName("更新退货单")
    void update() {
        insertReturnOrder("R-DAO-003");
        // 重新读取以获得持久化 id（insert 不回填 id）
        ReturnOrder order = returnOrderDAO.findByReturnOrderId("R-DAO-003");
        order.status = "APPROVED";

        assertTrue(returnOrderDAO.update(order));
        assertEquals("APPROVED", returnOrderDAO.findByReturnOrderId("R-DAO-003").status);
    }
}
