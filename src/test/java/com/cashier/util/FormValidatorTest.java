package com.cashier.util;

import com.cashier.util.FormValidator.ValidationRule;
import com.cashier.util.FormValidator.ValidationResult;
import com.cashier.util.FormValidator.Rules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FormValidator 单元测试
 * 测试表单验证规则和解析方法
 */
@DisplayName("表单验证工具测试")
class FormValidatorTest {

    @Test
    @DisplayName("非空验证规则 - 有效值")
    void testNotEmptyRule_Valid() {
        assertTrue(Rules.NOT_EMPTY.test("test value"));
        assertTrue(Rules.NOT_EMPTY.test("  value  "));
    }

    @Test
    @DisplayName("非空验证规则 - 无效值")
    void testNotEmptyRule_Invalid() {
        assertFalse(Rules.NOT_EMPTY.test(""));
        assertFalse(Rules.NOT_EMPTY.test("   "));
        assertFalse(Rules.NOT_EMPTY.test(null));
    }

    @Test
    @DisplayName("手机号验证规则 - 有效手机号")
    void testPhoneRule_Valid() {
        assertTrue(Rules.PHONE.test("13800138000"));
        assertTrue(Rules.PHONE.test("15912345678"));
        assertTrue(Rules.PHONE.test("18888888888"));
    }

    @Test
    @DisplayName("手机号验证规则 - 无效手机号")
    void testPhoneRule_Invalid() {
        assertFalse(Rules.PHONE.test("12800138000"));  // 12开头无效
        assertFalse(Rules.PHONE.test("1380013800"));   // 少一位
        assertFalse(Rules.PHONE.test("138001380000")); // 多一位
        assertFalse(Rules.PHONE.test("abc12345678")); // 含字母
    }

    @Test
    @DisplayName("手机号验证规则 - 空值通过")
    void testPhoneRule_Empty() {
        assertTrue(Rules.PHONE.test(""));
        assertTrue(Rules.PHONE.test(null));
    }

    @Test
    @DisplayName("邮箱验证规则 - 有效邮箱")
    void testEmailRule_Valid() {
        assertTrue(Rules.EMAIL.test("test@example.com"));
        assertTrue(Rules.EMAIL.test("user.name@domain.co.uk"));
        assertTrue(Rules.EMAIL.test("admin+tag@example.org"));
    }

    @Test
    @DisplayName("邮箱验证规则 - 无效邮箱")
    void testEmailRule_Invalid() {
        assertFalse(Rules.EMAIL.test("invalid"));
        assertFalse(Rules.EMAIL.test("@example.com"));
        assertFalse(Rules.EMAIL.test("test@"));
        assertFalse(Rules.EMAIL.test("test @example.com"));
    }

    @Test
    @DisplayName("金额验证规则 - 有效金额")
    void testAmountRule_Valid() {
        assertTrue(Rules.AMOUNT.test("0"));
        assertTrue(Rules.AMOUNT.test("123"));
        assertTrue(Rules.AMOUNT.test("123.45"));
        assertTrue(Rules.AMOUNT.test("0.99"));
    }

    @Test
    @DisplayName("金额验证规则 - 无效金额")
    void testAmountRule_Invalid() {
        assertFalse(Rules.AMOUNT.test("123.456"));  // 超过两位小数
        assertFalse(Rules.AMOUNT.test("0123"));     // 前导零
        assertFalse(Rules.AMOUNT.test("abc"));
    }

    @Test
    @DisplayName("数量验证规则 - 有效数量")
    void testQuantityRule_Valid() {
        assertTrue(Rules.QUANTITY.test("0"));
        assertTrue(Rules.QUANTITY.test("123"));
    }

    @Test
    @DisplayName("数量验证规则 - 无效数量")
    void testQuantityRule_Invalid() {
        assertFalse(Rules.QUANTITY.test("123.45"));  // 小数
        assertFalse(Rules.QUANTITY.test("0123"));     // 前导零
        assertFalse(Rules.QUANTITY.test("-1"));       // 负数
    }

