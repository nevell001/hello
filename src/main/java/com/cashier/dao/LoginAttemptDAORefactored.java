package com.cashier.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

/**
 * 登录尝试数据访问对象（重构版）
 * 持久化登录失败次数和锁定状态，按用户名分别跟踪。
 * 保持原签名：不抛出检查异常，SQL 异常记录日志并返回安全默认值。
 */
public class LoginAttemptDAORefactored extends BaseDAO {

    /**
     * 记录一次失败登录尝试
     * 如果不存在则插入，存在则递增 attempt_count
     *
     * @param username             用户名
     * @param maxAttempts          最大允许尝试次数
     * @param lockoutDurationMillis 锁定时长（毫秒）
     * @return 更新后的失败次数
     */
    public int recordFailedAttempt(String username, int maxAttempts, long lockoutDurationMillis) {
        String upsertSql = "INSERT INTO login_attempts (username, attempt_count, lockout_until, last_attempt_time) " +
            "VALUES (?, 1, NULL, ?) " +
            "ON DUPLICATE KEY UPDATE attempt_count = attempt_count + 1, last_attempt_time = ?";
        String lockSql = "UPDATE login_attempts SET lockout_until = ? WHERE username = ? AND attempt_count >= ?";

        try (Connection conn = getConnection()) {
            long now = System.currentTimeMillis();

            try (PreparedStatement pstmt = conn.prepareStatement(upsertSql)) {
                pstmt.setString(1, username);
                pstmt.setLong(2, now);
                pstmt.setLong(3, now);
                pstmt.executeUpdate();
            }

            // 如果达到阈值，设置锁定
            try (PreparedStatement pstmt = conn.prepareStatement(lockSql)) {
                pstmt.setLong(1, now + lockoutDurationMillis);
                pstmt.setString(2, username);
                pstmt.setInt(3, maxAttempts);
                pstmt.executeUpdate();
            }

            return getAttemptCount(username);
        } catch (SQLException e) {
            logger.error("记录登录失败尝试异常: username={}", username, e);
            return 0;
        }
    }

    /**
     * 获取当前失败尝试次数
     */
    public int getAttemptCount(String username) {
        String sql = "SELECT attempt_count FROM login_attempts WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("attempt_count");
                }
            }
        } catch (SQLException e) {
            logger.error("获取登录尝试次数异常: username={}", username, e);
        }
        return 0;
    }

    /**
     * 获取锁定结束时间（epoch millis），empty 表示未锁定
     */
    public Optional<Long> getLockoutUntil(String username) {
        String sql = "SELECT lockout_until FROM login_attempts WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long lockoutUntil = rs.getLong("lockout_until");
                    if (!rs.wasNull() && lockoutUntil > 0) {
                        // 检查锁定是否已过期
                        if (Instant.now().toEpochMilli() < lockoutUntil) {
                            return Optional.of(lockoutUntil);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("获取锁定时间异常: username={}", username, e);
        }
        return Optional.empty();
    }

    /**
     * 检查用户是否被锁定
     */
    public boolean isLocked(String username) {
        return getLockoutUntil(username).isPresent();
    }

    /**
     * 获取剩余锁定秒数
     */
    public long getRemainingLockoutSeconds(String username) {
        Optional<Long> lockoutUntil = getLockoutUntil(username);
        if (lockoutUntil.isEmpty()) {
            return 0;
        }
        long remaining = (lockoutUntil.get() - Instant.now().toEpochMilli()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * 重置登录尝试次数（登录成功或锁定过期后调用）
     */
    public void resetAttempts(String username) {
        String sql = "UPDATE login_attempts SET attempt_count = 0, lockout_until = NULL WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("重置登录尝试次数异常: username={}", username, e);
        }
    }

    /**
     * 如果锁定已过期，自动重置
     */
    public void resetIfLockoutExpired(String username) {
        Optional<Long> lockoutUntil = getLockoutUntil(username);
        if (lockoutUntil.isEmpty()) {
            // 可能锁定已过期，检查是否有记录需要重置
            String sql = "UPDATE login_attempts SET attempt_count = 0, lockout_until = NULL " +
                "WHERE username = ? AND lockout_until IS NOT NULL AND lockout_until <= ?";
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setLong(2, Instant.now().toEpochMilli());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.error("检查锁定过期异常: username={}", username, e);
            }
        }
    }
}
