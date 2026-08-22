package com.cashier.dao;

import com.cashier.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class CategoryDAORefactored extends BaseDAO {

    private static final RowMapper<Category> CATEGORY_MAPPER = new RowMapper<Category>() {
        @Override
        public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
            Category category = new Category();
            category.id = rs.getInt("id");
            category.name = rs.getString("name");
            category.description = rs.getString("description");
            return category;
        }
    };

    public List<Category> findAll() throws SQLException {
        return queryList("SELECT id, name, description FROM categories ORDER BY name", CATEGORY_MAPPER);
    }

    public Category findById(int id) throws SQLException {
        return queryOneOrNull("SELECT id, name, description FROM categories WHERE id = ?", CATEGORY_MAPPER, id);
    }

    public Category findByName(String name) throws SQLException {
        return queryOneOrNull("SELECT id, name, description FROM categories WHERE name = ?", CATEGORY_MAPPER, name);
    }

    public boolean insert(Category category) throws SQLException {
        long id = executeInsertReturnId(
            "INSERT INTO categories (name, description) VALUES (?, ?)", category.name, category.description);
        category.id = (int) id;
        return id > 0;
    }

    public boolean update(Category category) throws SQLException {
        return executeUpdate(
            "UPDATE categories SET name = ?, description = ? WHERE id = ?",
            category.name, category.description, category.id) > 0;
    }

    public boolean delete(int id) throws SQLException {
        return executeUpdate("DELETE FROM categories WHERE id = ?", id) > 0;
    }

    public boolean deleteByName(String name) throws SQLException {
        return executeUpdate("DELETE FROM categories WHERE name = ?", name) > 0;
    }

    public boolean exists(String name) throws SQLException {
        return queryInt("SELECT COUNT(*) FROM categories WHERE name = ?", name) > 0;
    }

    public void batchInsert(List<Category> categories) throws SQLException {
        List<Object[]> params = new ArrayList<>(categories.size());
        for (Category category : categories) {
            params.add(new Object[]{category.name, category.description});
        }
        batchUpdate("INSERT INTO categories (name, description) VALUES (?, ?)", params);
    }

    public void batchInsertWithConnection(Connection conn, List<Category> categories) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Category category : categories) {
                stmt.setString(1, category.name);
                stmt.setString(2, category.description);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }
}
