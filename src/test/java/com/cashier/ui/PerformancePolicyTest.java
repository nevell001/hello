package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformancePolicyTest {

    @Test
    @DisplayName("利润报表加载入库明细不得逐单查询")
    void profitReportLoadsInboundItemsInBatch() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ProfitReportController.java"
        ));
        String dao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/PurchaseInboundItemDAO.java"
        ));

        assertTrue(controller.contains("PurchaseInboundItemDAO.findByInboundIds("));
        assertTrue(dao.contains("findByInboundIds(Collection<Integer> inboundIds)"));
        assertFalse(controller.contains("PurchaseInboundItemDAO.findByInboundId(inbound.id)"));
    }

    @Test
    @DisplayName("同步最近交易必须在数据库侧限制数量")
    void syncRecentTransactionsUsesDatabaseLimit() throws Exception {
        String syncManager = Files.readString(Path.of(
            "src/main/java/com/cashier/api/sync/SyncManager.java"
        ));
        String transactionDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/TransactionDAO.java"
        ));

        assertTrue(syncManager.contains("TransactionDAO.findRecent(100)"));
        assertTrue(transactionDao.contains("findRecent(int limit)"));
        assertTrue(transactionDao.contains("ORDER BY timestamp DESC LIMIT ?"));
        assertFalse(syncManager.contains("TransactionDAO.findAll()"));
        assertFalse(syncManager.contains("transactions.subList("));
    }

    @Test
    @DisplayName("同步商品和会员必须分页返回")
    void syncProductsAndMembersUsePagedQueries() throws Exception {
        String syncManager = Files.readString(Path.of(
            "src/main/java/com/cashier/api/sync/SyncManager.java"
        ));
        String memberDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/MemberDAO.java"
        ));

        assertTrue(syncManager.contains("findAll(page, pageSize)"));
        assertTrue(syncManager.contains("putPageResult(responseData"));
        assertTrue(syncManager.contains("MAX_SYNC_PAGE_SIZE"));
        assertTrue(memberDao.contains("PageResult<Member> findAll(int pageNum, int pageSize)"));
        assertFalse(syncManager.contains("getProductDAO().findAll())"));
        assertFalse(syncManager.contains("MemberDAO.findAll())"));
    }

    @Test
    @DisplayName("统计页必须按日期范围查询交易")
    void statisticsReportQueriesTransactionsByDateRange() throws Exception {
        String statisticsController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/StatisticsController.java"
        ));

        assertTrue(statisticsController.contains("TransactionDAO.findByDateRange("));
        assertTrue(statisticsController.contains("DateTimeFormats.STANDARD_DATE_TIME"));
        assertFalse(statisticsController.contains("TransactionDAO.findAll()"));
        assertFalse(statisticsController.contains("filterTransactionsByDate("));
    }

    @Test
    @DisplayName("采购报表加载订单明细不得逐单查询")
    void purchaseReportLoadsOrderItemsInBatch() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseReportController.java"
        ));
        String dao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/PurchaseOrderItemDAO.java"
        ));

        assertTrue(controller.contains("PurchaseOrderItemDAO.findByOrderIds("));
        assertTrue(dao.contains("findByOrderIds(Collection<Integer> orderIds)"));
        assertFalse(controller.contains("PurchaseOrderItemDAO.findByOrderId(order.id)"));
    }

    @Test
    @DisplayName("库存报表必须按日期范围查询交易并预聚合销量")
    void inventoryReportQueriesTransactionsByDateRangeAndAggregatesSales() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryReportController.java"
        ));

        assertTrue(controller.contains("TransactionDAO.findByDateRange("));
        assertTrue(controller.contains("buildSalesStatsMap()"));
        assertTrue(controller.contains("SalesStats"));
        assertFalse(controller.contains("TransactionDAO.findAll()"));
        assertFalse(controller.contains("calculateSalesQuantity("));
        assertFalse(controller.contains("getLastSaleDate("));
    }

    @Test
    @DisplayName("交易 API 列表和今日统计不得全量拉取交易")
    void transactionApiUsesBoundedQueries() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/TransactionApiController.java"
        ));

        assertTrue(controller.contains("TransactionDAO.findRecent(limit)"));
        assertTrue(controller.contains("TransactionDAO.findByDateRange("));
        assertFalse(controller.contains("List<Transaction> transactions = TransactionDAO.findAll();"));
    }

    @Test
    @DisplayName("日报和月报 API 必须按日期范围查询交易")
    void reportApiDailyAndMonthlyUseDateRangeQueries() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/ReportApiController.java"
        ));

        assertTrue(controller.contains("dayTransactions = TransactionDAO.findByDateRange("));
        assertTrue(controller.contains("monthTransactions = TransactionDAO.findByDateRange("));
    }

    @Test
    @DisplayName("商品排行和支付方式 API 必须使用数据库聚合")
    void reportApiRankingAndPaymentStatsUseDatabaseAggregation() throws Exception {
        String controller = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/ReportApiController.java"
        ));
        String dao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/TransactionDAO.java"
        ));

        assertTrue(controller.contains("TransactionDAO.getTopProducts(limit)"));
        assertTrue(controller.contains("TransactionDAO.getPaymentMethodStats()"));
        assertTrue(dao.contains("getTopProducts(int limit)"));
        assertTrue(dao.contains("getPaymentMethodStats()"));
        assertFalse(controller.contains("TransactionDAO.findAll()"));
    }
}
