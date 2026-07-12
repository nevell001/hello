package com.cashier.api.controller;

import com.cashier.dao.TransactionDAO;
import com.cashier.model.Transaction;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 交易报表 REST API
 */
public class ReportApiController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ReportApiController.class);
    
    /**
     * 销售日报
     * GET /api/reports/daily?date=2024-01-01
     */
    public static void dailySales(Context ctx) {
        try {
            String dateStr = ctx.queryParam("date");
            if (dateStr == null) dateStr = LocalDate.now().toString();
            LocalDate date = LocalDate.parse(dateStr);
            List<Transaction> dayTransactions = TransactionDAO.findByDateRange(
                date.atStartOfDay().format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME),
                date.plusDays(1).atStartOfDay().minusSeconds(1).format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME)
            );
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal cashAmount = BigDecimal.ZERO;
            BigDecimal wechatAmount = BigDecimal.ZERO;
            BigDecimal alipayAmount = BigDecimal.ZERO;
            BigDecimal cardAmount = BigDecimal.ZERO;
            
            for (Transaction t : dayTransactions) {
                if (t.finalAmount != null) {
                    totalAmount = totalAmount.add(t.finalAmount);
                    
                    String payment = t.paymentMethod != null ? t.paymentMethod : "";
                    if (payment.contains("现金")) {
                        cashAmount = cashAmount.add(t.finalAmount);
                    } else if (payment.contains("微信")) {
                        wechatAmount = wechatAmount.add(t.finalAmount);
                    } else if (payment.contains("支付宝")) {
                        alipayAmount = alipayAmount.add(t.finalAmount);
                    } else if (payment.contains("银行卡")) {
                        cardAmount = cardAmount.add(t.finalAmount);
                    }
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("date", dateStr);
            result.put("totalTransactions", dayTransactions.size());
            result.put("totalAmount", totalAmount);
            result.put("cashAmount", cashAmount);
            result.put("wechatAmount", wechatAmount);
            result.put("alipayAmount", alipayAmount);
            result.put("cardAmount", cardAmount);
            result.put("transactions", dayTransactions);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取日报失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取日报失败: " + e.getMessage()));
        }
    }
    
    /**
     * 销售月报
     * GET /api/reports/monthly?month=2024-01
     */
    public static void monthlySales(Context ctx) {
        try {
            String monthStr = ctx.queryParam("month");
            if (monthStr == null) monthStr = LocalDate.now().format(com.cashier.util.DateTimeFormats.MONTH);
            LocalDate monthStart = LocalDate.parse(monthStr + "-01", com.cashier.util.DateTimeFormats.DATE);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            List<Transaction> monthTransactions = TransactionDAO.findByDateRange(
                monthStart.atStartOfDay().format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME),
                monthEnd.plusDays(1).atStartOfDay().minusSeconds(1).format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME)
            );
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            Map<String, BigDecimal> dailyAmounts = new TreeMap<>();
            Map<String, Integer> dailyCounts = new TreeMap<>();
            
            for (Transaction t : monthTransactions) {
                if (t.finalAmount != null) {
                    totalAmount = totalAmount.add(t.finalAmount);
                    
                    String day = t.timestamp.substring(0, 10);
                    dailyAmounts.merge(day, t.finalAmount, BigDecimal::add);
                    dailyCounts.merge(day, 1, Integer::sum);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("month", monthStr);
            result.put("totalTransactions", monthTransactions.size());
            result.put("totalAmount", totalAmount);
            result.put("dayCount", dailyAmounts.size());
            result.put("dailyAmounts", dailyAmounts);
            result.put("dailyCounts", dailyCounts);
            
            ctx.json(result);
        } catch (Exception e) {
            logger.error("获取月报失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取月报失败: " + e.getMessage()));
        }
    }
    
    /**
     * 商品销售排行
     * GET /api/reports/top-products?limit=10
     */
    public static void topProducts(Context ctx) {
        try {
            int limit = ctx.queryParamAsClass("limit", Integer.class).getOrDefault(10);
            List<Map<String, Object>> topList = TransactionDAO.getTopProducts(limit);
            
            ctx.json(Map.of("success", true, "data", topList));
        } catch (Exception e) {
            logger.error("获取商品排行失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取商品排行失败: " + e.getMessage()));
        }
    }
    
    /**
     * 支付方式统计
     * GET /api/reports/payment-methods
     */
    public static void paymentMethods(Context ctx) {
        try {
            List<Map<String, Object>> result = TransactionDAO.getPaymentMethodStats();
            
            ctx.json(Map.of("success", true, "data", result));
        } catch (Exception e) {
            logger.error("获取支付方式统计失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "获取支付方式统计失败: " + e.getMessage()));
        }
    }
}
