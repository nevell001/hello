package com.cashier.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PurchaseInboundItemTest {

    @Test
    void exposesProductNameAndQuantityForTableBinding() {
        PurchaseInboundItem item = new PurchaseInboundItem(
            1, 2, 3, "测试商品", 8, new BigDecimal("12.50")
        );

        assertEquals("测试商品", item.getProductName());
        assertEquals(8, item.getQuantity());
    }
}
