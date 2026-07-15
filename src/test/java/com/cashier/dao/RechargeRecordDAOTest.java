package com.cashier.dao;

import com.cashier.model.RechargeRecord;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("充值记录数据访问对象测试")
class RechargeRecordDAOTest extends DatabaseTestBase {

    @Test
    @DisplayName("查询会员最近充值记录时按手机号过滤并限制数量")
    void testFindRecentByMemberPhone() throws SQLException {
        RechargeRecord oldRecord = createRecord("R-OLD", "13800138000", 1_000L);
        RechargeRecord middleRecord = createRecord("R-MIDDLE", "13800138000", 2_000L);
        RechargeRecord newestRecord = createRecord("R-NEW", "13800138000", 3_000L);
        RechargeRecord otherMemberRecord = createRecord("R-OTHER", "13900139000", 4_000L);

        RechargeRecordDAO.insert(oldRecord);
        RechargeRecordDAO.insert(middleRecord);
        RechargeRecordDAO.insert(newestRecord);
        RechargeRecordDAO.insert(otherMemberRecord);

        var records = RechargeRecordDAO.findRecentByMemberPhone("13800138000", 2);

        assertEquals(2, records.size());
        assertEquals("R-NEW", records.get(0).recordId);
        assertEquals("R-MIDDLE", records.get(1).recordId);
    }

    @Test
    @DisplayName("查询最近充值记录时按时间倒序并限制数量")
    void testFindRecent() throws SQLException {
        RechargeRecord oldRecord = createRecord("R-OLD", "13800138000", 1_000L);
        RechargeRecord middleRecord = createRecord("R-MIDDLE", "13900139000", 2_000L);
        RechargeRecord newestRecord = createRecord("R-NEW", "13700137000", 3_000L);

        RechargeRecordDAO.insert(oldRecord);
        RechargeRecordDAO.insert(middleRecord);
        RechargeRecordDAO.insert(newestRecord);

        var records = RechargeRecordDAO.findRecent(2);

        assertEquals(2, records.size());
        assertEquals("R-NEW", records.get(0).recordId);
        assertEquals("R-MIDDLE", records.get(1).recordId);
    }

    @Test
    @DisplayName("会员手机号或数量无效时返回空列表")
    void testFindRecentByMemberPhoneRejectsInvalidArguments() throws SQLException {
        assertTrue(RechargeRecordDAO.findRecentByMemberPhone(null, 10).isEmpty());
        assertTrue(RechargeRecordDAO.findRecentByMemberPhone(" ", 10).isEmpty());
        assertTrue(RechargeRecordDAO.findRecentByMemberPhone("13800138000", 0).isEmpty());
        assertTrue(RechargeRecordDAO.findRecent(0).isEmpty());
    }

    private RechargeRecord createRecord(String recordId, String phone, long timestamp) {
        RechargeRecord record = new RechargeRecord(
            recordId,
            phone,
            "测试会员",
            BigDecimal.TEN,
            "现金",
            "系统"
        );
        record.timestamp = new Date(timestamp);
        return record;
    }
}
