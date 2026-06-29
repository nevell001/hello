package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportFeedbackPolicyTest {

    @Test
    @DisplayName("报表和交易导出成功应同步状态栏成功级别")
    void exportSuccessUpdatesStatusBar() throws Exception {
        List<String> controllerFiles = List.of(
            "src/main/java/com/cashier/controller/StatisticsController.java",
            "src/main/java/com/cashier/controller/InventoryReportController.java",
            "src/main/java/com/cashier/controller/PurchaseReportController.java",
            "src/main/java/com/cashier/controller/ProfitReportController.java",
            "src/main/java/com/cashier/controller/TransactionController.java"
        );

        for (String file : controllerFiles) {
            String controller = Files.readString(Path.of(file));
            assertTrue(controller.contains("if (filePath != null)"), file);
            assertTrue(controller.contains("StatusBarManager.updateSuccess"), file);
            assertTrue(controller.contains("get(\"success.export\")"), file);
        }
    }

    @Test
    @DisplayName("报表和交易导出失败仍应同步状态栏错误级别")
    void exportFailuresUpdateStatusBar() throws Exception {
        List<String> controllerFiles = List.of(
            "src/main/java/com/cashier/controller/StatisticsController.java",
            "src/main/java/com/cashier/controller/InventoryReportController.java",
            "src/main/java/com/cashier/controller/PurchaseReportController.java",
            "src/main/java/com/cashier/controller/ProfitReportController.java",
            "src/main/java/com/cashier/controller/TransactionController.java"
        );

        for (String file : controllerFiles) {
            String controller = Files.readString(Path.of(file));
            assertTrue(controller.contains("showError("), file);
            assertTrue(controller.contains("StatusBarManager.updateError(message)"), file);
        }
    }
}
