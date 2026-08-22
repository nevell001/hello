package com.cashier.dao;

import com.cashier.model.Transaction;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionDAOTest extends DatabaseTestBase {

    private Transaction insertTransaction(String id, String timestamp, String paymentMethod) throws Exception {
        Transaction transaction = new Transaction(
            id, timestamp, List.of(), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        transaction.paymentMethod = paymentMethod;
        transaction.operatorUsername = "op";
        transaction.operatorName = "操作员";
        assertTrue(TransactionDAO.insert(transaction));
        return transaction;
    }

    @Test
    @DisplayName("插入并按ID查询交易")
    void insertAndFindById() throws Exception {
        insertTransaction("T-DAO-001", "2026-08-22 10:00:00", "现金");

        Transaction found = TransactionDAO.findById("T-DAO-001");
        assertNotNull(found);
        assertEquals("现金", found.paymentMethod);
    }

    @Test
    @DisplayName("最近交易按时间倒序返回")
    void findRecent() throws Exception {
        insertTransaction("T-REC-001", "2026-08-22 10:00:00", "现金");
        insertTransaction("T-REC-002", "2026-08-22 11:00:00", "微信");

        List<Transaction> recent = TransactionDAO.findRecent(10);
        assertEquals(2, recent.size());
    }

    @Test
    @DisplayName("按日期范围与支付方式查询")
    void findByDateRangeAndPaymentMethod() throws Exception {
        insertTransaction("T-RNG-001", "2026-08-22 10:00:00", "现金");

        assertEquals(1, TransactionDAO.findByDateRange("2026-08-22 00:00:00", "2026-08-22 23:59:59").size());
        assertEquals(1, TransactionDAO.findByPaymentMethod("现金").size());
        assertEquals(0, TransactionDAO.findByPaymentMethod("支付宝").size());
    }

    @Test
    @DisplayName("统计聚合查询")
    void statistics() throws Exception {
        insertTransaction("T-STAT-001", "2026-08-22 10:00:00", "现金");

        assertEquals(1, TransactionDAO.getTransactionCount("2026-08-22 00:00:00", "2026-08-22 23:59:59"));
        assertEquals(0, BigDecimal.TEN.compareTo(BigDecimal.valueOf(
            TransactionDAO.getTotalRevenue("2026-08-22 00:00:00", "2026-08-22 23:59:59"))));
        assertNotNull(TransactionDAO.getStatistics("2026-08-22 00:00:00", "2026-08-22 23:59:59"));
        assertNotNull(TransactionDAO.getTopProducts(10));
        assertNotNull(TransactionDAO.getPaymentMethodStats());
    }

    @Test
    @DisplayName("不存在的交易返回null")
    void findMissingReturnsNull() throws Exception {
        assertNull(TransactionDAO.findById("T-MISSING"));
    }
}
