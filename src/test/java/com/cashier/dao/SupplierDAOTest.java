package com.cashier.dao;

import com.cashier.model.Supplier;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("供应商数据访问对象测试")
class SupplierDAOTest extends DatabaseTestBase {

    private final SupplierDAORefactored supplierDAO = DAOFactory.getInstance().getSupplierDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    @Test
    @DisplayName("查询最近供应商时按创建时间倒序并限制数量")
    void testFindRecentUsesLimitAndNewestFirst() throws SQLException {
        Supplier oldSupplier = createSupplier("S202607130001", "旧供应商", 1_000L);
        Supplier middleSupplier = createSupplier("S202607130002", "中间供应商", 2_000L);
        Supplier newestSupplier = createSupplier("S202607130003", "最新供应商", 3_000L);

        supplierDAO.insert(oldSupplier);
        supplierDAO.insert(middleSupplier);
        supplierDAO.insert(newestSupplier);

        var suppliers = supplierDAO.findRecent(2);

        assertEquals(2, suppliers.size());
        assertEquals("S202607130003", suppliers.get(0).supplierCode);
        assertEquals("S202607130002", suppliers.get(1).supplierCode);
    }

    @Test
    @DisplayName("搜索供应商时在数据库侧匹配并限制数量")
    void testSearchUsesKeywordAndLimit() throws SQLException {
        supplierDAO.insert(createSupplier("S202607130001", "华东食品", 1_000L));
        supplierDAO.insert(createSupplier("S202607130002", "华东日用品", 2_000L));
        supplierDAO.insert(createSupplier("S202607130003", "华南文具", 3_000L));

        var suppliers = supplierDAO.search("华东", 1);

        assertEquals(1, suppliers.size());
        assertEquals("S202607130002", suppliers.get(0).supplierCode);
    }

    @Test
    @DisplayName("按供应商编号前缀统计数量")
    void testCountBySupplierCodePrefix() throws SQLException {
        supplierDAO.insert(createSupplier("S202607130001", "供应商一", 1_000L));
        supplierDAO.insert(createSupplier("S202607130002", "供应商二", 2_000L));
        supplierDAO.insert(createSupplier("S202607140001", "供应商三", 3_000L));

        assertEquals(2, supplierDAO.countBySupplierCodePrefix("S20260713"));
    }

    @Test
    @DisplayName("按状态查询供应商时限制数量并按最近创建排序")
    void testFindByStatusUsesLimitAndNewestFirst() throws SQLException {
        Supplier activeOld = createSupplier("S202607130001", "启用旧供应商", 1_000L);
        Supplier activeNewest = createSupplier("S202607130002", "启用新供应商", 3_000L);
        Supplier inactive = createSupplier("S202607130003", "停用供应商", 4_000L);
        inactive.status = false;

        supplierDAO.insert(activeOld);
        supplierDAO.insert(activeNewest);
        supplierDAO.insert(inactive);

        var suppliers = supplierDAO.findByStatus(true, 1);

        assertEquals(1, suppliers.size());
        assertEquals("S202607130002", suppliers.get(0).supplierCode);
    }

    private Supplier createSupplier(String supplierCode, String name, long createTimeMillis) {
        Supplier supplier = new Supplier();
        supplier.supplierCode = supplierCode;
        supplier.name = name;
        supplier.contactPerson = "联系人";
        supplier.phone = "13800000000";
        supplier.address = "测试地址";
        supplier.rank = "C";
        supplier.status = true;
        supplier.remark = "";
        supplier.createTime = new Timestamp(createTimeMillis);
        supplier.updateTime = new Timestamp(createTimeMillis);
        return supplier;
    }
}
