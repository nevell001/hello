package com.cashier.ui;

import com.cashier.i18n.I18nManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteConfirmationPolicyTest {

    @Test
    @DisplayName("带名称删除确认必须替换商品名称占位符")
    void namedDeleteConfirmationFormatsNamePlaceholder() throws Exception {
        String baseController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/base/BaseController.java"
        ));

        assertTrue(baseController.contains("i18n.get(\"dialog.delete.confirm_with_name\", name)"));
        assertFalse(baseController.contains("String.format(i18n.get(\"dialog.delete.confirm_with_name\")"));
    }

    @Test
    @DisplayName("删除确认文案使用 MessageFormat 占位符")
    void deleteConfirmationMessageUsesMessageFormatPlaceholder() {
        I18nManager i18n = I18nManager.getInstance();
        Locale previousLocale = i18n.getCurrentLocale();

        try {
            i18n.setLocale(Locale.SIMPLIFIED_CHINESE);
            assertEquals("确定要删除“测试商品”吗？",
                i18n.get("dialog.delete.confirm_with_name", "测试商品"));
        } finally {
            i18n.setLocale(previousLocale);
        }
    }
}
