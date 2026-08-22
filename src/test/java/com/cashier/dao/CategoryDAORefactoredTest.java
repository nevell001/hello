package com.cashier.dao;

import com.cashier.model.Category;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryDAORefactoredTest extends DatabaseTestBase {

    private final CategoryDAORefactored categoryDAO = DAOFactory.getInstance().getCategoryDAO();

    @Test
    @DisplayName("插入分类并回填自增ID")
    void insertSetsGeneratedId() throws Exception {
        Category category = new Category("饮料", "各类饮品");

        assertTrue(categoryDAO.insert(category));
        assertTrue(category.id > 0);
        assertEquals("饮料", categoryDAO.findById(category.id).name);
    }

    @Test
    @DisplayName("按名称查询与存在性检查")
    void findByNameAndExists() throws Exception {
        categoryDAO.insert(new Category("零食", "膨化与坚果"));

        assertNotNull(categoryDAO.findByName("零食"));
        assertTrue(categoryDAO.exists("零食"));
        assertFalse(categoryDAO.exists("不存在分类"));
    }

    @Test
    @DisplayName("更新与删除")
    void updateAndDelete() throws Exception {
        Category category = new Category("生鲜", "蔬果肉蛋");
        categoryDAO.insert(category);

        category.name = "生鲜食品";
        assertTrue(categoryDAO.update(category));
        assertEquals("生鲜食品", categoryDAO.findById(category.id).name);

        assertTrue(categoryDAO.delete(category.id));
        assertNull(categoryDAO.findById(category.id));
    }

    @Test
    @DisplayName("批量插入与全量查询")
    void batchInsertAndFindAll() throws Exception {
        categoryDAO.batchInsert(List.of(new Category("日用品", ""), new Category("文具", "")));

        assertEquals(2, categoryDAO.findAll().size());
    }
}
