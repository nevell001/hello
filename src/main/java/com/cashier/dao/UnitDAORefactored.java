package com.cashier.dao;

import com.cashier.model.Unit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 单位数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class UnitDAORefactored extends BaseDAO {

    private static final RowMapper<Unit> UNIT_MAPPER = new RowMapper<Unit>() {
        @Override
        public Unit mapRow(ResultSet rs, int rowNum) throws SQLException {
            Unit unit = new Unit();
            unit.id = rs.getInt("id");
            unit.name = rs.getString("name");
            unit.description = rs.getString("description");
            return unit;
        }
    };

    public List<Unit> findAll() throws SQLException {
        return queryList("SELECT id, name, description FROM units ORDER BY name", UNIT_MAPPER);
    }

    public Unit findById(int id) throws SQLException {
        return queryOneOrNull("SELECT id, name, description FROM units WHERE id = ?", UNIT_MAPPER, id);
    }

    public Unit findByName(String name) throws SQLException {
        return queryOneOrNull("SELECT id, name, description FROM units WHERE name = ?", UNIT_MAPPER, name);
    }

    public boolean insert(Unit unit) throws SQLException {
        long id = executeInsertReturnId(
            "INSERT INTO units (name, description) VALUES (?, ?)", unit.name, unit.description);
        unit.id = (int) id;
        return id > 0;
    }

    public boolean update(Unit unit) throws SQLException {
        return executeUpdate(
            "UPDATE units SET name = ?, description = ? WHERE id = ?",
            unit.name, unit.description, unit.id) > 0;
    }

    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM units WHERE id = ?", id) > 0;
    }

    public boolean deleteByName(String name) throws SQLException {
        return executeUpdate("DELETE FROM units WHERE name = ?", name) > 0;
    }

    public boolean exists(String name) throws SQLException {
        return queryInt("SELECT COUNT(*) FROM units WHERE name = ?", name) > 0;
    }

    public void batchInsert(List<Unit> units) throws SQLException {
        List<Object[]> params = new ArrayList<>(units.size());
        for (Unit unit : units) {
            params.add(new Object[]{unit.name, unit.description});
        }
        batchUpdate("INSERT INTO units (name, description) VALUES (?, ?)", params);
    }
}
