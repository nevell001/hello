package com.cashier.util;

import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 货币工具测试：格式化、解析与货币切换。
 */
@DisplayName("货币工具测试")
class CurrencyUtilTest extends DatabaseTestBase {

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

    @AfterAll
    static void restoreCurrency() {
        // 恢复默认货币，避免污染其他测试
        CurrencyUtil.setCurrency("CNY");
    }

    @Test
    @DisplayName("默认人民币格式：符号、千分位、两位小数")
    void defaultFormatIsCNY() {
        assertEquals("¥", CurrencyUtil.getSymbol());
        assertEquals("CNY", CurrencyUtil.getCode());
        assertEquals("¥1,234.56", CurrencyUtil.format(1234.56));
        assertEquals("¥0.00", CurrencyUtil.format(BigDecimal.ZERO));
        assertEquals("¥0.00", CurrencyUtil.format((BigDecimal) null));
        assertEquals("¥100.00", CurrencyUtil.format(100L));
    }

    @Test
    @DisplayName("纯数字格式化不含货币符号")
    void formatNumberOnlyHasNoSymbol() {
        assertEquals("1,234.56", CurrencyUtil.formatNumberOnly(1234.56));
        assertEquals("0.00", CurrencyUtil.formatNumberOnly(0));
    }

    @Test
    @DisplayName("解析货币字符串为金额，非法输入返回 0")
    void parseCurrencyString() {
        assertEquals(1234.56, CurrencyUtil.parse("¥1,234.56"), 0.001);
        assertEquals(99.90, CurrencyUtil.parse("$99.90"), 0.001);
        assertEquals(-5.5, CurrencyUtil.parse("-5.50"), 0.001);
        assertEquals(0, CurrencyUtil.parse(""), 0.001);
        assertEquals(0, CurrencyUtil.parse("abc"), 0.001);
        assertEquals(0, CurrencyUtil.parse(null), 0.001);
    }

    @Test
    @DisplayName("切换货币后符号更新，不支持的代码被拒绝")
    void setCurrencyUpdatesSymbol() {
        assertTrue(CurrencyUtil.setCurrency("USD"));
        assertEquals("$", CurrencyUtil.getSymbol());
        assertEquals("USD", CurrencyUtil.getCode());

        assertFalse(CurrencyUtil.setCurrency("XXX"));

        assertTrue(CurrencyUtil.setCurrency("JPY"));
        assertEquals("JPY", CurrencyUtil.getCode());

        Map<String, CurrencyUtil.CurrencyInfo> supported = CurrencyUtil.getSupportedCurrencies();
        assertTrue(supported.containsKey("CNY"));
        assertTrue(supported.containsKey("USD"));
        assertFalse(supported.containsKey("XXX"));
    }
}
