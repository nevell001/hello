package com.cashier.dao;

import com.cashier.model.PurchaseApproval;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("采购审批数据访问对象测试")
class PurchaseApprovalDAORefactoredTest extends DatabaseTestBase {

    private final PurchaseApprovalDAORefactored approvalDAO =
        DAOFactory.getInstance().getPurchaseApprovalDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    @Test
    @DisplayName("插入审批记录后可按订单查询")
    void insertAndFindByOrderId() throws SQLException {
        PurchaseApproval approval = new PurchaseApproval(101, "admin", "approve", "同意");
        approval.approvalTime = new Timestamp(System.currentTimeMillis());

        assertTrue(approvalDAO.insert(approval));
        assertTrue(approval.id > 0);

        List<PurchaseApproval> approvals = approvalDAO.findByOrderId(101);

        assertEquals(1, approvals.size());
        assertEquals("admin", approvals.get(0).approver);
        assertEquals("approve", approvals.get(0).action);
        assertNotNull(approvals.get(0).approvalTime);

        assertTrue(approvalDAO.deleteByOrderId(101));
        assertTrue(approvalDAO.findByOrderId(101).isEmpty());
    }
}
