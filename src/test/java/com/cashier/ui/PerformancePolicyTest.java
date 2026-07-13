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

    @Test
    @DisplayName("商品会员库存列表 API 必须分页查询")
    void coreListApisUsePagedQueries() throws Exception {
        String productApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/ProductApiController.java"
        ));
        String memberApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/MemberApiController.java"
        ));
        String inventoryApi = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/InventoryApiController.java"
        ));
        String pagination = Files.readString(Path.of(
            "src/main/java/com/cashier/api/controller/ApiPagination.java"
        ));
        String productDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/ProductDAORefactored.java"
        ));

        assertTrue(productApi.contains("ApiPagination.from(ctx)"));
        assertTrue(productApi.contains("productDAO.findAll(page.page(), page.pageSize())"));
        assertTrue(productApi.contains("productDAO.search(keyword, page.page(), page.pageSize())"));
        assertTrue(productApi.contains("productDAO.findByCategory(category, page.page(), page.pageSize())"));
        assertFalse(productApi.contains("productDAO.findAll()"));
        assertFalse(productApi.contains("productDAO.search(keyword)"));

        assertTrue(memberApi.contains("MemberDAO.findAll(page.page(), page.pageSize())"));
        assertFalse(memberApi.contains("MemberDAO.findAll()"));

        assertTrue(inventoryApi.contains("productDAO.findAll(page.page(), page.pageSize())"));
        assertTrue(inventoryApi.contains("productDAO.findLowStock(page.page(), page.pageSize())"));
        assertTrue(inventoryApi.contains("productDAO.getInventorySummary()"));
        assertFalse(inventoryApi.contains("productDAO.findAll()"));

        assertTrue(pagination.contains("MAX_PAGE_SIZE = 500"));
        assertTrue(productDao.contains("findByCategory(String category, int pageNum, int pageSize)"));
        assertTrue(productDao.contains("findLowStock(int pageNum, int pageSize)"));
        assertTrue(productDao.contains("getInventorySummary()"));
    }

    @Test
    @DisplayName("桌面交易相关页面必须避免默认全量交易加载")
    void desktopTransactionViewsUseDateRangeOrAggregates() throws Exception {
        String transactionController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/TransactionController.java"
        ));
        String shiftController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ShiftController.java"
        ));
        String profitReportController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ProfitReportController.java"
        ));

        assertTrue(transactionController.contains("findTransactionsByCurrentDateRange()"));
        assertTrue(transactionController.contains("TransactionDAO.findByDateRange("));
        assertFalse(transactionController.contains("allTransactions = TransactionDAO.findAll();"));

        assertTrue(shiftController.contains("TransactionDAO.getTotalRevenue("));
        assertTrue(shiftController.contains("TransactionDAO.getTransactionCount("));
        assertTrue(shiftController.contains("TransactionDAO.findByDateRange("));
        assertFalse(shiftController.contains("TransactionDAO.findAll()"));

        assertTrue(profitReportController.contains("findTransactionsByDateRange(startDate"));
        assertTrue(profitReportController.contains("TransactionDAO.findByDateRange("));
        assertFalse(profitReportController.contains("allTransactions = TransactionDAO.findAll();"));
    }

    @Test
    @DisplayName("会员和库存桌面列表必须使用分页加载")
    void desktopMemberAndInventoryListsUsePagedQueries() throws Exception {
        String memberController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/MemberController.java"
        ));
        String inventoryController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryController.java"
        ));
        String memberDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/MemberDAO.java"
        ));

        assertTrue(memberController.contains("MemberDAO.findAll(FIRST_PAGE, DESKTOP_PAGE_SIZE)"));
        assertTrue(memberController.contains("MemberDAO.search(searchText, FIRST_PAGE, DESKTOP_PAGE_SIZE)"));
        assertFalse(memberController.contains("MemberDAO.findAll()"));

        assertTrue(inventoryController.contains("productDAO.findAll(FIRST_PAGE, DESKTOP_PAGE_SIZE)"));
        assertTrue(inventoryController.contains("productDAO.search(searchText, FIRST_PAGE, DESKTOP_PAGE_SIZE)"));
        assertFalse(inventoryController.contains("productDAO.findAll()"));
        assertFalse(inventoryController.contains("productDAO.search(searchText)"));

        assertTrue(memberDao.contains("PageResult<Member> search(String keyword, int pageNum, int pageSize)"));
    }

    @Test
    @DisplayName("收银台商品加载和扫码必须避免无界全量商品查询")
    void cartProductLookupUsesPagedAndTargetedQueries() throws Exception {
        String cartController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/CartController.java"
        ));

        assertTrue(cartController.contains("CART_PRODUCT_PAGE_SIZE = 500"));
        assertTrue(cartController.contains("productDAO.findAll(FIRST_PAGE, CART_PRODUCT_PAGE_SIZE)"));
        assertTrue(cartController.contains("productDAO.search(searchText.trim(), FIRST_PAGE, CART_PRODUCT_PAGE_SIZE)"));
        assertTrue(cartController.contains("findExactScanMatches(normalizedScanText)"));
        assertTrue(cartController.contains("productDAO.findByBarcode(scanText)"));
        assertTrue(cartController.contains("productDAO.findByProductCode(scanText)"));
        assertTrue(cartController.contains("productDAO.findByName(scanText)"));
        assertFalse(cartController.contains("productDAO.findAll()"));
    }

    @Test
    @DisplayName("盘点补货和商品编辑不得无界全量加载商品")
    void inventoryDialogsAvoidUnboundedProductLoads() throws Exception {
        String inventoryCheckController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/InventoryCheckController.java"
        ));
        String restockController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/RestockController.java"
        ));
        String productEditController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ProductEditController.java"
        ));
        String productDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/ProductDAORefactored.java"
        ));

        assertTrue(inventoryCheckController.contains("CHECK_PRODUCT_PAGE_SIZE = 500"));
        assertTrue(inventoryCheckController.contains("productDAO.findAll(FIRST_PAGE, CHECK_PRODUCT_PAGE_SIZE)"));
        assertTrue(inventoryCheckController.contains("productDAO.search(normalizedSearch, FIRST_PAGE, CHECK_PRODUCT_PAGE_SIZE)"));
        assertTrue(inventoryCheckController.contains("productDAO.findByCategory(selectedCategory, FIRST_PAGE, CHECK_PRODUCT_PAGE_SIZE)"));
        assertFalse(inventoryCheckController.contains("productDAO.findAll()"));

        assertFalse(restockController.contains("productDAO.findAll()"));
        assertFalse(productEditController.contains("productDAO.findAll()"));
        assertTrue(productEditController.contains("productDAO.countByProductCodePrefix(prefix)"));
        assertTrue(productDao.contains("countByProductCodePrefix(String prefix)"));
    }

    @Test
    @DisplayName("审计日志和退货订单列表必须避免默认全量加载")
    void auditAndReturnOrderListsUseBoundedQueries() throws Exception {
        String auditLogController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/AuditLogController.java"
        ));
        String operationLogDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/OperationLogDAO.java"
        ));
        String returnOrderController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/ReturnOrderController.java"
        ));
        String returnOrderDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/ReturnOrderDAO.java"
        ));

        assertTrue(auditLogController.contains("OperationLogDAO.findRecent(AUDIT_LOG_LIMIT)"));
        assertTrue(operationLogDao.contains("findRecent(int limit)"));
        assertTrue(operationLogDao.contains("ORDER BY timestamp DESC LIMIT ?"));
        assertFalse(auditLogController.contains("OperationLogDAO.findAll()"));

        assertTrue(returnOrderController.contains("ReturnOrderDAO.findRecent(RETURN_ORDER_LIMIT)"));
        assertTrue(returnOrderController.contains("ReturnOrderDAO.findByDateRange("));
        assertTrue(returnOrderDao.contains("findRecent(int limit)"));
        assertTrue(returnOrderDao.contains("ORDER BY create_time DESC LIMIT ?"));
        assertFalse(returnOrderController.contains("ReturnOrderDAO.findAll()"));
    }

    @Test
    @DisplayName("采购订单和入库历史默认列表必须有界加载")
    void purchaseListsUseRecentQueriesByDefault() throws Exception {
        String purchaseOrderController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseOrderController.java"
        ));
        String purchaseOrderDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/PurchaseOrderDAO.java"
        ));
        String purchaseInboundController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseInboundController.java"
        ));
        String purchaseInboundDao = Files.readString(Path.of(
            "src/main/java/com/cashier/dao/PurchaseInboundDAO.java"
        ));

        assertTrue(purchaseOrderController.contains("PurchaseOrderDAO.findRecent(PURCHASE_ORDER_LIMIT)"));
        assertTrue(purchaseOrderDao.contains("findRecent(int limit)"));
        assertTrue(purchaseOrderDao.contains("ORDER BY po.create_time DESC LIMIT ?"));
        assertFalse(purchaseOrderController.contains("PurchaseOrderDAO.findAll()"));

        assertTrue(purchaseInboundController.contains("PurchaseInboundDAO.findRecent(INBOUND_HISTORY_LIMIT)"));
        assertTrue(purchaseInboundDao.contains("findRecent(int limit)"));
        assertTrue(purchaseInboundDao.contains("ORDER BY pi.create_time DESC LIMIT ?"));
        assertFalse(purchaseInboundController.contains("PurchaseInboundDAO.findAll()"));
    }

    @Test
    @DisplayName("采购报表和审批页面不得默认全量加载采购订单")
    void purchaseReportsAndApprovalAvoidUnboundedOrderLoads() throws Exception {
        String purchaseReportController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseReportController.java"
        ));
        String purchaseApprovalController = Files.readString(Path.of(
            "src/main/java/com/cashier/controller/PurchaseApprovalController.java"
        ));

        assertTrue(purchaseReportController.contains("PurchaseOrderDAO.findByDateRange("));
        assertTrue(purchaseReportController.contains("loadOrdersByDateRange(startDate, endDate)"));
        assertFalse(purchaseReportController.contains("PurchaseOrderDAO.findAll()"));

        assertTrue(purchaseApprovalController.contains("PurchaseOrderDAO.findRecent(APPROVAL_ORDER_LIMIT)"));
        assertFalse(purchaseApprovalController.contains("PurchaseOrderDAO.findAll()"));
    }
}
