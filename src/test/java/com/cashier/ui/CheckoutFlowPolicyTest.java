package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckoutFlowPolicyTest {

    @Test
    @DisplayName("所有桌面结算入口只加载事务化购物车")
    void desktopCheckoutEntrypointsUseTransactionalCartFlow() throws Exception {
        String mainController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MainController.java"
        ));
        String posModeController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PosModeController.java"
        ));

        assertFalse(mainController.contains("CheckoutView.fxml"));
        assertTrue(mainController.contains("/com/cashier/view/CartView.fxml"));
        assertTrue(posModeController.contains("/com/cashier/view/CartView.fxml"));
        assertFalse(Files.exists(Path.of(
            "src/main/java/com/cashier/controller/CheckoutController.java"
        )));
        assertFalse(Files.exists(Path.of(
            "src/main/resources/com/cashier/view/CheckoutView.fxml"
        )));
    }

    @Test
    @DisplayName("购物车结算必须委托统一事务服务")
    void cartCheckoutDelegatesToTransactionService() throws Exception {
        String cartController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));

        assertTrue(cartController.contains("TransactionService.executeTransaction("));
        assertFalse(cartController.contains("DataService.saveInventory("));
        assertFalse(cartController.contains("DataService.saveMembers("));
    }
}
