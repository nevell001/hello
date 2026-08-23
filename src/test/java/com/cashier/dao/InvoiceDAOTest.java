package com.cashier.dao;

import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("发票数据访问对象测试")
class InvoiceDAOTest extends DatabaseTestBase {

    private final InvoiceDAORefactored invoiceDAO = DAOFactory.getInstance().getInvoiceDAO();

    @Test
    @DisplayName("分页查询发票时支持日期和状态过滤")
    void testFindPageWithDateAndStatusFilters() throws SQLException {
        Invoice oldIssued = createInvoice("INV-OLD", "ISSUED", LocalDate.of(2026, 1, 1));
        Invoice recentIssued = createInvoice("INV-RECENT", "ISSUED", LocalDate.of(2026, 7, 10));
        Invoice recentVoided = createInvoice("INV-VOIDED", "VOIDED", LocalDate.of(2026, 7, 11));

        invoiceDAO.insert(oldIssued);
        invoiceDAO.insert(recentIssued);
        invoiceDAO.insert(recentVoided);

        var page = invoiceDAO.findPage(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 31),
            "ISSUED",
            1,
            10
        );

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getData().size());
        assertEquals("INV-RECENT", page.getData().get(0).invoiceId);
        assertEquals(1, page.getData().get(0).items.size());
    }

    @Test
    @DisplayName("分页查询发票时按开票时间倒序并限制数量")
    void testFindPageUsesLimitAndNewestFirst() throws SQLException {
        invoiceDAO.insert(createInvoice("INV-1", "ISSUED", LocalDate.of(2026, 7, 1)));
        invoiceDAO.insert(createInvoice("INV-2", "ISSUED", LocalDate.of(2026, 7, 2)));
        invoiceDAO.insert(createInvoice("INV-3", "ISSUED", LocalDate.of(2026, 7, 3)));

        var page = invoiceDAO.findPage(null, null, null, 1, 2);

        assertEquals(3, page.getTotal());
        assertEquals(2, page.getData().size());
        assertEquals("INV-3", page.getData().get(0).invoiceId);
        assertEquals("INV-2", page.getData().get(1).invoiceId);
    }

    private Invoice createInvoice(String invoiceId, String status, LocalDate createDate) {
        Invoice invoice = new Invoice();
        invoice.invoiceId = invoiceId;
        invoice.invoiceCode = "CODE";
        invoice.invoiceNumber = invoiceId;
        invoice.transactionId = "TX-" + invoiceId;
        invoice.buyerName = "测试客户";
        invoice.sellerName = "测试商户";
        invoice.totalAmount = BigDecimal.TEN;
        invoice.taxAmount = BigDecimal.ONE;
        invoice.finalAmount = BigDecimal.valueOf(11);
        invoice.taxRate = BigDecimal.valueOf(0.13);
        invoice.createTime = Date.from(createDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        invoice.createBy = "admin";
        invoice.status = status;
        invoice.items = List.of(new InvoiceItem("测试商品", 1, BigDecimal.TEN));
        invoice.items.forEach(item -> item.calculateAmount(invoice.taxRate));
        return invoice;
    }
}
