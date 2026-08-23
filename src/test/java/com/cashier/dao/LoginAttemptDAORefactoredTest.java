package com.cashier.dao;

import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("登录尝试数据访问对象测试")
class LoginAttemptDAORefactoredTest extends DatabaseTestBase {

    private final LoginAttemptDAORefactored loginAttemptDAO =
        DAOFactory.getInstance().getLoginAttemptDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    @Test
    @DisplayName("连续失败按用户分别累计并触发锁定")
    void failedAttemptsIncrementAndLock() {
        assertEquals(1, loginAttemptDAO.recordFailedAttempt("alice", 3, 60_000));
        assertEquals(2, loginAttemptDAO.recordFailedAttempt("alice", 3, 60_000));
        assertEquals(3, loginAttemptDAO.recordFailedAttempt("alice", 3, 60_000));

        assertEquals(1, loginAttemptDAO.recordFailedAttempt("bob", 3, 60_000));
        assertEquals(1, loginAttemptDAO.getAttemptCount("bob"));

        assertTrue(loginAttemptDAO.isLocked("alice"));
        assertTrue(loginAttemptDAO.getRemainingLockoutSeconds("alice") > 0);
        assertFalse(loginAttemptDAO.isLocked("bob"));
    }

    @Test
    @DisplayName("登录成功后重置尝试次数并解除锁定")
    void resetAttemptsUnlocks() {
        loginAttemptDAO.recordFailedAttempt("carol", 2, 60_000);
        loginAttemptDAO.recordFailedAttempt("carol", 2, 60_000);
        assertTrue(loginAttemptDAO.isLocked("carol"));

        loginAttemptDAO.resetAttempts("carol");

        assertFalse(loginAttemptDAO.isLocked("carol"));
        assertEquals(0, loginAttemptDAO.getAttemptCount("carol"));
    }
}
