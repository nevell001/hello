package com.cashier.util;

import com.google.zxing.common.BitMatrix;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QrCodeImageUtilTest {

    @Test
    @DisplayName("支付内容可渲染为指定尺寸二维码")
    void rendersPaymentContent() throws Exception {
        BitMatrix matrix = QrCodeImageUtil.createMatrix("weixin://pay/test-order", 180);

        assertEquals(180, matrix.getWidth());
        assertEquals(180, matrix.getHeight());
    }

    @Test
    @DisplayName("空支付内容不能生成二维码")
    void rejectsBlankContent() {
        assertThrows(IllegalArgumentException.class, () -> QrCodeImageUtil.createMatrix(" ", 180));
    }
}