    @Test
    @DisplayName("折扣验证规则 - 有效折扣")
    void testDiscountRule_Valid() {
        assertTrue(Rules.DISCOUNT.test("0"));
        assertTrue(Rules.DISCOUNT.test("5"));
        assertTrue(Rules.DISCOUNT.test("9.5"));
        assertTrue(Rules.DISCOUNT.test("10"));
    }

    @Test
    @DisplayName("折扣验证规则 - 无效折扣")
    void testDiscountRule_Invalid() {
        assertFalse(Rules.DISCOUNT.test("10.1"), "10.1 should be invalid");  // 超过10
        assertFalse(Rules.DISCOUNT.test("-1"), "Negative should be invalid");   // 负数
        assertFalse(Rules.DISCOUNT.test("abc"), "Letters should be invalid");
    }

    @Test
    @DisplayName("折扣验证规则 - 边界值")
    void testDiscountRule_Boundary() {
        assertTrue(Rules.DISCOUNT.test("0"), "0 should be valid");
        assertTrue(Rules.DISCOUNT.test("10"), "10 should be valid");
        assertTrue(Rules.DISCOUNT.test("10.0"), "10.0 should be valid"); // 等于10
        assertTrue(Rules.DISCOUNT.test("9.99"), "9.99 should be valid");
        assertFalse(Rules.DISCOUNT.test("10.01"), "10.01 should be invalid"); // 超过10
    }

    @Test
    @DisplayName("百分比验证规则 - 有效百分比")
    void testPercentageRule_Valid() {
        assertTrue(Rules.PERCENTAGE.test("0"));
        assertTrue(Rules.PERCENTAGE.test("50"));
        assertTrue(Rules.PERCENTAGE.test("100"));
        assertTrue(Rules.PERCENTAGE.test("50.5%"));
    }

    @Test
    @DisplayName("百分比验证规则 - 无效百分比")
    void testPercentageRule_Invalid() {
        assertFalse(Rules.PERCENTAGE.test("101"));
        assertFalse(Rules.PERCENTAGE.test("-1"));
        assertFalse(Rules.PERCENTAGE.test("50.5.5"));
    }

    @Test
    @DisplayName("密码验证规则 - 有效密码")
    void testPasswordRule_Valid() {
        assertTrue(Rules.PASSWORD.test("123456"));
        assertTrue(Rules.PASSWORD.test("abcdef"));
    }

    @Test
    @DisplayName("密码验证规则 - 无效密码")
    void testPasswordRule_Invalid() {
        assertFalse(Rules.PASSWORD.test("12345"));
        assertFalse(Rules.PASSWORD.test(""));
        assertFalse(Rules.PASSWORD.test(null));
    }

    @Test
    @DisplayName("长度验证规则")
    void testLengthRule() {
        ValidationRule lengthRule = Rules.length(3, 10);
        assertTrue(lengthRule.test("abc"));
        assertTrue(lengthRule.test("1234567890"));
        assertFalse(lengthRule.test("ab"));
        assertFalse(lengthRule.test("12345678901"));
    }

    @Test
    @DisplayName("自定义正则验证规则")
    void testCustomRegexRule() {
        ValidationRule zipCodeRule = Rules.regex("^\\d{6}$", "请输入6位邮编");
        assertTrue(zipCodeRule.test("123456"));
        assertFalse(zipCodeRule.test("12345"));
        assertFalse(zipCodeRule.test("1234567"));
    }

    @Test
    @DisplayName("验证结果对象")
    void testValidationResult() {
        ValidationResult validResult = new ValidationResult(true, "");
        ValidationResult invalidResult = new ValidationResult(false, "错误信息");

        assertTrue(validResult.isValid());
        assertEquals("", validResult.getErrorMessage());

        assertFalse(invalidResult.isValid());
        assertEquals("错误信息", invalidResult.getErrorMessage());
    }

    @Test
    @DisplayName("parseDouble - 正常解析")
    void testParseDouble_Valid() {
        assertEquals(123.45, FormValidator.parseDouble("123.45"), 0.001);
        assertEquals(0, FormValidator.parseDouble("0"), 0.001);
        assertEquals(-100, FormValidator.parseDouble("-100"), 0.001);
    }

