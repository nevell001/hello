package com.cashier.dao;

import com.cashier.model.InventoryCheck;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("库存盘点数据访问对象测试")
class InventoryCheckDAOTest extends DatabaseTestBase {

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    @Test
    @DisplayName("生成盘点单号时基于数据库已有最大序号递增")
    void testGenerateNextCheckNoUsesDatabaseMaxSequence() throws SQLException {
        InventoryCheck existing = new InventoryCheck();
        existing.checkNo = "IC202606170001";
        existing.checkDate = "2026-06-17";
        existing.checkType = "full";
        existing.status = "checking";
        existing.operator = "admin";

        InventoryCheckDAO.insert(existing);

        String nextCheckNo = InventoryCheckDAO.generateNextCheckNo("2026-06-17");

        assertEquals("IC202606170002", nextCheckNo);
    }
}
