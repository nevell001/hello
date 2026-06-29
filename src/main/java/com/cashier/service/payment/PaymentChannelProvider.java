package com.cashier.service.payment;

import com.cashier.model.PaymentOrder;
import com.cashier.model.RefundRecord;

import java.util.Map;

/** 支付渠道适配器。真实微信/支付宝 SDK 实现只需接入此接口。 */
public interface PaymentChannelProvider {
    PaymentOrder.PaymentChannel channel();

    boolean isAvailable();

    String unavailableReason();

    void createOrder(PaymentOrder order);

    PaymentOrder.PaymentStatus queryStatus(PaymentOrder order);

    boolean verifyNotification(Map<String, String> notification);

    void refund(PaymentOrder order, RefundRecord refund);
}
