package com.cashier.dao;

import com.cashier.model.PaymentOrder;
import com.cashier.util.DatabaseManager;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("支付订单数据访问对象测试")
class PaymentDAOTest extends DatabaseTestBase {

    @BeforeEach
    void setUpPaymentTables() throws SQLException {
        PaymentDAO.createTable();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM refund_records");
            stmt.execute("DELETE FROM payment_orders");
        }
    }

    @Test
    @DisplayName("查询待支付订单时按创建时间倒序并限制数量")
    void testFindWaitingOrdersUsesLimitAndNewestFirst() throws SQLException {
        PaymentDAO.insert(createOrder("PAY-OLD", "ORDER-OLD", 1_000L, PaymentOrder.PaymentStatus.CREATED));
        PaymentDAO.insert(createOrder("PAY-MIDDLE", "ORDER-MIDDLE", 2_000L, PaymentOrder.PaymentStatus.WAITING));
        PaymentDAO.insert(createOrder("PAY-NEW", "ORDER-NEW", 3_000L, PaymentOrder.PaymentStatus.CREATED));
        PaymentDAO.insert(createOrder("PAY-SUCCESS", "ORDER-SUCCESS", 4_000L, PaymentOrder.PaymentStatus.SUCCESS));

        var orders = PaymentDAO.findWaitingOrders(2);

        assertEquals(2, orders.size());
        assertEquals("PAY-NEW", orders.get(0).paymentId);
        assertEquals("PAY-MIDDLE", orders.get(1).paymentId);
    }

    private PaymentOrder createOrder(
            String paymentId,
            String merchantOrderNo,
            long createTime,
            PaymentOrder.PaymentStatus status
    ) {
        PaymentOrder order = new PaymentOrder();
        order.paymentId = paymentId;
        order.transactionId = "TX-" + paymentId;
        order.merchantOrderNo = merchantOrderNo;
        order.paymentType = PaymentOrder.PaymentType.QRCODE_PAY;
        order.channel = PaymentOrder.PaymentChannel.WECHAT;
        order.amount = BigDecimal.TEN;
        order.status = status;
        order.createTime = Date.from(Instant.ofEpochMilli(createTime));
        order.expireTime = Date.from(Instant.now().plusSeconds(3600));
        order.terminalId = "POS-1";
        order.operator = "admin";
        return order;
    }
}
