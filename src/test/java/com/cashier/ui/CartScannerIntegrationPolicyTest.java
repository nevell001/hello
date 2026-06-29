package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CartScannerIntegrationPolicyTest {

    @Test
    @DisplayName("收银台必须注册并注销扫码焦点目标")
    void cartRegistersAndDisposesScannerFocusTarget() throws Exception {
        String cartController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));
        String mainController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MainController.java"
        ));

        assertTrue(cartController.contains("registerScannerFocusTarget();"));
        assertTrue(cartController.contains("unregisterFocusTarget(scannerFocusTarget)"));
        assertTrue(cartController.contains("public void dispose()"));
        assertTrue(mainController.contains("root.getProperties().put(\"controller\", controller)"));
        assertTrue(mainController.contains("cartController.dispose();"));
    }

    @Test
    @DisplayName("扫码加购必须精确匹配条码或商品编码")
    void scannerUsesExactBarcodeOrProductCodeMatch() throws Exception {
        String cartController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));

        assertTrue(cartController.contains("addScannedProductToCart(String scanText)"));
        assertTrue(cartController.contains("matchesExactScanCode(Product product, String scanText)"));
        assertTrue(cartController.contains("product.barcode.equalsIgnoreCase(scanText)"));
        assertTrue(cartController.contains("product.productCode.equalsIgnoreCase(scanText)"));
        assertTrue(cartController.contains("cart.scan.multiple_matches"));
    }

    @Test
    @DisplayName("扫码加购后应选中商品并抑制硬件抖动重复扫码")
    void scannerHighlightsAddedItemAndSuppressesJitter() throws Exception {
        String cartController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));

        assertTrue(cartController.contains("DUPLICATE_SCAN_SUPPRESSION_MILLIS = 300"));
        assertTrue(cartController.contains("isDuplicateSuccessfulScan(normalizedScanText)"));
        assertTrue(cartController.contains("rememberSuccessfulScan(normalizedScanText)"));
        assertTrue(cartController.contains("selectCartItem(product.name)"));
        assertTrue(cartController.contains("cartTable.getSelectionModel().clearAndSelect(i)"));
        assertTrue(cartController.contains("cartTable.scrollTo(i)"));
        assertTrue(cartController.contains("cart.scan.duplicate_ignored"));
    }
}
