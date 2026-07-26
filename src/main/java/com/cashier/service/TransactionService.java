package com.cashier.service;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.i18n.I18nManager;
import com.cashier.util.DatabaseManager;
import com.cashier.util.LoggerFactoryUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.*;
import java.time.LocalDateTime;

/**
 * 交易服务类
 * 封装交易相关的业务逻辑
 */
public class TransactionService {
    private static final Logger logger = LoggerFactoryUtil.getLogger(TransactionService.class);
    private static final com.cashier.dao.ProductDAORefactored productDAO = com.cashier.dao.DAOFactory.getInstance().getProductDAO();

    /**
     * 交易结果
     */
    public static class TransactionResult {
        private boolean success;
        private String transactionId;
        private String message;
        private Transaction transaction;

        public TransactionResult(boolean success, String transactionId, String message, Transaction transaction) {
            this.success = success;
            this.transactionId = transactionId;
            this.message = message;
            this.transaction = transaction;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getMessage() {
            return message;
        }

        public Transaction getTransaction() {
            return transaction;
        }
    }

    /**
     * 执行交易
     * @param cartItems 购物车商品列表
     * @param member 会员（可为 null）
     * @param paymentMethod 支付方式
     * @param receivedAmount 实收金额
     * @param changeAmount 找零金额
     * @param inventory 库存数据（用于更新内存中的库存）
     * @return 交易结果
     */
    public static TransactionResult executeTransaction(
            List<CartItem> cartItems,
            Member member,
            String paymentMethod,
            BigDecimal receivedAmount,
            BigDecimal changeAmount,
            Map<String, Product> inventory) {

        String transactionId = generateOrderNumber();
        Transaction transaction = createTransaction(transactionId, cartItems, member, paymentMethod,
            receivedAmount.doubleValue(), changeAmount.doubleValue());
        return executeTransaction(cartItems, member, transaction, inventory, null);
    }

    /**
     * 执行交易（double 版本，向后兼容）
     * @deprecated 请使用 {@link #executeTransaction(List, Member, String, BigDecimal, BigDecimal, Map)} 避免精度丢失
     */
    @Deprecated
    public static TransactionResult executeTransaction(
            List<CartItem> cartItems,
            Member member,
            String paymentMethod,
            double receivedAmount,
            double changeAmount,
            Map<String, Product> inventory) {

        String transactionId = generateOrderNumber();
        Transaction transaction = createTransaction(transactionId, cartItems, member, paymentMethod, receivedAmount, changeAmount);
        return executeTransaction(cartItems, member, transaction, inventory, null);
    }

    /**
     * 执行交易（使用外部已构造的交易记录，适用于控制器已完成优惠计算的场景）
     * @param cartItems 购物车商品列表
     * @param member 会员（可为 null）
     * @param transaction 已构造的交易记录
     * @param inventory 库存数据（用于更新内存中的库存）
     * @param appliedPromotion 已应用的促销（可为 null）
     * @return 交易结果
     */
    public static TransactionResult executeTransaction(
            List<CartItem> cartItems,
            Member member,
            Transaction transaction,
            Map<String, Product> inventory,
            Promotion appliedPromotion) {

        String transactionId = transaction.transactionId != null ? transaction.transactionId : generateOrderNumber();
        transaction.transactionId = transactionId;
        BigDecimal payableAmount = transaction.finalAmount != null ? transaction.finalAmount : calculateFinalAmount(cartItems, member);
        List<Product> updatedProducts = new ArrayList<>();

        try {
            boolean success = DatabaseManager.executeBooleanTransaction(conn -> {
                for (CartItem item : cartItems) {
                    Product product = inventory.get(item.product.name);
                    if (product == null) {
                        throw new SQLException(I18nManager.getInstance().get("service.product_not_found", item.product.name));
                    }

                    Product latestProduct = productDAO.findByIdWithConnection(conn, item.product.id);
                    if (latestProduct == null) {
                        throw new SQLException(I18nManager.getInstance().get("service.product_not_found", item.product.name));
                    }

                    if (latestProduct.quantity < item.quantity) {
                        throw new SQLException(I18nManager.getInstance().get("service.product_out_of_stock",
                            item.product.name, latestProduct.quantity, item.quantity));
                    }

                    product.quantity = latestProduct.quantity - item.quantity;
                    product.version = latestProduct.version;

                    if (!productDAO.updateWithVersionWithConnection(conn, product)) {
                        throw new SQLException(I18nManager.getInstance().get("service.product_update_failed", item.product.name));
                    }

                    updatedProducts.add(product);
                }

                if (member != null) {
                    Member latestMember = member.id > 0
                        ? MemberDAO.findByIdWithConnection(conn, member.id)
                        : MemberDAO.findByPhoneWithConnection(conn, member.phone);
                    if (latestMember == null) {
                        throw new SQLException(I18nManager.getInstance().get("service.member_not_found", member.phone));
                    }

                    boolean memberBalancePayment = I18nManager.getInstance().get("payment.method.member_balance").equals(transaction.paymentMethod);
                    if (memberBalancePayment && latestMember.getBalance().compareTo(payableAmount) < 0) {
                        throw new SQLException(I18nManager.getInstance().get("service.member_balance_insufficient",
                            latestMember.getBalance(), payableAmount));
                    }

                    // L-1: 使用 FLOOR 而非 DOWN，保证负数（退货场景）也向下取整，业务行为一致
                    BigDecimal earnedPoints = payableAmount.multiply(BigDecimal.TEN).setScale(0, RoundingMode.FLOOR);
                    BigDecimal updatedPoints = latestMember.getPoints().add(earnedPoints);
                    String updatedLevel = MemberService.calculateLevel(updatedPoints);
                    BigDecimal updatedDiscount = MemberService.getDiscountByLevelDecimal(updatedLevel);

                    member.id = latestMember.id;
                    member.memberCode = latestMember.memberCode;
                    member.phone = latestMember.phone;
                    member.name = latestMember.name;
                    member.level = updatedLevel;
                    member.discount = updatedDiscount;
                    member.discountRate = updatedDiscount;
                    member.birthday = latestMember.getBirthday();
                    member.balance = memberBalancePayment
                        ? latestMember.getBalance().subtract(payableAmount)
                        : latestMember.getBalance();
                    member.points = updatedPoints;

                    if (!MemberDAO.updateWithConnection(conn, member)) {
                        throw new SQLException(I18nManager.getInstance().get("service.member_update_failed"));
                    }
                }

                if (!TransactionDAO.insertWithConnection(conn, transaction)) {
                    throw new SQLException(I18nManager.getInstance().get("service.transaction_save_failed", transactionId));
                }

                if (appliedPromotion != null && !PromotionDAO.incrementUsageWithConnection(conn, appliedPromotion.id)) {
                    throw new SQLException(I18nManager.getInstance().get("service.promotion_update_failed", appliedPromotion.id));
                }

                return true;
            });

            if (!success) {
                logger.warn("Transaction not committed: transactionId={}", transactionId);
                return new TransactionResult(false, null, I18nManager.getInstance().get("service.transaction_failed"), null);
            }

            for (Product product : updatedProducts) {
                inventory.put(product.name, product);
            }

            logger.info("交易成功完成，交易ID: {}", transactionId);
            AuditService.success(transaction.operatorUsername, "TRANSACTION", "SALE_COMPLETED",
                "交易单号=" + transactionId + ", 金额=" + transaction.finalAmount,
                cartItems.size());
            
            // 广播交易成功事件
            com.cashier.api.sync.SyncManager.getInstance().broadcastSyncEvent(
                com.cashier.api.sync.SyncEventType.TRANSACTION_CREATED,
                Map.of(
                    "transactionId", transactionId,
                    "finalAmount", transaction.finalAmount.toString(),
                    "paymentMethod", transaction.paymentMethod,
                    "timestamp", transaction.timestamp,
                    "itemCount", cartItems.size()
                )
            );
            
            return new TransactionResult(true, transactionId, I18nManager.getInstance().get("service.transaction_success"), transaction);
        } catch (SQLException | RuntimeException e) {
            logger.error("Transaction failed: {}", e.getMessage(), e);
            AuditService.failure(transaction.operatorUsername, "TRANSACTION", "SALE_FAILED",
                "transactionId=" + transactionId + ", reason=" + e.getMessage());
            return new TransactionResult(false, null, I18nManager.getInstance().get("service.transaction_failed_detail", e.getMessage()), null);
        }
    }

