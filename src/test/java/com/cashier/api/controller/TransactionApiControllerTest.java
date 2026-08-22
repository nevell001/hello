package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.TransactionDAORefactored;
import com.cashier.model.Transaction;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionApiControllerTest extends DatabaseTestBase {

    private final TransactionDAORefactored transactionDAO = DAOFactory.getInstance().getTransactionDAO();

    private Transaction insertTransaction(String id) throws Exception {
        Transaction transaction = new Transaction(
            id, "2026-08-06 12:00:00", List.of(),
            BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        transaction.paymentMethod = "现金";
        transaction.operatorUsername = "op";
        transaction.operatorName = "操作员";
        assertTrue(transactionDAO.insert(transaction));
        return transaction;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("交易列表返回最近记录")
    void listReturnsTransactions() throws Exception {
        insertTransaction("T-LIST-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions");
        TransactionApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Integer) response(ctx).get("total") >= 1);
    }

    @Test
    @DisplayName("按日期范围筛选交易")
    void listByDateRange() throws Exception {
        insertTransaction("T-DATE-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions")
            .withQueryParam("startDate", "2026-08-06")
            .withQueryParam("endDate", "2026-08-06");
        TransactionApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Integer) response(ctx).get("total") >= 1);
    }

    @Test
    @DisplayName("按支付方式筛选交易")
    void listByPaymentMethod() throws Exception {
        insertTransaction("T-PAY-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions")
            .withQueryParam("paymentMethod", "现金");
        TransactionApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Integer) response(ctx).get("total") >= 1);
    }

    @Test
    @DisplayName("获取交易详情")
    void getExistingTransaction() throws Exception {
        Transaction saved = insertTransaction("T-GET-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions/T-GET-001")
            .withPathParam("id", saved.transactionId);
        TransactionApiController.get(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("不存在的交易返回 404")
    void getMissingTransactionReturns404() {
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions/NOPE")
            .withPathParam("id", "NOPE");
        TransactionApiController.get(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("今日统计返回成功")
    void todayStatsReturnsSuccess() throws Exception {
        insertTransaction("T-STAT-001");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/transactions/today-stats");
        TransactionApiController.todayStats(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }
}
