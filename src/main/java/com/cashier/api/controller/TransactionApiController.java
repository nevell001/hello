package com.cashier.api.controller;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.MemberDAO;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.dao.ReturnOrderDAO;
import com.cashier.dao.ReturnOrderItemDAO;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.*;
import com.cashier.util.DatabaseManager;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 交易收银 REST API
 */
public class TransactionApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TransactionApiController.class);
    private static final DateTimeFormatter ID_FORMATTER = com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME_MILLIS;
    private static final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    
    /**
     * 获取交易列表
     * GET /api/transactions
     */
    public static void list(Context ctx) {
        try {
            String startDate = ctx.queryParam("startDate");
            String endDate = ctx.queryParam("endDate");
            String paymentMethod = ctx.queryParam("paymentMethod");
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(100);

            List<Transaction> transactions = hasDateFilter(startDate, endDate)
                ? TransactionDAO.findByDateRange(toStartDateTime(startDate), toEndDateTime(endDate))
                : TransactionDAO.findRecent(limit);
            
            // 按条件筛选
            if (paymentMethod != null && !paymentMethod.isEmpty()) {
                transactions = filterTransactions(transactions, paymentMethod);
            }
            
            // 按时间倒序
            transactions.sort((a, b) -> {
                if (a.timestamp == null || b.timestamp == null) return 0;
                return b.timestamp.compareTo(a.timestamp);
            });
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", transactions);
            result.put("total", transactions.size());
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取交易列表失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取交易列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个交易
     * GET /api/transactions/:id
     */
    public static void get(Context ctx) {
        try {
            String transactionId = ctx.pathParam("id");
            Transaction transaction = TransactionDAO.findById(transactionId);
            
            if (transaction == null) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(Map.of("success", false, "message", "交易不存在"));
                return;
            }
            
            ctx.json(Map.of("success", true, "data", transaction));
        } catch (Exception e) {
            logger.error("获取交易详情失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取交易详情失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建新交易（收银）
     * POST /api/transactions
     */
    public static void create(Context ctx) {
        try {
            TransactionRequest request = ctx.bodyAsClass(TransactionRequest.class);
            
            // 生成交易ID
            String transactionId = "T" + LocalDateTime.now().format(ID_FORMATTER);
            
            Transaction transaction = new Transaction();
            transaction.transactionId = transactionId;
            transaction.timestamp = LocalDateTime.now().format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
            transaction.items = request.items;
            transaction.totalAmount = request.totalAmount != null ? request.totalAmount : BigDecimal.ZERO;
            transaction.tax = request.tax != null ? request.tax : BigDecimal.ZERO;
            transaction.finalAmount = request.finalAmount != null ? request.finalAmount : BigDecimal.ZERO;
            transaction.paymentMethod = request.paymentMethod != null ? request.paymentMethod : "现金";
            transaction.memberId = request.memberId != null ? request.memberId : 0;
            transaction.memberPhone = request.memberPhone != null ? request.memberPhone : "";
            transaction.memberName = request.memberName != null ? request.memberName : "";
            transaction.operatorUsername = request.operatorUsername != null ? request.operatorUsername : "";
            transaction.operatorName = request.operatorName != null ? request.operatorName : "";
            
            TransactionDAO.insert(transaction);
            
            logger.info("创建交易: {} - 金额: {} - 支付方式: {}", 
                transactionId, transaction.finalAmount, transaction.paymentMethod);
            
            // 广播交易成功事件
            com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
                com.cashier.api.sync.SyncEventType.TRANSACTION_CREATED,
                Map.of(
                    "transactionId", transactionId,
                    "finalAmount", transaction.finalAmount.toString(),
                    "paymentMethod", transaction.paymentMethod,
                    "timestamp", transaction.timestamp,
                    "itemCount", transaction.items != null ? transaction.items.size() : 0
                )
            );
            
            ctx.status(HttpStatus.CREATED)
               .json(Map.of("success", true, "data", transaction, "transactionId", transactionId));
        } catch (Exception e) {
            logger.error("创建交易失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "创建交易失败: " + e.getMessage()));
        }
    }
    
    /**
     * 取消交易（退货）
     * POST /api/transactions/:id/refund
     */
    public static void refund(Context ctx) {
        try {
            String transactionId = ctx.pathParam("id");
            Transaction transaction = TransactionDAO.findById(transactionId);

            if (!validateRefundRequest(ctx, transaction)) {
                return;
            }

            // 执行退款事务
            boolean success = DatabaseManager.executeBooleanTransaction(
                conn -> processRefundTransaction(conn, transactionId, transaction));

            if (success) {
                respondRefundSuccess(ctx, transactionId, transaction);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .json(Map.of("success", false, "message", "退款处理失败"));
            }
        } catch (Exception e) {
            logger.error("交易退款失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "交易退款失败: " + e.getMessage()));
        }
    }

    private static boolean validateRefundRequest(Context ctx, Transaction transaction) {
        if (transaction == null) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(Map.of("success", false, "message", "交易不存在"));
            return false;
        }
        if ("REFUNDED".equals(transaction.status)) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(Map.of("success", false, "message", "该交易已退款"));
            return false;
        }
        return true;
    }

    private static boolean processRefundTransaction(Connection conn, String transactionId, Transaction transaction)
            throws SQLException {
        try {
            ReturnOrder returnOrder = createRefundReturnOrder(conn, transactionId, transaction);
            if (!ReturnOrderDAO.insertWithConnection(conn, returnOrder)) {
                return false;
            }
            if (!createReturnItemsAndRestoreInventory(conn, transaction, returnOrder.returnOrderId)) {
                return false;
            }
            adjustMemberPointsAfterRefund(conn, transaction);
            TransactionDAO.updateStatusWithConnection(conn, transactionId, "REFUNDED");
            return true;
        } catch (SQLException e) {
            logger.error("退款事务执行失败", e);
            throw e;
        }
    }

    private static ReturnOrder createRefundReturnOrder(Connection conn, String transactionId, Transaction transaction)
            throws SQLException {
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.originalTransactionId = transactionId;
        returnOrder.memberId = transaction.memberId > 0 ? transaction.memberId : null;
        returnOrder.memberName = transaction.memberName;
        returnOrder.totalAmount = transaction.finalAmount != null ? transaction.finalAmount : BigDecimal.ZERO;
        returnOrder.returnReason = "API退款";
        returnOrder.paymentMethod = mapPaymentMethodToRefund(transaction.paymentMethod);
        returnOrder.operatorName = transaction.operatorName;
        returnOrder.status = "COMPLETED"; // 直接完成，无需审批
        returnOrder.returnOrderId = ReturnOrderDAO.generateNextReturnOrderId(conn);
        return returnOrder;
    }

    private static boolean createReturnItemsAndRestoreInventory(
            Connection conn, Transaction transaction, String returnOrderId) throws SQLException {
        if (transaction.items == null || transaction.items.isEmpty()) {
            return true;
        }

        List<ReturnOrderItem> returnItems = new ArrayList<>();
        for (Product product : transaction.items) {
            returnItems.add(createReturnOrderItem(returnOrderId, product));
            productDAO.updateQuantityWithConnection(conn, product.id, product.quantity);
        }
        return ReturnOrderItemDAO.batchInsertWithConnection(conn, returnItems);
    }

    private static ReturnOrderItem createReturnOrderItem(String returnOrderId, Product product) {
        ReturnOrderItem item = new ReturnOrderItem();
        item.returnOrderId = returnOrderId;
        item.productId = product.id;
        item.productCode = product.productCode;
        item.productName = product.name;
        item.barcode = product.barcode;
        item.category = product.category;
        item.returnQuantity = product.quantity;
        item.unitPrice = product.price != null ? product.price : BigDecimal.ZERO;
        item.returnAmount = item.unitPrice.multiply(BigDecimal.valueOf(item.returnQuantity));
        item.condition = "GOOD";
        return item;
    }

    private static void adjustMemberPointsAfterRefund(Connection conn, Transaction transaction) throws SQLException {
        if (transaction.memberId <= 0 || transaction.finalAmount == null) {
            return;
        }

        // 积分按消费金额的 1% 计算，退货时扣减
        double pointsToDeduct = transaction.finalAmount.divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN).doubleValue();
        MemberDAO.updatePointsWithConnection(conn, transaction.memberId, -pointsToDeduct);

        // 重新计算会员等级
        Member member = MemberDAO.findByIdWithConnection(conn, transaction.memberId);
        if (member != null) {
            member.level = calculateMemberLevel(member.points);
            MemberDAO.updateWithConnection(conn, member);
        }
    }

    private static void respondRefundSuccess(Context ctx, String transactionId, Transaction transaction) {
        logger.info("交易退款成功: {} - 金额: {}", transactionId, transaction.finalAmount);

        // 广播交易退款事件
        com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
            com.cashier.api.sync.SyncEventType.TRANSACTION_REFUNDED,
            Map.of(
                "transactionId", transactionId,
                "refundAmount", transaction.finalAmount.toString(),
                "timestamp", LocalDateTime.now().format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME)
            )
        );

        ctx.json(Map.of("success", true, "message", "退款成功",
            "transactionId", transactionId,
            "refundAmount", transaction.finalAmount));
    }
    
    /**
     * 映射支付方式到退款方式
     */
    private static String mapPaymentMethodToRefund(String paymentMethod) {
        if (paymentMethod == null) return "CASH";
        if (paymentMethod.contains("微信")) return "WECHAT";
        if (paymentMethod.contains("支付宝")) return "ALIPAY";
        if (paymentMethod.contains("银行卡") || paymentMethod.contains("刷卡")) return "CARD";
        return "CASH";
    }
    
    /**
     * 根据积分计算会员等级
     */
    private static String calculateMemberLevel(BigDecimal points) {
        if (points == null) points = BigDecimal.ZERO;
        double p = points.doubleValue();
        if (p >= 10000) return "钻石会员";
        if (p >= 5000) return "金卡会员";
        if (p >= 2000) return "银卡会员";
        return "普通会员";
    }
    
    /**
     * 今日交易统计
     * GET /api/transactions/today
     */
    public static void todayStats(Context ctx) {
        try {
            String today = LocalDateTime.now().format(com.cashier.util.DateTimeFormats.DATE);
            List<Transaction> transactions = TransactionDAO.findByDateRange(
                today + " 00:00:00",
                today + " 23:59:59"
            );
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            for (Transaction t : transactions) {
                if (t.finalAmount != null) {
                    totalAmount = totalAmount.add(t.finalAmount);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("date", today);
            result.put("count", transactions.size());
            result.put("totalAmount", totalAmount);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取今日交易统计失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取今日交易统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 筛选交易
     */
    private static List<Transaction> filterTransactions(List<Transaction> transactions, String paymentMethod) {
        List<Transaction> result = new ArrayList<>();
        
        for (Transaction t : transactions) {
            if (t.paymentMethod == null || !t.paymentMethod.contains(paymentMethod)) {
                continue;
            }
            result.add(t);
        }
        
        return result;
    }

    private static boolean hasDateFilter(String startDate, String endDate) {
        return startDate != null && !startDate.isBlank()
            || endDate != null && !endDate.isBlank();
    }

    private static String toStartDateTime(String startDate) {
        String date = startDate == null || startDate.isBlank()
            ? LocalDate.now().minusDays(30).format(com.cashier.util.DateTimeFormats.DATE)
            : startDate;
        return date.length() == 10 ? date + " 00:00:00" : date;
    }

    private static String toEndDateTime(String endDate) {
        String date = endDate == null || endDate.isBlank()
            ? LocalDate.now().format(com.cashier.util.DateTimeFormats.DATE)
            : endDate;
        return date.length() == 10 ? date + " 23:59:59" : date;
    }
    
    /**
     * 交易请求DTO
     */
    public static class TransactionRequest {
        public List<com.cashier.model.Product> items;
        public BigDecimal totalAmount;
        public BigDecimal tax;
        public BigDecimal finalAmount;
        public String paymentMethod;
        public Integer memberId;
        public String memberPhone;
        public String memberName;
        public String operatorUsername;
        public String operatorName;
    }
}
