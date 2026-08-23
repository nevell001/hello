package com.cashier.service;

import com.cashier.constant.FXConstants;
import com.cashier.dao.DAOFactory;
import com.cashier.model.Product;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据服务测试：设置持久化、主题偏好、库存加载。
 */
@DisplayName("数据服务测试")
class DataServiceTest extends DatabaseTestBase {

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS theme_preferences (
                    username VARCHAR(50) PRIMARY KEY,
                    theme_name VARCHAR(50),
                    updated_at BIGINT
                )
                """);
        }
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM theme_preferences");
        }
    }

    @Test
    @DisplayName("设置保存后可回读，空库提供默认值")
    void settingsPersistAndDefault() throws SQLException {
        Map<String, String> defaults = DataService.loadSettings();
        assertEquals("0.0", defaults.get("taxRate"));
        assertEquals("0", defaults.get("transactionCount"));

        Map<String, String> settings = new HashMap<>();
        settings.put("taxRate", "0.13");
        settings.put("receiptFooter", "谢谢惠顾");
        DataService.saveSettings(settings);

        Map<String, String> loaded = DataService.loadSettings();
        assertEquals("0.13", loaded.get("taxRate"));
        assertEquals("谢谢惠顾", loaded.get("receiptFooter"));
    }

    @Test
    @DisplayName("主题偏好默认值、保存回读与用户回退默认")
    void themePreferencePersistsAndFallsBack() {
        assertEquals(FXConstants.DEFAULT_THEME, DataService.loadThemePreference("nobody"));

        DataService.saveThemePreference("default", "dark");
        assertEquals("dark", DataService.loadThemePreference("nobody"));

        DataService.saveThemePreference("alice", "light");
        assertEquals("light", DataService.loadThemePreference("alice"));
        // 旧主题名归一化
        DataService.saveThemePreference("bob", "intellij");
        assertEquals("lisuan", DataService.loadThemePreference("bob"));
    }

    @Test
    @DisplayName("库存加载返回有界商品列表")
    void loadInventoryReturnsProducts() throws SQLException {
        for (int i = 1; i <= 3; i++) {
            Product product = new Product();
            product.productCode = "INV" + i;
            product.name = "库存商品" + i;
            product.price = BigDecimal.valueOf(10 + i);
            product.quantity = 10;
            product.category = "测试";
            product.barcode = "B" + i;
            product.unit = "个";
            product.cost = BigDecimal.valueOf(5);
            assertTrue(DAOFactory.getInstance().getProductDAO().insert(product));
        }

        Map<String, Product> inventory = DataService.loadInventory();

        assertFalse(inventory.isEmpty());
        assertEquals("库存商品1", inventory.get("库存商品1").name);
        assertEquals(10, inventory.get("库存商品2").quantity);
    }
}
