package com.cashier.dao;

import com.cashier.model.Unit;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitDAORefactoredTest extends DatabaseTestBase {

    private final UnitDAORefactored unitDAO = DAOFactory.getInstance().getUnitDAO();

    @Test
    @DisplayName("插入单位并回填自增ID")
    void insertSetsGeneratedId() throws Exception {
        Unit unit = new Unit("箱", "纸箱");

        assertTrue(unitDAO.insert(unit));
        assertTrue(unit.id > 0);
        assertEquals("箱", unitDAO.findById(unit.id).name);
    }

    @Test
    @DisplayName("按名称查询与存在性检查")
    void findByNameAndExists() throws Exception {
        Unit unit = new Unit("瓶", "瓶装");
        unitDAO.insert(unit);

        assertNotNull(unitDAO.findByName("瓶"));
        assertTrue(unitDAO.exists("瓶"));
        assertFalse(unitDAO.exists("不存在单位"));
    }

    @Test
    @DisplayName("更新与删除")
    void updateAndDelete() throws Exception {
        Unit unit = new Unit("个", "单个");
        unitDAO.insert(unit);

        unit.name = "件";
        assertTrue(unitDAO.update(unit));
        assertEquals("件", unitDAO.findById(unit.id).name);

        assertTrue(unitDAO.delete(unit.id));
        assertNull(unitDAO.findById(unit.id));
    }

    @Test
    @DisplayName("批量插入")
    void batchInsert() throws Exception {
        unitDAO.batchInsert(List.of(new Unit("公斤", "kg"), new Unit("斤", "500g")));

        assertEquals(2, unitDAO.findAll().size());
    }
}
