package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.TransactionDAO;
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

class ReportApiControllerTest extends DatabaseTestBase {

    private void insertTransaction(String id, String timestamp, String paymentMethod) throws Exception {
        Transaction transaction = new Transaction(
            id, timestamp, List.of(),
            BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);
        transaction.paymentMethod = paymentMethod;
        transaction.operatorUsername = "op";
        transaction.operatorName = "操作员";
        assertTrue(TransactionDAO.insert(transaction));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("日报返回成功")
    void dailySalesReturnsSuccess() throws Exception {
        insertTransaction("R-DAILY-001", "2026-08-06 12:00:00", "现金");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/daily-sales")
            .withQueryParam("date", "2026-08-06");
        ReportApiController.dailySales(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("月报返回成功")
    void monthlySalesReturnsSuccess() throws Exception {
        insertTransaction("R-MONTH-001", "2026-08-06 12:00:00", "微信");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/monthly-sales")
            .withQueryParam("month", "2026-08");
        ReportApiController.monthlySales(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("商品销售排行返回成功")
    void topProductsReturnsSuccess() throws Exception {
        insertTransaction("R-TOP-001", "2026-08-06 12:00:00", "现金");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/top-products");
        ReportApiController.topProducts(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("支付方式统计返回成功")
    void paymentMethodsReturnsSuccess() throws Exception {
        insertTransaction("R-PAY-001", "2026-08-06 12:00:00", "支付宝");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/reports/payment-methods");
        ReportApiController.paymentMethods(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }
}
