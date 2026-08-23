package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.dao.PurchaseApprovalDAO;
import com.cashier.dao.PurchaseInboundDAO;
import com.cashier.dao.PurchaseInboundItemDAO;
import com.cashier.dao.PurchaseOrderDAO;
import com.cashier.dao.PurchaseOrderItemDAO;
import com.cashier.model.PurchaseApproval;
import com.cashier.model.PurchaseInbound;
import com.cashier.model.PurchaseInboundItem;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * 采购业务服务，统一采购审批和入库的事务边界。
 */
public final class PurchaseService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PurchaseService.class);
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    private static final Set<String> APPROVAL_ACTIONS = Set.of("approve", "reject");

    private PurchaseService() {
    }

    public static void approveOrder(int orderId, String approver, String action, String remark)
            throws SQLException {
        if (orderId <= 0 || approver == null || approver.isBlank() || !APPROVAL_ACTIONS.contains(action)) {
            throw new IllegalArgumentException("采购审批参数无效");
        }

        String status = "approve".equals(action) ? "approved" : "rejected";
        PurchaseApproval approval = new PurchaseApproval(orderId, approver, action, remark);

        boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
            if (!DAOFactory.getInstance().getPurchaseOrderDAO().approvePendingWithConnection(conn, orderId, approver, remark, status)) {
                throw new SQLException("采购订单不存在或已完成审批");
            }
            if (!PurchaseApprovalDAO.insertWithConnection(conn, approval)) {
                throw new SQLException("保存采购审批记录失败");
            }
            return true;
        });

        if (!success) {
            throw new SQLException("采购审批事务未提交");
        }
        logger.info("采购审批完成: orderId={}, action={}, approver={}", orderId, action, approver);
    }

    public static void receiveInbound(PurchaseInbound inbound, List<PurchaseInboundItem> items)
            throws SQLException {
        validateInbound(inbound, items);

        boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
            String orderStatus = DAOFactory.getInstance().getPurchaseOrderDAO().findStatusForUpdate(conn, inbound.orderId);
            if (!"approved".equals(orderStatus)) {
                throw new SQLException("只有已审批的采购订单可以入库");
            }
            if (!PurchaseInboundDAO.insertWithConnection(conn, inbound)) {
                throw new SQLException("保存采购入库单失败");
            }

            for (PurchaseInboundItem item : items) {
                item.inboundId = inbound.id;
                if (!PurchaseInboundItemDAO.insertWithConnection(conn, item)) {
                    throw new SQLException("保存采购入库明细失败: " + item.productName);
                }
                if (!PurchaseOrderItemDAO.increaseInboundQuantityWithConnection(conn, item.orderItemId, item.quantity)) {
                    throw new SQLException("入库数量超过采购数量或订单明细不存在: " + item.productName);
                }
                if (!productDAO.updateQuantityWithConnection(conn, item.productId, item.quantity)) {
                    throw new SQLException("更新商品库存失败: " + item.productName);
                }
            }

            if (PurchaseOrderItemDAO.areAllInboundWithConnection(conn, inbound.orderId)
                    && !DAOFactory.getInstance().getPurchaseOrderDAO().updateStatusWithConnection(conn, inbound.orderId, "completed")) {
                throw new SQLException("更新采购订单完成状态失败");
            }
            return true;
        });

        if (!success) {
            throw new SQLException("采购入库事务未提交");
        }
        logger.info("采购入库完成: inboundNo={}, orderId={}, itemCount={}",
            inbound.inboundNo, inbound.orderId, items.size());
    }

    private static void validateInbound(PurchaseInbound inbound, List<PurchaseInboundItem> items) {
        if (inbound == null || inbound.orderId <= 0 || inbound.inboundNo == null || inbound.inboundNo.isBlank()) {
            throw new IllegalArgumentException("采购入库单参数无效");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("采购入库明细不能为空");
        }
        if (items.stream().anyMatch(item -> item == null || item.orderItemId <= 0
                || item.productId <= 0 || item.quantity <= 0)) {
            throw new IllegalArgumentException("采购入库明细参数无效");
        }
    }
}
