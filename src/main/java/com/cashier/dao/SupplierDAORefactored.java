package com.cashier.dao;

import com.cashier.model.Supplier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * 供应商数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class SupplierDAORefactored extends BaseDAO {

    private static final String SELECT_COLUMNS =
        "id, supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time ";

    private static final RowMapper<Supplier> SUPPLIER_MAPPER = new RowMapper<Supplier>() {
        @Override
        public Supplier mapRow(ResultSet rs, int rowNum) throws SQLException {
            Supplier supplier = new Supplier();
            supplier.id = rs.getInt("id");
            supplier.supplierCode = rs.getString("supplier_code");
            supplier.name = rs.getString("name");
            supplier.contactPerson = rs.getString("contact_person");
            supplier.phone = rs.getString("phone");
            supplier.address = rs.getString("address");
            supplier.rank = rs.getString("rank");
            supplier.status = rs.getBoolean("status");
            supplier.remark = rs.getString("remark");
            supplier.createTime = rs.getTimestamp("create_time");
            supplier.updateTime = rs.getTimestamp("update_time");
            return supplier;
        }
    };

    public Supplier findById(int id) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS + " FROM suppliers WHERE id = ?", SUPPLIER_MAPPER, id);
    }

    public List<Supplier> findAll() throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS + " FROM suppliers ORDER BY id", SUPPLIER_MAPPER);
    }

    public List<Supplier> findRecent(int limit) throws SQLException {
        int safeLimit = limit > 0 ? limit : 100;
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM suppliers ORDER BY create_time DESC, id DESC LIMIT ?", SUPPLIER_MAPPER, safeLimit);
    }

    public List<Supplier> search(String keyword, int limit) throws SQLException {
        int safeLimit = limit > 0 ? limit : 100;
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String searchPattern = "%" + normalizedKeyword.toLowerCase(java.util.Locale.ROOT) + "%";
        return queryList("SELECT " + SELECT_COLUMNS +
                " FROM suppliers WHERE LOWER(COALESCE(name, '')) LIKE ? " +
                "OR LOWER(COALESCE(contact_person, '')) LIKE ? " +
                "OR LOWER(COALESCE(phone, '')) LIKE ? " +
                "OR LOWER(COALESCE(supplier_code, '')) LIKE ? " +
                "ORDER BY create_time DESC, id DESC LIMIT ?",
            SUPPLIER_MAPPER, searchPattern, searchPattern, searchPattern, searchPattern, safeLimit);
    }

    public int countBySupplierCodePrefix(String prefix) throws SQLException {
        return queryInt("SELECT COUNT(*) FROM suppliers WHERE supplier_code LIKE ?", prefix + "%");
    }

    public Supplier findByCode(String supplierCode) throws SQLException {
        return queryOneOrNull("SELECT " + SELECT_COLUMNS +
            " FROM suppliers WHERE supplier_code = ?", SUPPLIER_MAPPER, supplierCode);
    }

    public List<Supplier> findByName(String name) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM suppliers WHERE name LIKE ? ORDER BY name", SUPPLIER_MAPPER, "%" + name + "%");
    }

    public List<Supplier> findByRank(String rank) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM suppliers WHERE `rank` = ? ORDER BY name", SUPPLIER_MAPPER, rank);
    }

    public List<Supplier> findByStatus(boolean status) throws SQLException {
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM suppliers WHERE status = ? ORDER BY name", SUPPLIER_MAPPER, status);
    }

    public List<Supplier> findByStatus(boolean status, int limit) throws SQLException {
        int safeLimit = limit > 0 ? limit : 100;
        return queryList("SELECT " + SELECT_COLUMNS +
            " FROM suppliers WHERE status = ? ORDER BY create_time DESC, id DESC LIMIT ?",
            SUPPLIER_MAPPER, status, safeLimit);
    }

    public boolean insert(Supplier supplier) throws SQLException {
        if (supplier.name == null || supplier.name.trim().isEmpty()) {
            throw new SQLException("供应商名称不能为空");
        }
        if (supplier.supplierCode == null || supplier.supplierCode.trim().isEmpty()) {
            throw new SQLException("供应商编号不能为空");
        }
        long id = executeInsertReturnId(
            "INSERT INTO suppliers (supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            supplier.supplierCode, supplier.name, supplier.contactPerson, supplier.phone,
            supplier.address, supplier.rank, supplier.status, supplier.remark,
            supplier.createTime, supplier.updateTime);
        supplier.id = (int) id;
        return id > 0;
    }

    public boolean update(Supplier supplier) throws SQLException {
        return executeUpdate(
            "UPDATE suppliers SET supplier_code = ?, name = ?, contact_person = ?, phone = ?, " +
                "address = ?, `rank` = ?, status = ?, remark = ?, update_time = ? WHERE id = ?",
            supplier.supplierCode, supplier.name, supplier.contactPerson, supplier.phone,
            supplier.address, supplier.rank, supplier.status, supplier.remark,
            new Timestamp(System.currentTimeMillis()), supplier.id) > 0;
    }

    public boolean delete(int id) throws SQLException {
        if (hasPurchaseOrders(id)) {
            throw new SQLException("该供应商存在采购订单记录，无法删除。请先删除相关采购订单。");
        }
        return executeUpdate("DELETE FROM suppliers WHERE id = ?", id) > 0;
    }

    public boolean hasPurchaseOrders(int supplierId) throws SQLException {
        return queryInt("SELECT COUNT(*) FROM purchase_orders WHERE supplier_id = ?", supplierId) > 0;
    }

    public void batchInsert(List<Supplier> suppliers) throws SQLException {
        List<Object[]> params = new ArrayList<>(suppliers.size());
        for (Supplier supplier : suppliers) {
            params.add(new Object[]{
                supplier.supplierCode, supplier.name, supplier.contactPerson, supplier.phone,
                supplier.address, supplier.rank, supplier.status, supplier.remark,
                supplier.createTime, supplier.updateTime});
        }
        batchUpdate(
            "INSERT INTO suppliers (supplier_code, name, contact_person, phone, address, `rank`, status, remark, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", params);
    }
}
