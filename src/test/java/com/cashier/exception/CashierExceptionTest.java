package com.cashier.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CashierExceptionTest {

    @Test
    @DisplayName("基础异常默认错误码与级别")
    void baseExceptionDefaults() {
        CashierException exception = new CashierException("出错了");

        assertEquals("出错了", exception.getMessage());
        assertEquals("UNKNOWN", exception.getErrorCode());
        assertEquals(CashierException.ErrorLevel.ERROR, exception.getErrorLevel());
    }

    @Test
    @DisplayName("带错误码的异常")
    void exceptionWithCode() {
        CashierException exception = new CashierException("库存不足", "INSUFFICIENT_STOCK");

        assertEquals("INSUFFICIENT_STOCK", exception.getErrorCode());
    }

    @Test
    @DisplayName("数据库异常保留错误类型与 SQL")
    void databaseExceptionKeepsTypeAndSql() {
        DatabaseException exception = new DatabaseException(
            "连接失败", DatabaseException.DbErrorType.CONNECTION_FAILED,
            "SELECT 1", new IllegalStateException("boom"));

        assertEquals(DatabaseException.DbErrorType.CONNECTION_FAILED, exception.getDbErrorType());
        assertEquals("SELECT 1", exception.getSql());
        assertEquals("boom", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("数据库异常工厂方法")
    void databaseExceptionFactories() {
        Throwable cause = new RuntimeException("db down");

        DatabaseException connectionFailed = DatabaseException.connectionFailed(cause);
        assertEquals(DatabaseException.DbErrorType.CONNECTION_FAILED, connectionFailed.getDbErrorType());
        assertSame(cause, connectionFailed.getCause());

        DatabaseException duplicate = DatabaseException.duplicateKey("products.name");
        assertEquals(DatabaseException.DbErrorType.DUPLICATE_KEY, duplicate.getDbErrorType());
        assertNull(duplicate.getCause());
    }

    @Test
    @DisplayName("认证异常工厂方法携带正确类型")
    void authenticationExceptionFactories() {
        AuthenticationException invalid = AuthenticationException.invalidCredentials();
        assertEquals(AuthenticationException.AuthErrorType.INVALID_CREDENTIALS, invalid.getAuthErrorType());

        AuthenticationException locked = AuthenticationException.accountLocked();
        assertEquals(AuthenticationException.AuthErrorType.ACCOUNT_LOCKED, locked.getAuthErrorType());

        AuthenticationException denied = AuthenticationException.permissionDenied("管理员");
        assertEquals(AuthenticationException.AuthErrorType.PERMISSION_DENIED, denied.getAuthErrorType());
    }

    @Test
    @DisplayName("业务异常保留错误类型与关联实体")
    void businessExceptionKeepsTypeAndEntity() {
        Object product = new Object();
        BusinessException exception = new BusinessException(
            "数据校验失败", BusinessException.BusinessErrorType.DATA_VALIDATION_FAILED, product);

        assertEquals(BusinessException.BusinessErrorType.DATA_VALIDATION_FAILED, exception.getBusinessErrorType());
        assertSame(product, exception.getRelatedEntity());
    }
}
