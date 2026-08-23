package com.cashier.dao;

import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("系统设置数据访问对象测试")
class SystemSettingsDAORefactoredTest extends DatabaseTestBase {

    private final SystemSettingsDAORefactored settingsDAO =
        DAOFactory.getInstance().getSystemSettingsDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    @Test
    @DisplayName("设置键值后可读取，重复设置使用 upsert 更新")
    void setAndGetSettingUpserts() throws SQLException {
        assertTrue(settingsDAO.setSetting("taxRate", "0.06"));
        assertEquals("0.06", settingsDAO.getSetting("taxRate"));

        assertTrue(settingsDAO.setSetting("taxRate", "0.13"));
        assertEquals("0.13", settingsDAO.getSetting("taxRate"));
        assertEquals(1, settingsDAO.getAllSettings().size());
    }

    @Test
    @DisplayName("删除设置后读取返回 null")
    void deleteSettingRemovesKey() throws SQLException {
        settingsDAO.setSetting("printerModel", "GP-58");
        assertTrue(settingsDAO.deleteSetting("printerModel"));
        assertNull(settingsDAO.getSetting("printerModel"));
        assertFalse(settingsDAO.deleteSetting("not-exists"));
    }

    @Test
    @DisplayName("获取所有设置返回键值映射")
    void getAllSettingsReturnsAll() throws SQLException {
        settingsDAO.setSetting("a", "1");
        settingsDAO.setSetting("b", "2");

        Map<String, String> all = settingsDAO.getAllSettings();

        assertEquals("1", all.get("a"));
        assertEquals("2", all.get("b"));
        assertNull(all.get("missing"));
    }
}
