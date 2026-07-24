package com.cashier.dao;

import com.cashier.model.Member;
import com.cashier.model.PageResult;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * 会员数据访问对象
 * 负责会员相关的数据库操作
 */
public class MemberDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(MemberDAO.class);

    /**
     * 根据ID查找会员
     */
    public static Member findById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return findByIdWithConnection(conn, id);
        }
    }

    /**
     * 使用指定连接根据ID查找会员
     * @param conn 数据库连接
     * @param id 会员ID
     * @return 会员对象，不存在时返回 null
     * @throws SQLException 数据库操作异常
     */
    public static Member findByIdWithConnection(Connection conn, int id) throws SQLException {
        String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday, version FROM members WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMember(rs);
                }
            }
        }

        return null;
    }

    /**
     * 根据手机号查找会员
     */
    public static Member findByPhone(String phone) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            return findByPhoneWithConnection(conn, phone);
        }
    }

    /**
     * 使用指定连接根据手机号查找会员
     * @param conn 数据库连接
     * @param phone 手机号
     * @return 会员对象，不存在时返回 null
     * @throws SQLException 数据库操作异常
     */
    public static Member findByPhoneWithConnection(Connection conn, String phone) throws SQLException {
        String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday, version FROM members WHERE phone = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMember(rs);
                }
            }
        }
        return null;
    }

    /**
     * 查询所有会员
     */
    public static List<Member> findAll() throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday, version FROM members ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                members.add(mapRowToMember(rs));
            }
        }
        return members;
    }

    /**
     * 分页查询会员，用于同步和接口避免一次性加载全部会员。
     */
    public static PageResult<Member> findAll(int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }

        List<Member> members = new ArrayList<>();
        long total = count();
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday, version " +
                     "FROM members ORDER BY name LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, pageSize);
            pstmt.setInt(2, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRowToMember(rs));
                }
            }
        }
        return new PageResult<>(members, pageNum, pageSize, total);
    }

    /**
     * 统计会员总数。
     */
    public static long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM members";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    /**
     * 聚合会员总数、余额和积分，避免统计时加载全部会员。
     */
    public static Map<String, Object> getMemberSummary() throws SQLException {
        String sql = "SELECT COUNT(*) AS total_count, " +
                     "COALESCE(SUM(balance), 0) AS total_balance, " +
                     "COALESCE(SUM(points), 0) AS total_points " +
                     "FROM members";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("totalCount", rs.getLong("total_count"));
                summary.put("totalBalance", rs.getBigDecimal("total_balance"));
                summary.put("totalPoints", rs.getBigDecimal("total_points"));
                return summary;
            }
        }

        Map<String, Object> emptySummary = new HashMap<>();
        emptySummary.put("totalCount", 0L);
        emptySummary.put("totalBalance", BigDecimal.ZERO);
        emptySummary.put("totalPoints", BigDecimal.ZERO);
        return emptySummary;
    }

    /**
     * 按会员等级统计数量。
     */
    public static Map<String, Integer> countByLevel() throws SQLException {
        Map<String, Integer> levelStats = new HashMap<>();
        String sql = "SELECT level, COUNT(*) AS level_count FROM members GROUP BY level";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String level = rs.getString("level");
                levelStats.put(level, rs.getInt("level_count"));
            }
        }
        return levelStats;
    }

    /**
     * 插入新会员
     * 如果会员ID大于0，则使用指定的ID；否则由数据库自动生成ID
     */
    public static boolean insert(Member member) throws SQLException {
        // 自动生成会员编号
        if (member.memberCode == null || member.memberCode.trim().isEmpty()) {
            member.memberCode = generateMemberCode();
        }

        String sql;
        boolean useProvidedId = member.id > 0;

        if (useProvidedId) {
            // 使用用户提供的ID
            sql = "INSERT INTO members (id, member_code, phone, name, points, level, discount, balance, birthday, version) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            // 由数据库自动生成ID
            sql = "INSERT INTO members (member_code, phone, name, points, level, discount, balance, birthday) " +
                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int paramIndex = 1;
            if (useProvidedId) {
                pstmt.setInt(paramIndex++, member.id);
            }

            pstmt.setString(paramIndex++, member.memberCode);
            pstmt.setString(paramIndex++, member.phone);
            pstmt.setString(paramIndex++, member.name);
            pstmt.setBigDecimal(paramIndex++, member.points);
            pstmt.setString(paramIndex++, member.level);
            pstmt.setBigDecimal(paramIndex++, member.discount);
            pstmt.setBigDecimal(paramIndex++, member.balance);
            pstmt.setString(paramIndex++, member.birthday);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0 && !useProvidedId) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        member.id = generatedKeys.getInt(1);
                    }
                }
            }
            return affectedRows > 0;
        }
    }

    /**
     * 生成会员编号
     * @return 会员编号
     */
    private static String generateMemberCode() throws SQLException {
        // 格式: MEM + yyyyMMdd + 4位递增序号
        String dateStr = java.time.LocalDate.now(java.time.ZoneId.systemDefault()).format(com.cashier.util.DateTimeFormats.COMPACT_DATE);
        
        String sql = "SELECT member_code FROM members WHERE member_code LIKE ? ORDER BY member_code DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, "MEM" + dateStr + "%");
            ResultSet rs = pstmt.executeQuery();
            
            int sequence = 1;
            if (rs.next()) {
                String lastCode = rs.getString("member_code");
                // 提取序号部分（最后4位）
                try {
                    String lastSeq = lastCode.substring(lastCode.length() - 4);
                    sequence = Integer.parseInt(lastSeq) + 1;
                } catch (Exception e) {
                    // 如果解析失败，使用默认值
                }
            }
            
            // 格式化为4位数字，前面补0
            return String.format("MEM%s%04d", dateStr, sequence);
        }
    }

    /**
     * 更新会员（使用乐观锁）
     */
    public static boolean update(Member member) throws SQLException {
        String sql = "UPDATE members SET member_code = ?, phone = ?, name = ?, points = ?, level = ?, discount = ?, balance = ?, birthday = ?, version = version + 1 " +
                     "WHERE id = ? AND version = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, member.memberCode);
            pstmt.setString(2, member.phone);
            pstmt.setString(3, member.name);
            pstmt.setBigDecimal(4, member.points);
            pstmt.setString(5, member.level);
            pstmt.setBigDecimal(6, member.discount);
            pstmt.setBigDecimal(7, member.balance);
            pstmt.setString(8, member.birthday);
            pstmt.setInt(9, member.id);
            pstmt.setInt(10, member.version);

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                logger.warn("会员更新乐观锁冲突: id={}, 期望version={}", member.id, member.version);
                throw new SQLException("会员数据已被其他操作修改，请重试 (id=" + member.id + ")");
            }
            member.version++;
            return true;
        }
    }

    /**
     * 使用指定的数据库连接更新会员
     * @param conn 数据库连接
     * @param member 会员对象
     * @return 如果更新成功返回true，否则返回false
     * @throws SQLException 数据库操作异常
     */
    public static boolean updateWithConnection(Connection conn, Member member) throws SQLException {
        String sql = "UPDATE members SET member_code = ?, phone = ?, name = ?, points = ?, level = ?, discount = ?, balance = ?, birthday = ?, version = version + 1 " +
                     "WHERE id = ? AND version = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, member.memberCode);
            pstmt.setString(2, member.phone);
            pstmt.setString(3, member.name);
            pstmt.setBigDecimal(4, member.points);
            pstmt.setString(5, member.level);
            pstmt.setBigDecimal(6, member.discount);
            pstmt.setBigDecimal(7, member.balance);
            pstmt.setString(8, member.birthday);
            pstmt.setInt(9, member.id);
            pstmt.setInt(10, member.version);

            int affected = pstmt.executeUpdate();
            if (affected == 0) {
                // 乐观锁冲突：版本号不匹配，说明有其他事务已修改该记录
                logger.warn("会员更新乐观锁冲突: id={}, 期望version={}", member.id, member.version);
                throw new SQLException("会员数据已被其他操作修改，请重试 (id=" + member.id + ")");
            }
            // 更新成功后递增内存中的版本号
            member.version++;
            return true;
        }
    }

    /**
     * 删除会员
     */
    public static boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM members WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据手机号删除会员（兼容旧代码）
     */
    public static boolean deleteByPhone(String phone) throws SQLException {
        String sql = "DELETE FROM members WHERE phone = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, phone);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新会员积分
     */
    public static boolean updatePoints(int id, double delta) throws SQLException {
        String sql = "UPDATE members SET points = points + ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, java.math.BigDecimal.valueOf(delta));
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新会员积分（带 Connection，用于事务）
     */
    public static boolean updatePointsWithConnection(Connection conn, int id, double delta) throws SQLException {
        String sql = "UPDATE members SET points = points + ? WHERE id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, java.math.BigDecimal.valueOf(delta));
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据手机号更新会员积分（兼容旧代码）
     */
    public static boolean updatePointsByPhone(String phone, double delta) throws SQLException {
        String sql = "UPDATE members SET points = points + ? WHERE phone = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, java.math.BigDecimal.valueOf(delta));
            pstmt.setString(2, phone);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新会员余额
     */
    public static boolean updateBalance(int id, double delta) throws SQLException {
        String sql = "UPDATE members SET balance = balance + ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, java.math.BigDecimal.valueOf(delta));
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 根据手机号更新会员余额（兼容旧代码）
     */
    public static boolean updateBalanceByPhone(String phone, double delta) throws SQLException {
        String sql = "UPDATE members SET balance = balance + ? WHERE phone = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setBigDecimal(1, java.math.BigDecimal.valueOf(delta));
            pstmt.setString(2, phone);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 搜索会员（按姓名或手机号）
     */
    public static List<Member> search(String keyword) throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday, version FROM members " +
                     "WHERE name LIKE ? OR phone LIKE ? ORDER BY name";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(mapRowToMember(rs));
            }
        }
        return members;
    }

    /**
     * 分页搜索会员（按姓名或手机号）。
     */
    public static PageResult<Member> search(String keyword, int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }

        List<Member> members = new ArrayList<>();
        String pattern = "%" + keyword + "%";
        long total = countByKeyword(pattern);
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday, version FROM members " +
                     "WHERE name LIKE ? OR phone LIKE ? ORDER BY name LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setInt(3, pageSize);
            pstmt.setInt(4, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    members.add(mapRowToMember(rs));
                }
            }
        }
        return new PageResult<>(members, pageNum, pageSize, total);
    }

    private static long countByKeyword(String pattern) throws SQLException {
        String sql = "SELECT COUNT(*) FROM members WHERE name LIKE ? OR phone LIKE ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    /**
     * 根据等级查询会员
     */
    public static List<Member> findByLevel(String level) throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT id, member_code, phone, name, points, level, discount, balance, birthday, version FROM members " +
                     "WHERE level = ? ORDER BY points DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, level);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(mapRowToMember(rs));
            }
        }
        return members;
    }

    /**
     * 批量插入会员
     */
    public static void batchInsert(List<Member> members) throws SQLException {
        String sql = "INSERT INTO members (phone, name, points, level, discount, balance, birthday) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Member member : members) {
                pstmt.setString(1, member.phone);
                pstmt.setString(2, member.name);
                pstmt.setBigDecimal(3, member.points);
                pstmt.setString(4, member.level);
                pstmt.setBigDecimal(5, member.discount);
                pstmt.setBigDecimal(6, member.balance);
                pstmt.setString(7, member.birthday);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    /**
     * 将 ResultSet 映射为 Member 对象
     */
    private static Member mapRowToMember(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.id = rs.getInt("id");
        member.memberCode = rs.getString("member_code");
        member.phone = rs.getString("phone");
        member.name = rs.getString("name");
        member.points = rs.getBigDecimal("points");
        member.level = rs.getString("level");
        member.discount = rs.getBigDecimal("discount");
        member.discountRate = member.discount;
        member.balance = rs.getBigDecimal("balance");
        member.birthday = rs.getString("birthday");
        try {
            member.version = rs.getInt("version");
        } catch (SQLException e) {
            // H2 测试数据库可能没有 version 列
            member.version = 0;
        }
        return member;
    }
}
