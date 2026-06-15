package com.cashier.util;

import com.cashier.util.DatabaseConnectionHelper.DiagnosticResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库连接诊断工具测试
 */
public class DatabaseConnectionHelperTest {

    @Test
    public void testDiagnoseConnection() {
        DiagnosticResult result = DatabaseConnectionHelper.diagnoseConnection();

        assertNotNull(result);
        assertNotNull(result.getFullMessage());
    }

    @Test
    public void testExtractHostPort() throws Exception {
        Method method = DatabaseConnectionHelper.class.getDeclaredMethod("extractHostPort", String.class);
        method.setAccessible(true);

        assertEquals("localhost:3306",
            method.invoke(null, "jdbc:mysql://localhost:3306/lisuan_system?useSSL=false"));
        assertEquals("192.168.1.100:3307",
            method.invoke(null, "jdbc:mysql://192.168.1.100:3307/dbname"));
        assertEquals("db.example.com",
            method.invoke(null, "jdbc:mysql://db.example.com/cashier"));
    }

    public static void main(String[] args) {
        System.out.println("=== 数据库连接诊断工具 ===\n");

        DiagnosticResult result = DatabaseConnectionHelper.diagnoseConnection();

        System.out.println("═══════════════════════════════════════");
        System.out.println("       数据库连接诊断报告");
        System.out.println("═══════════════════════════════════════\n");

        if (result.success) {
            System.out.println("✅ 状态：连接成功");
            System.out.println("\n数据库连接正常，可以正常使用收银系统。");
        } else {
            System.out.println("❌ 状态：连接失败");
            System.out.println("\n【错误信息】");
            System.out.println(result.errorMessage);
            System.out.println("\n【解决方案】");
            System.out.println(result.solution);
        }

        System.out.println("\n═══════════════════════════════════════");
    }
}