    /**
     * 创建交易记录
     * @param transactionId 交易ID
     * @param cartItems 购物车商品列表
     * @param member 会员
     * @param paymentMethod 支付方式
     * @param receivedAmount 实收金额
     * @param changeAmount 找零金额
     * @return 交易记录
     */
    private static Transaction createTransaction(
            String transactionId,
            List<CartItem> cartItems,
            Member member,
            String paymentMethod,
            double receivedAmount,
            double changeAmount) {

        Transaction transaction = new Transaction();
        transaction.transactionId = transactionId;
        transaction.timestamp = com.cashier.util.DateTimeFormats.formatStandard(LocalDateTime.now());
        transaction.items = new ArrayList<>();

        for (CartItem item : cartItems) {
            transaction.items.add(item.product);
        }

        transaction.totalAmount = calculateTotalAmount(cartItems);

        // 计算税费 — 直接从 String 构造 BigDecimal，避免 double 中转精度丢失
        Map<String, String> settings = DataService.loadSettings();
        BigDecimal taxRate = new BigDecimal(settings.getOrDefault("taxRate", "0.0"));
        transaction.tax = transaction.getTotalAmount()
                .multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        transaction.finalAmount = calculateFinalAmount(cartItems, member);
        transaction.paymentMethod = paymentMethod;

        if (member != null) {
            transaction.memberPhone = member.phone;
        }

        return transaction;
    }

    /**
     * 计算总金额
     * @param cartItems 购物车商品列表
     * @return 总金额
     */
    public static BigDecimal calculateTotalAmount(List<CartItem> cartItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            total = total.add(item.subtotal == null ? BigDecimal.ZERO : item.subtotal);
        }
        return total;
    }

    /**
     * 计算最终金额（应用会员折扣）
     * @param cartItems 购物车商品列表
     * @param member 会员
     * @return 最终金额
     */
    public static BigDecimal calculateFinalAmount(List<CartItem> cartItems, Member member) {
        BigDecimal total = calculateTotalAmount(cartItems);
        if (member != null) {
            // 折扣值范围：0-10，10表示不打折，0表示免费
            BigDecimal discountRate = member.getDiscount().divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
            total = total.multiply(discountRate);
        }
        // 规整到 2 位小数，避免下游比较/显示出错
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成订单号
     * @return 订单号
     */
    private static final java.util.concurrent.atomic.AtomicLong orderSequence =
        new java.util.concurrent.atomic.AtomicLong(0);

    public static String generateOrderNumber() {
        String ts = com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME_MILLIS.format(LocalDateTime.now());
        return "ORD" + ts + String.format("%04d", orderSequence.getAndIncrement() % 10000);
    }

    /**
     * 获取交易统计信息
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    public static TransactionStatistics getTransactionStatistics(String startDate, String endDate) {
        try {
            return TransactionDAO.getStatistics(startDate, endDate);
        } catch (SQLException e) {
            logger.error("获取交易统计失败", e);
            return new TransactionStatistics(0, BigDecimal.ZERO, 0, 0, 0);
        }
    }
}
