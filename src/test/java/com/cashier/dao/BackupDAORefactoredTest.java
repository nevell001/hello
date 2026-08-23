package com.cashier.dao;

import com.cashier.model.BackupConfig;
import com.cashier.model.BackupRecord;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("备份数据访问对象测试")
class BackupDAORefactoredTest extends DatabaseTestBase {

    private final BackupDAORefactored backupDAO = DAOFactory.getInstance().getBackupDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS backup_records (
                    backup_id VARCHAR(50) PRIMARY KEY,
                    backup_type VARCHAR(20),
                    target VARCHAR(20),
                    file_name VARCHAR(100),
                    local_path VARCHAR(200),
                    remote_path VARCHAR(200),
                    file_size BIGINT,
                    status VARCHAR(20),
                    create_time DATETIME,
                    start_time DATETIME,
                    finish_time DATETIME,
                    duration_seconds INT,
                    content_type VARCHAR(20),
                    scope VARCHAR(20),
                    operator VARCHAR(50),
                    remark VARCHAR(200),
                    error_message VARCHAR(500),
                    checksum VARCHAR(50),
                    auto_backup BOOLEAN
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS backup_config (
                    id INT PRIMARY KEY,
                    auto_backup_enabled BOOLEAN,
                    target VARCHAR(20),
                    content_type VARCHAR(20),
                    backup_interval_hours INT,
                    retention_days INT,
                    max_backup_count INT,
                    local_backup_path VARCHAR(100),
                    last_backup_time DATETIME,
                    next_backup_time DATETIME,
                    update_time DATETIME
                )
                """);
        }
    }

    @AfterEach
    void cleanup() throws SQLException {
        try (Connection conn = getTestConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM backup_records");
            stmt.execute("DELETE FROM backup_config");
        }
    }

    private BackupRecord createRecord(String id, String status) {
        BackupRecord record = new BackupRecord();
        record.backupId = id;
        record.backupType = BackupRecord.BackupType.MANUAL;
        record.target = BackupRecord.BackupTarget.LOCAL;
        record.fileName = id + ".sql";
        record.localPath = "/tmp/" + id + ".sql";
        record.fileSize = 1024;
        record.status = BackupRecord.BackupStatus.valueOf(status);
        record.contentType = BackupRecord.BackupContentType.FULL;
        record.scope = BackupRecord.BackupScope.FULL;
        record.operator = "admin";
        record.autoBackup = false;
        return record;
    }

    @Test
    @DisplayName("备份记录插入后可查最近与全部")
    void backupRecordCrudAndQueries() throws SQLException {
        assertTrue(backupDAO.insert(createRecord("B001", "SUCCESS")));
        assertTrue(backupDAO.insert(createRecord("B002", "SUCCESS")));
        assertTrue(backupDAO.insert(createRecord("B003", "FAILED")));

        BackupRecord found = backupDAO.findById("B002");
        assertNotNull(found);
        assertEquals("/tmp/B002.sql", found.localPath);

        List<BackupRecord> recent = backupDAO.findRecent(2);
        assertEquals(2, recent.size());

        List<BackupRecord> successful = backupDAO.findSuccessful();
        assertEquals(2, successful.size());

        assertEquals(3, backupDAO.countBackups());

        assertTrue(backupDAO.updateStatus("B003", BackupRecord.BackupStatus.SUCCESS));
        assertEquals(3, backupDAO.findSuccessful().size());
    }

    @Test
    @DisplayName("配置不存在时自动创建默认值并可更新")
    void configCreatedByDefaultAndUpdatable() throws SQLException {
        BackupConfig config = backupDAO.getConfig();
        assertNotNull(config);

        config.autoBackupEnabled = true;
        config.backupIntervalHours = 6;
        config.retentionDays = 15;
        config.maxBackupCount = 10;
        config.localBackupPath = "/opt/lisuan/backup";
        assertTrue(backupDAO.saveConfig(config));

        BackupConfig reloaded = backupDAO.getConfig();
        assertTrue(reloaded.autoBackupEnabled);
        assertEquals(6, reloaded.backupIntervalHours);
        assertEquals("/opt/lisuan/backup", reloaded.localBackupPath);
    }
}
