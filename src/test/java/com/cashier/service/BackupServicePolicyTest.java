package com.cashier.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackupServicePolicyTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("恢复 ZIP 时拒绝越权路径")
    void restoreZipRejectsPathTraversal() {
        IOException exception = assertThrows(IOException.class,
            () -> BackupService.resolveZipEntryPath(tempDir, "../config/database.properties"));

        assertEquals("备份文件包含非法路径: ../config/database.properties", exception.getMessage());
    }

    @Test
    @DisplayName("恢复 ZIP 时只允许写入目标目录内")
    void restoreZipAllowsSafeRelativePath() throws Exception {
        Path resolved = BackupService.resolveZipEntryPath(tempDir, "data/products.csv");

        assertEquals(tempDir.resolve("data/products.csv").toAbsolutePath().normalize(), resolved);
    }
}
