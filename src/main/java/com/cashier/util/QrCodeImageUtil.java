package com.cashier.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.scene.image.WritableImage;

/** 将支付渠道返回的二维码内容渲染为 JavaFX 图像。 */
public final class QrCodeImageUtil {
    private QrCodeImageUtil() {
    }

    public static WritableImage create(String content, int size) throws WriterException {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }
        BitMatrix matrix = createMatrix(content, size);
        WritableImage image = new WritableImage(size, size);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.getPixelWriter().setArgb(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }

    static BitMatrix createMatrix(String content, int size) throws WriterException {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("二维码内容不能为空");
        }
        return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
    }
}
