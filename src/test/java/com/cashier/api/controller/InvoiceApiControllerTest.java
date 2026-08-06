package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceApiControllerTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("空请求体从交易创建发票返回 400")
    void createFromTransactionWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/invoices/from-transaction");
        InvoiceApiController.createFromTransaction(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体手工创建发票返回 400")
    void createManualWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/invoices/manual");
        InvoiceApiController.createManual(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体作废发票返回 400")
    void voidInvoiceWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/invoices/INV-1/void")
            .withPathParam("id", "INV-1");
        InvoiceApiController.voidInvoice(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("空请求体记录打印返回 400")
    void recordPrintWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/invoices/INV-2/print")
            .withPathParam("id", "INV-2");
        InvoiceApiController.recordPrint(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }

    @Test
    @DisplayName("设置销售方信息成功")
    void setSellerInfoSucceeds() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/invoices/seller-info")
            .withBody(Map.of("name", "测试公司", "taxId", "91330100"));
        InvoiceApiController.setSellerInfo(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("空请求体设置销售方信息返回 400")
    void setSellerInfoWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/invoices/seller-info");
        InvoiceApiController.setSellerInfo(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
    }
}
