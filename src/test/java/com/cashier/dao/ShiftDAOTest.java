package com.cashier.dao;

import com.cashier.model.Shift;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("班次数据访问对象测试")
class ShiftDAOTest extends DatabaseTestBase {

    private final ShiftDAORefactored shiftDAO = DAOFactory.getInstance().getShiftDAO();

    @Test
    @DisplayName("查询最近班次时按开始时间倒序并限制数量")
    void testFindRecentUsesLimitAndNewestFirst() throws SQLException {
        Shift oldShift = createShift("SHIFT-OLD", Instant.ofEpochMilli(1_000L));
        Shift middleShift = createShift("SHIFT-MIDDLE", Instant.ofEpochMilli(2_000L));
        Shift newestShift = createShift("SHIFT-NEW", Instant.ofEpochMilli(3_000L));

        shiftDAO.insert(oldShift);
        shiftDAO.insert(middleShift);
        shiftDAO.insert(newestShift);

        var shifts = shiftDAO.findRecent(2);

        assertEquals(2, shifts.size());
        assertEquals("SHIFT-NEW", shifts.get(0).shiftId);
        assertEquals("SHIFT-MIDDLE", shifts.get(1).shiftId);
    }

    private Shift createShift(String shiftId, Instant startTime) {
        Shift shift = new Shift(shiftId, "admin", "管理员", startTime, BigDecimal.ZERO, 0);
        shift.endShift(BigDecimal.valueOf(100), 3,
            BigDecimal.valueOf(40), BigDecimal.valueOf(30), BigDecimal.valueOf(20), BigDecimal.TEN);
        return shift;
    }
}
