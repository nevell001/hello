package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySelectionFeedbackPolicyTest {

    @Test
    @DisplayName("商品管理空选择操作应给出警告提示")
    void inventoryActionsWarnWhenNoProductSelected() throws Exception {
        String inventoryController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryController.java"
        ));

        assertTrue(inventoryController.contains("showWarning(i18n.get(\"runtime.select_product_first\"))"));
        assertTrue(inventoryController.contains("if (selected.isEmpty())"));
        assertTrue(inventoryController.contains("Product selected = getSelectedItem(inventoryTable);"));
    }

    @Test
    @DisplayName("分类和单位管理空选择操作应给出警告提示")
    void categoryAndUnitActionsWarnWhenNoRowSelected() throws Exception {
        String inventoryController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryController.java"
        ));

        assertTrue(inventoryController.contains("Category sel = categoryTable.getSelectionModel().getSelectedItem();"));
        assertTrue(inventoryController.contains("Unit sel = unitTable.getSelectionModel().getSelectedItem();"));
        assertTrue(countOccurrences(inventoryController,
            "showWarning(i18n.get(\"runtime.select_product_first\"))") >= 5);
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