    @Test
    @DisplayName("parseDouble - 空值返回默认值")
    void testParseDouble_Empty() {
        assertEquals(10.0, FormValidator.parseDouble(null, 10.0), 0.001);
        assertEquals(20.0, FormValidator.parseDouble("", 20.0), 0.001);
        assertEquals(30.0, FormValidator.parseDouble("   ", 30.0), 0.001);
    }

    @Test
    @DisplayName("parseDouble - 无效值返回默认值")
    void testParseDouble_Invalid() {
        assertEquals(5.0, FormValidator.parseDouble("abc", 5.0), 0.001);
        assertEquals(5.0, FormValidator.parseDouble("123.45.67", 5.0), 0.001);
    }

    @Test
    @DisplayName("parseDouble - 非空版本")
    void testParseDouble_NotNull() {
        assertEquals(123.45, FormValidator.parseDouble("123.45"), 0.001);
        assertThrows(IllegalArgumentException.class, () -> FormValidator.parseDouble(null));
        assertThrows(IllegalArgumentException.class, () -> FormValidator.parseDouble(""));
        assertThrows(IllegalArgumentException.class, () -> FormValidator.parseDouble("abc"));
    }

    @Test
    @DisplayName("parseInt - 正常解析")
    void testParseInt_Valid() {
        assertEquals(123, FormValidator.parseInt("123"));
        assertEquals(0, FormValidator.parseInt("0"));
        assertEquals(-100, FormValidator.parseInt("-100"));
    }

    @Test
    @DisplayName("parseInt - 空值返回默认值")
    void testParseInt_Empty() {
        assertEquals(10, FormValidator.parseInt(null, 10));
        assertEquals(20, FormValidator.parseInt("", 20));
        assertEquals(30, FormValidator.parseInt("   ", 30));
    }

    @Test
    @DisplayName("parseInt - 无效值返回默认值")
    void testParseInt_Invalid() {
        assertEquals(5, FormValidator.parseInt("abc", 5));
        assertEquals(5, FormValidator.parseInt("123.45", 5));
    }

    @Test
    @DisplayName("parseInt - 非空版本")
    void testParseInt_NotNull() {
        assertEquals(123, FormValidator.parseInt("123"));
        assertThrows(IllegalArgumentException.class, () -> FormValidator.parseInt(null));
        assertThrows(IllegalArgumentException.class, () -> FormValidator.parseInt(""));
        assertThrows(IllegalArgumentException.class, () -> FormValidator.parseInt("abc"));
    }

    @Test
    @DisplayName("parseLong - 正常解析")
    void testParseLong_Valid() {
        assertEquals(12345678901L, FormValidator.parseLong("12345678901"));
        assertEquals(0L, FormValidator.parseLong("0"));
        assertEquals(-100L, FormValidator.parseLong("-100"));
    }

    @Test
    @DisplayName("parseLong - 空值返回默认值")
    void testParseLong_Empty() {
        assertEquals(100L, FormValidator.parseLong(null, 100));
        assertEquals(200L, FormValidator.parseLong("", 200));
        assertEquals(300L, FormValidator.parseLong("   ", 300));
    }

    @Test
    @DisplayName("parseLong - 无效值返回默认值")
    void testParseLong_Invalid() {
        assertEquals(50L, FormValidator.parseLong("abc", 50));
        assertEquals(50L, FormValidator.parseLong("123.45", 50));
    }

    @Test
    @DisplayName("parseLong - 非空版本")
    void testParseLong_NotNull() {
        assertEquals(12345678901L, FormValidator.parseLong("12345678901"));
        assertThrows(IllegalArgumentException.class, () -> FormValidator.parseLong(null));
        assertThrows(IllegalArgumentException.class, () -> FormValidator.parseLong(""));
        assertThrows(IllegalArgumentException.class, () -> FormValidator.parseLong("abc"));
    }
}
