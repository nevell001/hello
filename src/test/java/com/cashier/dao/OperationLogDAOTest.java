package com.cashier.dao;

import com.cashier.model.OperationLog;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogDAOTest extends DatabaseTestBase {

    @BeforeEach
    void setUp() throws Exception {
        if (!isInitialized()) {
            initTestDatabase();
        }
        clearTestData();
    }

    @Test
    void persistsCompleteAuditRecordUsingEpochTimestamp() throws Exception {
        OperationLog log = new OperationLog();
        log.operation = "LOGIN";
        log.category = "AUTH";
        log.result = "FAILURE";
        log.logLevel = "WARN";
        log.details = "用户名不存在";
        log.affectedRecords = 0;

        assertTrue(OperationLogDAO.insert(log));
        List<OperationLog> result = OperationLogDAO.findAll();

        assertEquals(1, result.size());
        assertEquals("AUTH", result.get(0).category);
        assertEquals("FAILURE", result.get(0).result);
        assertEquals(log.timestamp.getTime(), result.get(0).timestamp.getTime());
    }
}
