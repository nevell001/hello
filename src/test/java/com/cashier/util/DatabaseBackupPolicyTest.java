package com.cashier.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseBackupPolicyTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("备份命令使用 JDBC URL 中配置的真实数据库名")
    void databaseNameIsParsedFromJdbcUrl() {
        assertEquals("lisuan_system", DatabaseManager.getDatabaseNameFromUrl(
            "jdbc:mysql://localhost:3306/lisuan_system?useSSL=false"));
        assertEquals("store_pos", DatabaseManager.getDatabaseNameFromUrl(
            "jdbc:mysql://db.example.com/store_pos"));
        assertEquals("branch_01", DatabaseManager.getDatabaseNameFromUrl(
            "jdbc:mysql://127.0.0.1:3307/branch_01?serverTimezone=Asia/Shanghai"));
    }

    @Test
    @DisplayName("无法解析库名时使用安全默认值")
    void databaseNameFallsBackToDefault() {
        assertEquals("lisuan_system", DatabaseManager.getDatabaseNameFromUrl(null));
        assertEquals("lisuan_system", DatabaseManager.getDatabaseNameFromUrl(""));
        assertEquals("lisuan_system", DatabaseManager.getDatabaseNameFromUrl("jdbc:mysql://localhost:3306/"));
    }

    @Test
    @DisplayName("空 SQL 文件不能被当成有效备份")
    void emptySqlBackupIsInvalid() throws Exception {
        File emptyFile = Files.createFile(tempDir.resolve("empty.sql")).toFile();
        File sqlFile = tempDir.resolve("backup.sql").toFile();
        Files.writeString(sqlFile.toPath(), "CREATE TABLE example (id INT);");

        assertFalse(DatabaseManager.isValidSqlBackupFile(emptyFile));
        assertFalse(DatabaseManager.isValidSqlBackupFile(tempDir.resolve("missing.sql").toFile()));
        assertTrue(DatabaseManager.isValidSqlBackupFile(sqlFile));
    }

    @Test
    @DisplayName("备份文件名前缀只保留安全字符")
    void backupFilePrefixIsSafe() {
        assertEquals("branch_01", DatabaseManager.sanitizeBackupFilePrefix("branch_01"));
        assertEquals("store_pos_prod", DatabaseManager.sanitizeBackupFilePrefix("store/pos prod"));
        assertEquals("lisuan_system", DatabaseManager.sanitizeBackupFilePrefix(" "));
    }
}
