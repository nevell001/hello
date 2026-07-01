package com.cashier.service;

import com.cashier.model.BackupRecord;
import com.cashier.model.BackupConfig;
import com.cashier.dao.BackupDAO;
import com.cashier.i18n.I18nManager;
import com.cashier.util.DatabaseManager;
import com.cashier.api.sync.SyncManager;
import com.cashier.api.sync.SyncEventType;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;

/**
 * 备份服务
 * 支持本地备份和云存储上传
 */
public class BackupService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(BackupService.class);
    
    private static BackupService instance;
    private static BackupConfig config;
    private ScheduledExecutorService scheduler;
    
    private BackupService() {}
    
    public static BackupService getInstance() {
        if (instance == null) {
            instance = new BackupService();
        }
        return instance;
    }
    
    /**
     * 初始化备份服务
     */
    public static void init() {
        try {
            BackupDAO.createTable();
            config = BackupDAO.getConfig();
            
            // 创建备份目录
            if (config.localBackupPath != null) {
                Files.createDirectories(Paths.get(config.localBackupPath));
            }
            
            logger.info("备份服务初始化成功");
        } catch (Exception e) {
            logger.error("备份服务初始化失败", e);
        }
    }
    
    /**
     * 执行备份
     */
    public static BackupRecord executeBackup(BackupRecord.BackupContentType contentType,
                                              BackupRecord.BackupTarget target,
                                              String operator) throws SQLException {
        BackupRecord record = BackupRecord.createManual(contentType, target, operator);
        
        // 保存记录
        BackupDAO.insert(record);
        
        // 开始备份
        record.startTime = new Date();
        BackupDAO.updateStatus(record.backupId, BackupRecord.BackupStatus.RUNNING);
        
        try {
            // 创建备份文件
            String localPath = createBackupFile(record);
            record.localPath = localPath;
            
            // 计算文件大小和校验码
            File backupFile = new File(localPath);
            record.fileSize = backupFile.length();
            record.checksum = calculateChecksum(backupFile);
            
            // 上传到云存储（如果目标不是本地）
            if (target != BackupRecord.BackupTarget.LOCAL) {
                BackupDAO.updateStatus(record.backupId, BackupRecord.BackupStatus.UPLOADING);
                String remotePath = uploadToCloud(record, backupFile);
                record.remotePath = remotePath;
            }
            
            // 完成
            record.finishTime = new Date();
            record.calculateDuration();
            BackupDAO.updateFinish(record.backupId, localPath, record.remotePath, 
                record.fileSize, record.checksum, BackupRecord.BackupStatus.SUCCESS, null);
            
            // 广播备份成功事件
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.BACKUP_SUCCESS,
                Map.of("backupId", record.backupId, "fileSize", record.fileSize));
            
            logger.info("备份完成: {} - {} bytes", record.backupId, record.fileSize);
            
        } catch (Exception e) {
            logger.error("备份失败: {}", record.backupId, e);
            
            BackupDAO.updateFinish(record.backupId, null, null, 0, null, 
                BackupRecord.BackupStatus.FAILED, e.getMessage());
            
            SyncManager.getInstance().broadcastSyncEvent(SyncEventType.BACKUP_FAILED,
                Map.of("backupId", record.backupId, "error", e.getMessage()));
        }
        
        return record;
    }
    
    /**
     * 创建备份文件
     */
    private static String createBackupFile(BackupRecord record) throws IOException {
        String backupDir = config.localBackupPath != null ? config.localBackupPath : "backups";
        Path backupPath = Paths.get(backupDir);
        
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
        }
        
        String fileName = record.fileName;
        Path zipPath = backupPath.resolve(fileName);
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            
            switch (record.contentType) {
                case FULL:
                    // 备份数据库和文件
                    addDatabaseBackup(zos);
                    addFilesBackup(zos, "data/");
                    addConfigBackup(zos);
                    break;
                    
                case DATABASE:
                    addDatabaseBackup(zos);
                    break;
                    
                case FILES:
                    addFilesBackup(zos, "data/");
                    break;
                    
                case CONFIG:
                    addConfigBackup(zos);
                    break;
                    
                case LOGS:
                    addLogsBackup(zos);
                    break;
            }
        }

        if (!Files.exists(zipPath) || Files.size(zipPath) == 0) {
            throw new IOException(I18nManager.getInstance().get("service.backup_file_empty", zipPath));
        }
        
        logger.debug("备份文件创建: {}", zipPath);
        return zipPath.toString();
    }
    
    /**
     * 添加数据库备份
     */
    private static void addDatabaseBackup(ZipOutputStream zos) throws IOException {
        // 创建临时文件用于 SQL 备份
        File tempSqlFile = File.createTempFile("cashier_backup_", ".sql");
        
        try {
            // 使用 DatabaseManager 进行 MySQL 备份
            boolean success = com.cashier.util.DatabaseManager.backup(tempSqlFile);
            
            if (success && tempSqlFile.exists() && tempSqlFile.length() > 0) {
                addToZip(zos, "database/backup.sql", tempSqlFile);
            } else {
                throw new IOException(I18nManager.getInstance().get("service.backup_sql_export_failed"));
            }
        } finally {
            // 删除临时文件
            if (tempSqlFile.exists()) {
                tempSqlFile.delete();
            }
        }
    }
    
    /**
     * 添加文件备份
     */
    private static void addFilesBackup(ZipOutputStream zos, String prefix) throws IOException {
        // 备份数据目录下的文件
        Path dataDir = Paths.get("data");
        
        if (Files.exists(dataDir)) {
            Files.walk(dataDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        String entryName = prefix + dataDir.relativize(path).toString();
                        addToZip(zos, entryName, path.toFile());
                    } catch (IOException e) {
                        logger.warn("备份文件失败: {}", path, e);
                    }
                });
        }
        
        // 备份发票文件
        Path invoiceDir = Paths.get("invoices");
        if (Files.exists(invoiceDir)) {
            Files.walk(invoiceDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    try {
                        addToZip(zos, "invoices/" + invoiceDir.relativize(path).toString(), path.toFile());
                    } catch (IOException e) {
                        logger.warn("备份发票文件失败: {}", path, e);
                    }
                });
        }
    }
    
    /**
     * 添加配置备份
     */
    private static void addConfigBackup(ZipOutputStream zos) throws IOException {
        // 备份配置文件
        Path configDir = Paths.get("config");
        
        if (Files.exists(configDir)) {
            Files.walk(configDir)
                .filter(path -> !Files.isDirectory(path) &&
                    (path.toString().endsWith(".properties") ||
                     path.toString().endsWith(".yaml") ||
                     path.toString().endsWith(".json")))
                .forEach(path -> {
                    try {
                        addToZip(zos, "config/" + configDir.relativize(path).toString(), path.toFile());
                    } catch (IOException e) {
                        logger.warn("备份配置文件失败: {}", path, e);
                    }
                });
        }
    }
    
    /**
     * 添加日志备份
     */
    private static void addLogsBackup(ZipOutputStream zos) throws IOException {
        Path logsDir = Paths.get("logs");
        
        if (Files.exists(logsDir)) {
            Files.walk(logsDir)
                .filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".log"))
                .forEach(path -> {
                    try {
                        addToZip(zos, "logs/" + logsDir.relativize(path).toString(), path.toFile());
                    } catch (IOException e) {
                        logger.warn("备份日志文件失败: {}", path, e);
                    }
                });
        }
    }
    
    /**
     * 添加文件到ZIP
     */
    private static void addToZip(ZipOutputStream zos, String entryName, File file) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }
        
        zos.closeEntry();
    }
    
    /**
     * 上传到云存储
     */
    private static String uploadToCloud(BackupRecord record, File backupFile) throws IOException {
        if (record.target == BackupRecord.BackupTarget.LOCAL) {
            return null;
        }

        throw new IOException("云备份目标 " + record.target.getDisplayName() + " 尚未接入真实上传适配器，已阻止模拟成功");
    }
    
    /**
     * 计算文件MD5校验码
     */
    private static String calculateChecksum(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                md.update(buffer, 0, len);
            }
        }
        
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    /**
     * 恢复备份
     */
    public static boolean restoreBackup(String backupId) throws SQLException, IOException {
        BackupRecord record = BackupDAO.findById(backupId);
        
        if (record == null || !record.status.isSuccess()) {
            logger.warn("无法恢复备份: {}", backupId);
            return false;
        }
        
        if (record.localPath == null || record.localPath.isBlank()) {
            logger.warn("备份文件路径为空: {}", backupId);
            return false;
        }

        File backupFile = new File(record.localPath);
        
        if (!backupFile.exists()) {
            logger.warn("备份文件不存在: {}", record.localPath);
            return false;
        }

        if (record.checksum != null && !record.checksum.isBlank()) {
            try {
                String actualChecksum = calculateChecksum(backupFile);
                if (!record.checksum.equalsIgnoreCase(actualChecksum)) {
                    logger.error("备份校验失败: {}, expected={}, actual={}", backupId, record.checksum, actualChecksum);
                    return false;
                }
            } catch (Exception e) {
                throw new IOException("备份校验失败: " + backupId, e);
            }
        }
        
        Path workspaceRoot = Paths.get("").toAbsolutePath().normalize();
        Path tempRestoreDir = Files.createTempDirectory("cashier_restore_");
        Path databaseBackupFile = null;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entry.isDirectory()) {
                    Path directoryPath = resolveZipEntryPath(workspaceRoot, entryName);
                    Files.createDirectories(directoryPath);
                } else {
                    Path destPath;
                    if ("database/backup.sql".equals(entryName)) {
                        destPath = tempRestoreDir.resolve("backup.sql").normalize();
                        databaseBackupFile = destPath;
                    } else {
                        destPath = resolveZipEntryPath(workspaceRoot, entryName);
                    }

                    Files.createDirectories(destPath.getParent());
                    Files.copy(zis, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
                
                zis.closeEntry();
            }

            if (databaseBackupFile != null && Files.exists(databaseBackupFile)) {
                boolean databaseRestored = DatabaseManager.restore(databaseBackupFile.toFile());
                if (!databaseRestored) {
                    logger.error("数据库备份恢复失败: {}", backupId);
                    return false;
                }
            }
        } finally {
            deleteDirectoryQuietly(tempRestoreDir);
        }
        
        // 广播恢复事件
        SyncManager.getInstance().broadcastSyncEvent(SyncEventType.BACKUP_RESTORED,
            Map.of("backupId", backupId));
        
        logger.info("备份恢复完成: {}", backupId);
        return true;
    }

    static Path resolveZipEntryPath(Path targetRoot, String entryName) throws IOException {
        Path normalizedRoot = targetRoot.toAbsolutePath().normalize();
        Path normalizedPath = normalizedRoot.resolve(entryName).normalize();
        if (!normalizedPath.startsWith(normalizedRoot)) {
            throw new IOException("备份文件包含非法路径: " + entryName);
        }
        return normalizedPath;
    }

    private static void deleteDirectoryQuietly(Path directory) {
        try {
            if (directory == null || !Files.exists(directory)) {
                return;
            }

            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        logger.debug("清理临时恢复目录失败: {}", path, e);
                    }
                });
        } catch (IOException e) {
            logger.debug("清理临时恢复目录失败: {}", directory, e);
        }
    }
    
    /**
     * 清理过期备份
     */
    public static int cleanupExpiredBackups() throws SQLException, IOException {
        int retentionDays = config.retentionDays;
        int deleted = BackupDAO.deleteExpired(retentionDays);
        
        // 删除对应的文件
        Path backupDir = Paths.get(config.localBackupPath);
        if (Files.exists(backupDir)) {
            long cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L;
            
            Files.walk(backupDir)
                .filter(path -> !Files.isDirectory(path))
                .filter(path -> new File(path.toString()).lastModified() < cutoff)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        logger.debug("删除过期备份文件: {}", path);
                    } catch (IOException e) {
                        logger.warn("删除文件失败: {}", path, e);
                    }
                });
        }
        
        if (deleted > 0) {
            logger.info("清理过期备份: {} 个", deleted);
        }
        
        return deleted;
    }
    
    /**
     * 获取配置
     */
    public static BackupConfig getConfig() throws SQLException {
        if (config == null) {
            config = BackupDAO.getConfig();
        }
        return config;
    }
    
    /**
     * 更新配置
     */
    public static void updateConfig(BackupConfig newConfig) throws SQLException {
        BackupDAO.saveConfig(newConfig);
        config = newConfig;
        logger.info("备份配置已更新");
    }
    
    /**
     * 启动自动备份服务
     */
    public void start() {
        try {
            init();
            config = BackupDAO.getConfig();
            
            if (config.autoBackupEnabled && config.backupIntervalHours > 0) {
                scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        if (config.needsBackup()) {
                            executeAutoBackup();
                        }
                    } catch (Exception e) {
                        logger.error("自动备份执行失败", e);
                    }
                }, config.backupIntervalHours, config.backupIntervalHours, TimeUnit.HOURS);
                
                logger.info("自动备份服务已启动，周期: {} 小时", config.backupIntervalHours);
            }
        } catch (Exception e) {
            logger.error("启动自动备份服务失败", e);
        }
    }
    
    /**
     * 停止自动备份服务
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            scheduler = null;
            logger.info("自动备份服务已停止");
        }
    }
    
    /**
     * 执行自动备份
     */
    private void executeAutoBackup() {
        try {
            BackupRecord record = executeBackup(config.contentType, config.target, "system");
            
            if (record.status.isSuccess()) {
                // 更新最后备份时间
                BackupDAO.updateLastBackupTime(new Date());
                
                // 清理过期备份
                cleanupExpiredBackups();
            }
        } catch (Exception e) {
            logger.error("自动备份失败", e);
        }
    }
}
