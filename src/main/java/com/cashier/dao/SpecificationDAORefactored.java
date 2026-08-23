package com.cashier.dao;

import com.cashier.model.Specification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * 商品规格类型数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class SpecificationDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "id, name, code, type, description, sort_order, enabled, create_time, update_time ";

    private static final RowMapper<Specification> SPECIFICATION_MAPPER = new RowMapper<Specification>() {
        @Override
        public Specification mapRow(ResultSet rs, int rowNum) throws SQLException {
            Specification specification = new Specification();
            specification.id = rs.getInt("id");
            specification.name = rs.getString("name");
            specification.code = rs.getString("code");
            specification.type = rs.getString("type");
            specification.description = rs.getString("description");
            specification.sortOrder = rs.getInt("sort_order");
            specification.enabled = rs.getBoolean("enabled");
            specification.createTime = rs.getTimestamp("create_time");
            specification.updateTime = rs.getTimestamp("update_time");
            return specification;
        }
    };

    /**
     * 插入规格类型
     *
     * @return 生成的自增 ID，失败返回 0
     */
    public int insert(Specification specification) throws SQLException {
        long id = executeInsertReturnId(
            "INSERT INTO specifications (name, code, type, description, sort_order, enabled, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            specification.name, specification.code, specification.type, specification.description,
            specification.sortOrder, specification.enabled,
            new Timestamp(specification.createTime.getTime()),
            new Timestamp(specification.updateTime.getTime()));
        return (int) id;
    }

    /**
     * 更新规格类型
     */
    public boolean update(Specification specification) throws SQLException {
        specification.updateTime = new java.util.Date();
        return executeUpdate(
            "UPDATE specifications SET name = ?, code = ?, type = ?, description = ?, " +
                "sort_order = ?, enabled = ?, update_time = ? WHERE id = ?",
            specification.name, specification.code, specification.type, specification.description,
            specification.sortOrder, specification.enabled,
            new Timestamp(specification.updateTime.getTime()), specification.id) > 0;
    }

    /**
     * 删除规格类型
     */
    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM specifications WHERE id = ?", id) > 0;
    }

    /**
     * 根据ID查找规格类型
     */
    public Specification findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM specifications WHERE id = ?", SPECIFICATION_MAPPER, id);
    }

    /**
     * 查找所有规格类型
     */
    public List<Specification> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM specifications ORDER BY sort_order ASC, id ASC", SPECIFICATION_MAPPER);
    }

    /**
     * 根据类型查找规格类型
     */
    public List<Specification> findByType(String type) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM specifications WHERE type = ? ORDER BY sort_order ASC, id ASC", SPECIFICATION_MAPPER, type);
    }

    /**
     * 查找启用的规格类型
     */
    public List<Specification> findEnabled() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM specifications WHERE enabled = true ORDER BY sort_order ASC, id ASC", SPECIFICATION_MAPPER);
    }
}
