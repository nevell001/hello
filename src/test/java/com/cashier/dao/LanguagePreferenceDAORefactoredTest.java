package com.cashier.dao;

import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("语言偏好数据访问对象测试")
class LanguagePreferenceDAORefactoredTest extends DatabaseTestBase {

    private final LanguagePreferenceDAORefactored preferenceDAO =
        DAOFactory.getInstance().getLanguagePreferenceDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS language_preferences (
                    username VARCHAR(50) PRIMARY KEY,
                    language_tag VARCHAR(20),
                    currency_code VARCHAR(10) DEFAULT 'CNY',
                    updated_at BIGINT
                )
                """);
        }
    }

    @AfterEach
    void cleanup() throws SQLException {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM language_preferences");
        }
    }

    @Test
    @DisplayName("用户无偏好时回退到全局默认与系统默认")
    void languagePreferenceFallbackChain() throws SQLException {
        assertEquals("zh-CN", preferenceDAO.getLanguagePreference("nobody"));

        preferenceDAO.setLanguagePreference("default", "en-US");
        assertEquals("en-US", preferenceDAO.getLanguagePreference("nobody"));

        preferenceDAO.setLanguagePreference("alice", "zh-TW");
        assertEquals("zh-TW", preferenceDAO.getLanguagePreference("alice"));
    }

    @Test
    @DisplayName("货币偏好默认 CNY 且可更新")
    void currencyPreferenceDefaultsAndUpdates() throws SQLException {
        assertEquals("CNY", preferenceDAO.getCurrencyPreference("bob"));

        assertTrue(preferenceDAO.setCurrencyPreference("bob", "USD"));
        assertEquals("USD", preferenceDAO.getCurrencyPreference("bob"));

        assertTrue(preferenceDAO.setCurrencyPreference("bob", "JPY"));
        assertEquals("JPY", preferenceDAO.getCurrencyPreference("bob"));
    }
}
