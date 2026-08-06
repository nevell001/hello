package com.cashier.api.sync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncMessageTest {

    @Test
    @DisplayName("创建事件消息")
    void createMessage() {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", 42);

        SyncMessage message = SyncMessage.create(SyncEventType.PRODUCT_UPDATED, data);

        assertEquals(SyncEventType.PRODUCT_UPDATED.name(), message.type);
        assertTrue(message.timestamp > 0);
        assertEquals(42, message.data.get("productId"));
    }

    @Test
    @DisplayName("从终端创建的消息携带来源终端")
    void createFromTerminalAddsSource() {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", 7);

        SyncMessage message = SyncMessage.createFromTerminal(
            SyncEventType.INVENTORY_CHANGED, "POS-01", data);

        assertEquals("POS-01", message.data.get("sourceTerminal"));
        assertEquals(7, message.data.get("productId"));
    }

    @Test
    @DisplayName("默认构造与字段访问")
    void defaultConstructorRoundTrip() {
        SyncMessage message = new SyncMessage();
        message.type = "CUSTOM";
        message.timestamp = 123456L;
        message.data = Map.of("k", "v");

        assertNotNull(message);
        assertEquals("CUSTOM", message.type);
        assertEquals(123456L, message.timestamp);
        assertEquals("v", message.data.get("k"));
    }
}
