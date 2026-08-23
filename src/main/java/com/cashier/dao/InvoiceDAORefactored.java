package com.cashier.dao;

import com.cashier.model.Invoice;
import com.cashier.model.InvoiceItem;
import com.cashier.model.PageResult;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发票数据访问对象（重构版）
 * 实例方法 + BaseDAO 通用查询，通过 DAOFactory 获取。
 */
public class InvoiceDAORefactored extends BaseDAO {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InvoiceDAORefactored.class);

    public void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS invoices (
                invoice_id VARCHAR(50) PRIMARY KEY,
                invoice_code VARCHAR(20),
                invoice_number VARCHAR(20),
                transaction_id VARCHAR(50),
                buyer_name VARCHAR(100),
                buyer_tax_id VARCHAR(30),
                buyer_address VARCHAR(200),
                buyer_phone VARCHAR(50),
                buyer_bank VARCHAR(100),
                seller_name VARCHAR(100),
                seller_tax_id VARCHAR(30),
                seller_address VARCHAR(200),
                seller_phone VARCHAR(50),
                seller_bank VARCHAR(100),
                total_amount DECIMAL(10,2),
                tax_amount DECIMAL(10,2),
                final_amount DECIMAL(10,2),
                tax_rate DECIMAL(5,4),
                create_time DATETIME,
                print_time DATETIME,
                create_by VARCHAR(50),
                status VARCHAR(20),
                void_reason VARCHAR(200),
                void_time DATETIME,
                remark VARCHAR(500),
                payee VARCHAR(50),
                checker VARCHAR(50),
                print_count INT DEFAULT 0,
                pdf_path VARCHAR(200),
                image_path VARCHAR(200)
            )
            """;
        String itemsSql = """
            CREATE TABLE IF NOT EXISTS invoice_items (
                id INT AUTO_INCREMENT PRIMARY KEY,
                invoice_id VARCHAR(50),
                product_name VARCHAR(100),
                specification VARCHAR(100),
                unit VARCHAR(20),
                quantity INT,
                unit_price DECIMAL(10,2),
                amount DECIMAL(10,2),
                tax_rate DECIMAL(5,4),
                tax_amount DECIMAL(10,2),
                total_amount DECIMAL(10,2),
                FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
            )
            """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(itemsSql);
            logger.info("发票表创建成功");
        }
    }

    public boolean insert(Invoice invoice) throws SQLException {
        try (Connection conn = getConnection()) {
            return insertWithConnection(conn, invoice);
        }
    }

    public boolean insertWithConnection(Connection conn, Invoice invoice) throws SQLException {
        String sql = """
            INSERT INTO invoices (
                invoice_id, invoice_code, invoice_number, transaction_id,
                buyer_name, buyer_tax_id, buyer_address, buyer_phone, buyer_bank,
                seller_name, seller_tax_id, seller_address, seller_phone, seller_bank,
                total_amount, tax_amount, final_amount, tax_rate,
                create_time, create_by, status, remark, payee, checker
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, invoice.invoiceId);
            pstmt.setString(2, invoice.invoiceCode);
            pstmt.setString(3, invoice.invoiceNumber);
            pstmt.setString(4, invoice.transactionId);
            pstmt.setString(5, invoice.buyerName);
            pstmt.setString(6, invoice.buyerTaxId);
            pstmt.setString(7, invoice.buyerAddress);
            pstmt.setString(8, invoice.buyerPhone);
            pstmt.setString(9, invoice.buyerBank);
            pstmt.setString(10, invoice.sellerName);
            pstmt.setString(11, invoice.sellerTaxId);
            pstmt.setString(12, invoice.sellerAddress);
            pstmt.setString(13, invoice.sellerPhone);
            pstmt.setString(14, invoice.sellerBank);
            pstmt.setBigDecimal(15, invoice.totalAmount);
            pstmt.setBigDecimal(16, invoice.taxAmount);
            pstmt.setBigDecimal(17, invoice.finalAmount);
            pstmt.setBigDecimal(18, invoice.taxRate);
            pstmt.setTimestamp(19, new Timestamp(invoice.createTime.getTime()));
            pstmt.setString(20, invoice.createBy);
            pstmt.setString(21, invoice.status);
            pstmt.setString(22, invoice.remark);
            pstmt.setString(23, invoice.payee);
            pstmt.setString(24, invoice.checker);
            int rows = pstmt.executeUpdate();
            if (rows > 0 && invoice.items != null) {
                insertItems(conn, invoice.invoiceId, invoice.items);
            }
            return rows > 0;
        }
    }

    private void insertItems(Connection conn, String invoiceId, List<InvoiceItem> items) throws SQLException {
        String sql = """
            INSERT INTO invoice_items (
                invoice_id, product_name, specification, unit,
                quantity, unit_price, amount, tax_rate, tax_amount, total_amount
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (InvoiceItem item : items) {
                pstmt.setString(1, invoiceId);
                pstmt.setString(2, item.productName);
                pstmt.setString(3, item.specification);
                pstmt.setString(4, item.unit);
                pstmt.setInt(5, item.quantity);
                pstmt.setBigDecimal(6, item.unitPrice);
                pstmt.setBigDecimal(7, item.amount);
                pstmt.setBigDecimal(8, item.taxRate);
                pstmt.setBigDecimal(9, item.taxAmount);
                pstmt.setBigDecimal(10, item.totalAmount);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public Invoice findById(String invoiceId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM invoices WHERE invoice_id = ?")) {
            pstmt.setString(1, invoiceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.items = findItemsByInvoiceId(conn, invoiceId);
                    return invoice;
                }
            }
        }
        return null;
    }

    public Invoice findByTransactionId(String transactionId) throws SQLException {
        try (Connection conn = getConnection()) {
            return findByTransactionIdWithConnection(conn, transactionId);
        }
    }

    public Invoice findByTransactionIdWithConnection(Connection conn, String transactionId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT * FROM invoices WHERE transaction_id = ?")) {
            pstmt.setString(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Invoice invoice = mapResultSetToInvoice(rs);
                    invoice.items = findItemsByInvoiceId(conn, invoice.invoiceId);
                    return invoice;
                }
            }
        }
        return null;
    }

    public List<Invoice> findAll() throws SQLException {
        String sql = "SELECT * FROM invoices ORDER BY create_time DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return readInvoices(conn, rs);
        }
    }

    public PageResult<Invoice> findPage(LocalDate startDate, LocalDate endDate, String status,
                                        int pageNum, int pageSize) throws SQLException {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }
        QueryFilter filter = buildQueryFilter(startDate, endDate, status);
        long total = countByFilter(filter);
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT * FROM invoices " + filter.whereClause() +
            " ORDER BY create_time DESC LIMIT ? OFFSET ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindFilterParameters(pstmt, filter.parameters());
            int nextIndex = filter.parameters().size() + 1;
            pstmt.setInt(nextIndex, pageSize);
            pstmt.setInt(nextIndex + 1, offset);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Invoice> invoices = readInvoices(conn, rs);
                return new PageResult<>(invoices, pageNum, pageSize, total);
            }
        }
    }

    private long countByFilter(QueryFilter filter) throws SQLException {
        String sql = "SELECT COUNT(*) FROM invoices " + filter.whereClause();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            bindFilterParameters(pstmt, filter.parameters());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private QueryFilter buildQueryFilter(LocalDate startDate, LocalDate endDate, String status) {
        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        if (startDate != null && endDate != null) {
            conditions.add("create_time BETWEEN ? AND ?");
            parameters.add(Timestamp.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            parameters.add(Timestamp.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        if (status != null && !status.isBlank()) {
            conditions.add("status = ?");
            parameters.add(status);
        }
        return new QueryFilter(conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions), parameters);
    }

    private void bindFilterParameters(PreparedStatement pstmt, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object parameter = parameters.get(i);
            if (parameter instanceof Timestamp timestamp) {
                pstmt.setTimestamp(i + 1, timestamp);
            } else {
                pstmt.setObject(i + 1, parameter);
            }
        }
    }

    private record QueryFilter(String whereClause, List<Object> parameters) {
    }

    private List<Invoice> readInvoices(Connection conn, ResultSet rs) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        List<String> invoiceIds = new ArrayList<>();
        while (rs.next()) {
            Invoice invoice = mapResultSetToInvoice(rs);
            invoices.add(invoice);
            invoiceIds.add(invoice.invoiceId);
        }
        if (!invoices.isEmpty()) {
            Map<String, List<InvoiceItem>> itemsMap = findItemsByInvoiceIds(conn, invoiceIds);
            for (Invoice inv : invoices) {
                inv.items = itemsMap.getOrDefault(inv.invoiceId, new ArrayList<>());
            }
        }
        return invoices;
    }

    private List<InvoiceItem> findItemsByInvoiceId(Connection conn, String invoiceId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT * FROM invoice_items WHERE invoice_id = ?")) {
            pstmt.setString(1, invoiceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<InvoiceItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapResultSetToInvoiceItem(rs));
                }
                return items;
            }
        }
    }

    private Map<String, List<InvoiceItem>> findItemsByInvoiceIds(Connection conn, List<String> invoiceIds)
        throws SQLException {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return new HashMap<>();
        }
        String placeholders = String.join(",", Collections.nCopies(invoiceIds.size(), "?"));
        String sql = "SELECT * FROM invoice_items WHERE invoice_id IN (" + placeholders + ")";
        Map<String, List<InvoiceItem>> result = new HashMap<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < invoiceIds.size(); i++) {
                pstmt.setString(i + 1, invoiceIds.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String invId = rs.getString("invoice_id");
                    result.computeIfAbsent(invId, k -> new ArrayList<>()).add(mapResultSetToInvoiceItem(rs));
                }
            }
        }
        return result;
    }

    public boolean updateStatus(String invoiceId, String status) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE invoices SET status = ? WHERE invoice_id = ?")) {
            pstmt.setString(1, status);
            pstmt.setString(2, invoiceId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean voidInvoice(String invoiceId, String reason) throws SQLException {
        String sql = "UPDATE invoices SET status = 'VOIDED', void_reason = ?, void_time = ? WHERE invoice_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reason);
            pstmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(3, invoiceId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updatePrintInfo(String invoiceId, String pdfPath, String imagePath) throws SQLException {
        String sql = "UPDATE invoices SET status = 'PRINTED', print_time = ?, print_count = print_count + 1, " +
            "pdf_path = ?, image_path = ? WHERE invoice_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setString(2, pdfPath);
            pstmt.setString(3, imagePath);
            pstmt.setString(4, invoiceId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Invoice> findByDateRange(LocalDate startDate, LocalDate endDate) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE create_time BETWEEN ? AND ? ORDER BY create_time DESC";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            pstmt.setTimestamp(2, Timestamp.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            try (ResultSet rs = pstmt.executeQuery()) {
                return readInvoices(conn, rs);
            }
        }
    }

    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.invoiceId = rs.getString("invoice_id");
        invoice.invoiceCode = rs.getString("invoice_code");
        invoice.invoiceNumber = rs.getString("invoice_number");
        invoice.transactionId = rs.getString("transaction_id");
        invoice.buyerName = rs.getString("buyer_name");
        invoice.buyerTaxId = rs.getString("buyer_tax_id");
        invoice.buyerAddress = rs.getString("buyer_address");
        invoice.buyerPhone = rs.getString("buyer_phone");
        invoice.buyerBank = rs.getString("buyer_bank");
        invoice.sellerName = rs.getString("seller_name");
        invoice.sellerTaxId = rs.getString("seller_tax_id");
        invoice.sellerAddress = rs.getString("seller_address");
        invoice.sellerPhone = rs.getString("seller_phone");
        invoice.sellerBank = rs.getString("seller_bank");
        invoice.totalAmount = rs.getBigDecimal("total_amount");
        invoice.taxAmount = rs.getBigDecimal("tax_amount");
        invoice.finalAmount = rs.getBigDecimal("final_amount");
        invoice.taxRate = rs.getBigDecimal("tax_rate");
        invoice.createTime = rs.getTimestamp("create_time");
        invoice.printTime = rs.getTimestamp("print_time");
        invoice.createBy = rs.getString("create_by");
        invoice.status = rs.getString("status");
        invoice.voidReason = rs.getString("void_reason");
        invoice.voidTime = rs.getTimestamp("void_time");
        invoice.remark = rs.getString("remark");
        invoice.payee = rs.getString("payee");
        invoice.checker = rs.getString("checker");
        invoice.printCount = rs.getInt("print_count");
        invoice.pdfPath = rs.getString("pdf_path");
        invoice.imagePath = rs.getString("image_path");
        return invoice;
    }

    private InvoiceItem mapResultSetToInvoiceItem(ResultSet rs) throws SQLException {
        InvoiceItem item = new InvoiceItem();
        item.productName = rs.getString("product_name");
        item.specification = rs.getString("specification");
        item.unit = rs.getString("unit");
        item.quantity = rs.getInt("quantity");
        item.unitPrice = rs.getBigDecimal("unit_price");
        item.amount = rs.getBigDecimal("amount");
        item.taxRate = rs.getBigDecimal("tax_rate");
        item.taxAmount = rs.getBigDecimal("tax_amount");
        item.totalAmount = rs.getBigDecimal("total_amount");
        return item;
    }
}
