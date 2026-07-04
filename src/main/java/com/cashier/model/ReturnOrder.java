package com.cashier.model;

import com.cashier.i18n.I18nKeys;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 退货订单模型类
 */
public class ReturnOrder {
    public int id;
    public String returnOrderId;  // 退货单号（格式：R + 年月日 + 4位序号）
    public String originalTransactionId;  // 原交易ID
    public Integer memberId;  // 会员ID（可选）
    public String memberName;  // 会员名称
    public Instant returnDate;  // 退货日期
    public String returnReason;  // 退货原因
    public BigDecimal totalAmount;  // 退货总金额
    public String status;  // 状态：PENDING（待审批）、APPROVED（已批准）、REJECTED（已拒绝）、COMPLETED（已完成）
    public String paymentMethod;  // 退款方式：CASH（现金）、WECHAT（微信）、ALIPAY（支付宝）、CARD（银行卡）
    public String operatorName;  // 操作员
    public String approverName;  // 审批人
    public Instant approvalDate;  // 审批日期
    public String approvalComment;  // 审批意见
    public Instant completedDate;  // 完成日期
    public String notes;  // 备注
    public Instant createTime;  // 创建时间
    public Instant updateTime;  // 更新时间

    public ReturnOrder() {
        this.returnDate = Instant.now();
        this.createTime = Instant.now();
        this.updateTime = Instant.now();
        this.status = "PENDING";
        this.totalAmount = BigDecimal.ZERO;
    }

    private static BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // Getter 方法
    public int getId() {
        return id;
    }

    public String getReturnOrderId() {
        return returnOrderId;
    }

    public String getOriginalTransactionId() {
        return originalTransactionId;
    }

    public Integer getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public Instant getReturnDate() {
        return returnDate;
    }

    public String getReturnDateFormatted() {
        return returnDate != null ? returnDate.atZone(ZoneId.systemDefault())
            .toLocalDateTime().format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME) : "";
    }

    public String getReturnReason() {
        return returnReason;
    }

    public BigDecimal getTotalAmount() {
        return defaultDecimal(totalAmount);
    }

    public String getTotalAmountFormatted() {
        return com.cashier.util.CurrencyUtil.format(getTotalAmount().doubleValue());
    }

    public String getStatus() {
        return status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getApproverName() {
        return approverName;
    }

    public Instant getApprovalDate() {
        return approvalDate;
    }

    public String getApprovalComment() {
        return approvalComment;
    }

    public Instant getCompletedDate() {
        return completedDate;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public String getStatusText() {
        switch (status) {
            case "PENDING":
                return com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.STATUS_PENDING_APPROVAL);
            case "APPROVED":
                return com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.STATUS_APPROVED);
            case "REJECTED":
                return com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.STATUS_REJECTED);
            case "COMPLETED":
                return com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.STATUS_COMPLETED);
            default:
                return status;
        }
    }

    public String getPaymentMethodText() {
        switch (paymentMethod) {
            case "CASH":
                return com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_CASH);
            case "WECHAT":
                return com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_WECHAT);
            case "ALIPAY":
                return com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_ALIPAY);
            case "CARD":
                return com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_CARD);
            default:
                return paymentMethod;
        }
    }
}
