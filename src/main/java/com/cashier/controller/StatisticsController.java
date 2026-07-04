package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Transaction;
import com.cashier.util.CurrencyUtil;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.SQLException;
import javafx.scene.control.*;
import javafx.scene.chart.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 数据统计控制器
 * 处理销售统计和报表
 */
public class StatisticsController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(StatisticsController.class);

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> timeRangeComboBox;

    @FXML
    private Label totalSalesLabel;

    @FXML
    private Label transactionCountLabel;

    @FXML
    private Label avgTransactionLabel;

    @FXML
    private Label memberSalesLabel;

    @FXML
    private Label cashSalesLabel;

    @FXML
    private Label wechatSalesLabel;

    @FXML
    private Label alipaySalesLabel;

    @FXML
    private Label cardSalesLabel;

    @FXML
    private Label topProductLabel;

    @FXML
    private Label topProductCountLabel;

    @FXML
    private Label topProductAmountLabel;

    @FXML
    private TableView<StatisticsRecord> categoryTable;

    @FXML
    private TableColumn<StatisticsRecord, String> categoryNameColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> categoryCountColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> categoryAmountColumn;

    @FXML
    private TableView<StatisticsRecord> hourlyTable;

    @FXML
    private TableColumn<StatisticsRecord, String> hourColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> hourCountColumn;

    @FXML
    private TableColumn<StatisticsRecord, String> hourAmountColumn;

    @FXML
    private Button queryButton;

    @FXML
    private Button exportButton;

    @FXML
    private PieChart paymentMethodPieChart;

    @FXML
    private LineChart<String, Number> salesTrendLineChart;

    @FXML
    private BarChart<String, Number> categorySalesBarChart;

    private List<Transaction> allTransactions;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化时间范围下拉框
        timeRangeComboBox.setItems(javafx.collections.FXCollections.observableArrayList(
            "今天",
            "昨天",
            "本周",
            "上周",
            "本月",
            "上月",
            "自定义"
        ));
        com.cashier.util.I18nUiUtils.configureComboBox(
            timeRangeComboBox, com.cashier.util.I18nUiUtils::dateRange);
        timeRangeComboBox.getSelectionModel().select(0);

        // 设置默认日期范围（今天）
        LocalDate today = LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);

        // 设置表格列
        setupCategoryTableColumns();
        setupHourlyTableColumns();

        // 加载交易数据
        loadTransactions();

        // 执行查询
        handleQuery();

        // 监听时间范围变化
        timeRangeComboBox.setOnAction(event -> handleTimeRangeChange());
    }

    /**
     * 设置分类表格列
     */
    private void setupCategoryTableColumns() {
        categoryNameColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
        categoryCountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().count)));
        categoryAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().amount)));
    }

    /**
     * 设置小时表格列
     */
    private void setupHourlyTableColumns() {
        hourColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
        hourCountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().count)));
        hourAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(CurrencyUtil.format(cellData.getValue().amount)));
    }

    /**
     * 加载交易数据
     */
    private void loadTransactions() {
        logger.info("StatisticsController: 开始加载交易数据...");
        try {
            allTransactions = TransactionDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载交易数据失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            allTransactions = new java.util.ArrayList<>();
        }
        logger.info("StatisticsController: 加载了 {} 条交易记录", allTransactions.size());
    }

    /**
     * 处理时间范围变化
     */
    public void handleTimeRangeChange() {
        String selected = timeRangeComboBox.getSelectionModel().getSelectedItem();
        LocalDate today = LocalDate.now();

        switch (selected) {
            case "今天":
                startDatePicker.setValue(today);
                endDatePicker.setValue(today);
                break;
            case "昨天":
                LocalDate yesterday = today.minusDays(1);
                startDatePicker.setValue(yesterday);
                endDatePicker.setValue(yesterday);
                break;
            case "本周":
                LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
                startDatePicker.setValue(startOfWeek);
                endDatePicker.setValue(today);
                break;
            case "上周":
                LocalDate startOfLastWeek = today.minusDays(today.getDayOfWeek().getValue() - 1).minusWeeks(1);
                LocalDate endOfLastWeek = startOfLastWeek.plusDays(6);
                startDatePicker.setValue(startOfLastWeek);
                endDatePicker.setValue(endOfLastWeek);
                break;
            case "本月":
                LocalDate startOfMonth = today.withDayOfMonth(1);
                startDatePicker.setValue(startOfMonth);
                endDatePicker.setValue(today);
                break;
            case "上月":
                LocalDate startOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                LocalDate endOfLastMonth = today.withDayOfMonth(1).minusDays(1);
                startDatePicker.setValue(startOfLastMonth);
                endDatePicker.setValue(endOfLastMonth);
                break;
            case "自定义":
                // 不自动设置日期
                break;
            default:
                break;
        }
    }

    /**
     * 处理查询
     */
    @FXML
    public void handleQuery() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.SELECT_DATE_RANGE));
            return;
        }

        if (startDate.isAfter(endDate)) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.INVALID_DATE_RANGE));
            return;
        }

        // 筛选交易记录
        List<Transaction> filteredTransactions = filterTransactionsByDate(startDate, endDate);

        // 计算统计数据
        calculateStatistics(filteredTransactions);
    }

    /**
     * 处理刷新
     */
    @FXML
    public void handleRefresh() {
        // 重新加载数据
        loadTransactions();
        
        // 重新查询
        handleQuery();
        
        logger.info("数据统计已刷新");
    }

    /**
     * 按日期筛选交易记录
     */
    private List<Transaction> filterTransactionsByDate(LocalDate startDate, LocalDate endDate) {
        List<Transaction> filtered = new ArrayList<>();
        java.time.format.DateTimeFormatter sdf = com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME;

        for (Transaction t : allTransactions) {
            try {
                java.time.LocalDate localDate = java.time.LocalDateTime.parse(t.timestamp, sdf)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

                if (!localDate.isBefore(startDate) && !localDate.isAfter(endDate)) {
                    filtered.add(t);
                }
            } catch (Exception e) {
                // 日期解析失败，跳过该记录
            }
        }

        return filtered;
    }

    /**
     * 计算统计数据
     */
    private void calculateStatistics(List<Transaction> transactions) {
        // 总销售额
        double totalSales = 0.0;
        int transactionCount = transactions.size();

        // 按支付方式统计
        double cashSales = 0.0;
        double wechatSales = 0.0;
        double alipaySales = 0.0;
        double cardSales = 0.0;
        double memberSales = 0.0;

        // 商品统计
        Map<String, Integer> productCountMap = new HashMap<>();
        Map<String, Double> productAmountMap = new HashMap<>();

        // 分类统计
        Map<String, Integer> categoryCountMap = new HashMap<>();
        Map<String, Double> categoryAmountMap = new HashMap<>();

        // 小时统计
        Map<Integer, Integer> hourCountMap = new HashMap<>();
        Map<Integer, Double> hourAmountMap = new HashMap<>();

        for (Transaction t : transactions) {
            totalSales += t.getFinalAmount().doubleValue();

            // 支付方式统计
            switch (t.paymentMethod) {
                case "现金":
                    cashSales += t.getFinalAmount().doubleValue();
                    break;
                case "微信":
                    wechatSales += t.getFinalAmount().doubleValue();
                    break;
                case "支付宝":
                    alipaySales += t.getFinalAmount().doubleValue();
                    break;
                case "银行卡":
                    cardSales += t.getFinalAmount().doubleValue();
                    break;
                default:
                    logger.debug("跳过未知支付方式统计: {}", t.paymentMethod);
                    break;
            }

            // 会员统计
            if (t.memberPhone != null && !t.memberPhone.isEmpty()) {
                memberSales += t.getFinalAmount().doubleValue();
            }

            // 商品统计
            if (t.items != null) {
                for (var item : t.items) {
                    productCountMap.put(item.name, productCountMap.getOrDefault(item.name, 0) + item.quantity);
                    productAmountMap.put(item.name, productAmountMap.getOrDefault(item.name, 0.0) + item.getPrice().multiply(BigDecimal.valueOf(item.quantity)).doubleValue());

                    // 分类统计
                    String category = item.category;
                    if (category == null || category.isEmpty()) {
                        category = I18nManager.getInstance().get(I18nKeys.Report.UNCATEGORIZED);
                    }
                    categoryCountMap.put(category, categoryCountMap.getOrDefault(category, 0) + item.quantity);
                    categoryAmountMap.put(category, categoryAmountMap.getOrDefault(category, 0.0) + item.getPrice().multiply(BigDecimal.valueOf(item.quantity)).doubleValue());
                }
            }

            // 小时统计
            try {
                java.time.format.DateTimeFormatter sdf = com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME;
                int hour = java.time.LocalDateTime.parse(t.timestamp, sdf)
                    .atZone(ZoneId.systemDefault())
                    .getHour();
                hourCountMap.put(hour, hourCountMap.getOrDefault(hour, 0) + 1);
                hourAmountMap.put(hour, hourAmountMap.getOrDefault(hour, 0.0) + t.getFinalAmount().doubleValue());
            } catch (Exception e) {
                // 解析失败，跳过
            }
        }

        // 更新UI
        totalSalesLabel.setText(CurrencyUtil.format(totalSales));
        transactionCountLabel.setText(String.valueOf(transactionCount));
        avgTransactionLabel.setText(transactionCount > 0 ? CurrencyUtil.format(totalSales / transactionCount) : CurrencyUtil.format(0));
        memberSalesLabel.setText(CurrencyUtil.format(memberSales));
        cashSalesLabel.setText(CurrencyUtil.format(cashSales));
        wechatSalesLabel.setText(CurrencyUtil.format(wechatSales));
        alipaySalesLabel.setText(CurrencyUtil.format(alipaySales));
        cardSalesLabel.setText(CurrencyUtil.format(cardSales));

        // 找出销售最多的商品
        if (!productCountMap.isEmpty()) {
            String topProduct = Collections.max(productCountMap.entrySet(), Map.Entry.comparingByValue()).getKey();
            topProductLabel.setText(topProduct);
            topProductCountLabel.setText(String.valueOf(productCountMap.get(topProduct)));
            topProductAmountLabel.setText(CurrencyUtil.format(productAmountMap.get(topProduct)));
        } else {
            topProductLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Statistics.NO_DATA));
            topProductCountLabel.setText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.MemberEdit.POINTS_HINT));
            topProductAmountLabel.setText(CurrencyUtil.format(0));
        }

        // 更新分类表格
        updateCategoryTable(categoryCountMap, categoryAmountMap);

        // 更新小时表格
        updateHourlyTable(hourCountMap, hourAmountMap);

        // 更新图表
        updateCharts(cashSales, wechatSales, alipaySales, cardSales, transactions, categoryCountMap, categoryAmountMap);
    }

    /**
     * 更新分类表格
     */
    private void updateCategoryTable(Map<String, Integer> countMap, Map<String, Double> amountMap) {
        javafx.collections.ObservableList<StatisticsRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (String category : countMap.keySet()) {
            list.add(new StatisticsRecord(category, countMap.get(category), amountMap.get(category)));
        }

        // 按金额排序
        list.sort((a, b) -> Double.compare(b.amount, a.amount));
        categoryTable.setItems(list);
    }

    /**
     * 更新小时表格
     */
    private void updateHourlyTable(Map<Integer, Integer> countMap, Map<Integer, Double> amountMap) {
        javafx.collections.ObservableList<StatisticsRecord> list = javafx.collections.FXCollections.observableArrayList();

        for (int hour = 0; hour < 24; hour++) {
            if (countMap.containsKey(hour)) {
                list.add(new StatisticsRecord(
                    String.format("%02d:00-%02d:00", hour, hour + 1),
                    countMap.get(hour),
                    amountMap.get(hour)
                ));
            }
        }

        hourlyTable.setItems(list);
    }

    /**
     * 处理导出
     */
    @FXML
    public void handleExport() {
        if (allTransactions == null || allTransactions.isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_statistics"));
            return;
        }

        // 显示导出格式选择对话框
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>(
            "Excel", "Excel", "PDF"
        );
        formatDialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.EXPORT_FORMAT));
        formatDialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.PLEASE_SELECT_FORMAT));
        formatDialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.FORMAT_LABEL));

        formatDialog.showAndWait().ifPresent(format -> {
            com.cashier.util.ExportUtil.ExportFormat exportFormat =
                "Excel".equals(format) ? com.cashier.util.ExportUtil.ExportFormat.EXCEL
                                      : com.cashier.util.ExportUtil.ExportFormat.PDF;

            exportStatistics(exportFormat);
        });
    }

    /**
     * 导出统计数据
     */
    private void exportStatistics(com.cashier.util.ExportUtil.ExportFormat format) {
        try {
            // 准备表头
            java.util.List<String> headers = java.util.Arrays.asList(
                "统计项目", "数值"
            );

            // 准备数据
            java.util.List<String[]> data = new java.util.ArrayList<>();

            // 添加汇总统计数据
            data.add(new String[]{"总体销售额", totalSalesLabel.getText()});
            data.add(new String[]{"交易数量", transactionCountLabel.getText()});
            data.add(new String[]{"平均客单价", avgTransactionLabel.getText()});
            data.add(new String[]{"会员销售额", memberSalesLabel.getText()});
            data.add(new String[]{"现金销售额", cashSalesLabel.getText()});
            data.add(new String[]{"微信销售额", wechatSalesLabel.getText()});
            data.add(new String[]{"支付宝销售额", alipaySalesLabel.getText()});
            data.add(new String[]{"银行卡销售额", cardSalesLabel.getText()});
            data.add(new String[]{"", ""}); // 空行分隔

            // 添加商品排名
            data.add(new String[]{"热销商品", topProductLabel.getText()});
            data.add(new String[]{"热销商品销量", topProductCountLabel.getText()});
            data.add(new String[]{"热销商品销售额", topProductAmountLabel.getText()});
            data.add(new String[]{"", ""}); // 空行分隔

            // 添加分类统计
            data.add(new String[]{"分类销售统计", ""});
            for (StatisticsRecord record : categoryTable.getItems()) {
                data.add(new String[]{
                    "  " + record.name,
                    String.format("%d 笔, ¥%.2f", record.count, record.amount)
                });
            }
            data.add(new String[]{"", ""}); // 空行分隔

            // 添加时段统计
            data.add(new String[]{"时段销售统计", ""});
            for (StatisticsRecord record : hourlyTable.getItems()) {
                data.add(new String[]{
                    "  " + record.name,
                    String.format("%d 笔, ¥%.2f", record.count, record.amount)
                });
            }

            // 导出数据
            String timeRange = startDatePicker.getValue() + " 至 " + endDatePicker.getValue();
            String filePath = com.cashier.util.ExportUtil.export(
                "数据统计报表 (" + timeRange + ")",
                headers,
                data,
                format,
                "数据统计"
            );

            if (filePath != null) {
                com.cashier.util.StatusBarManager.updateSuccess(
                    com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                successAlert.setHeaderText(null);
                successAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_SUCCESS_PATH) + "\n" + filePath);
                successAlert.showAndWait();
                logger.info("统计数据导出成功: {}", filePath);
            } else {
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_FAILED));
            }
        } catch (Exception e) {
            logger.error("导出统计数据失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_FAILED_DETAIL, e.getMessage()));
        }
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        com.cashier.util.StatusBarManager.updateError(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Label.ERROR));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 更新图表
     */
    private void updateCharts(double cashSales, double wechatSales, double alipaySales, double cardSales,
                             List<Transaction> transactions, Map<String, Integer> categoryCountMap,
                             Map<String, Double> categoryAmountMap) {
        // 更新支付方式分布饼图
        updatePaymentMethodPieChart(cashSales, wechatSales, alipaySales, cardSales);

        // 更新销售趋势折线图
        updateSalesTrendLineChart(transactions);

        // 更新分类销售柱状图
        updateCategorySalesBarChart(categoryCountMap, categoryAmountMap);
    }

    /**
     * 更新支付方式分布饼图
     */
    private void updatePaymentMethodPieChart(double cashSales, double wechatSales, double alipaySales, double cardSales) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        if (cashSales > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_CASH), cashSales));
        }
        if (wechatSales > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_WECHAT), wechatSales));
        }
        if (alipaySales > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_ALIPAY), alipaySales));
        }
        if (cardSales > 0) {
            pieChartData.add(new PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_CARD), cardSales));
        }
        
        paymentMethodPieChart.setData(pieChartData);
        paymentMethodPieChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("statistics.payment_distribution"));
        paymentMethodPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
        
        // 为每个数据项添加百分比显示
        for (PieChart.Data data : pieChartData) {
            double total = cashSales + wechatSales + alipaySales + cardSales;
            double percentage = (data.getPieValue() / total) * 100;
            data.setName(String.format("%s (%.1f%%)", data.getName(), percentage));
        }
    }

    /**
     * 更新销售趋势折线图
     */
    private void updateSalesTrendLineChart(List<Transaction> transactions) {
        // 按日期统计销售额
        Map<String, Double> dailySalesMap = new TreeMap<>();
        java.time.format.DateTimeFormatter formatter = com.cashier.util.DateTimeFormats.DATE;
        
        for (Transaction t : transactions) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(t.timestamp, formatter);
                String dateStr = date.format(formatter);
                dailySalesMap.put(dateStr, dailySalesMap.getOrDefault(dateStr, 0.0) + t.getFinalAmount().doubleValue());
            } catch (Exception e) {
                // 解析失败，跳过
            }
        }
        
        // 创建数据系列
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(I18nManager.getInstance().get(I18nKeys.Chart.SERIES_SALES));
        
        for (Map.Entry<String, Double> entry : dailySalesMap.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        salesTrendLineChart.getData().clear();
        salesTrendLineChart.getData().add(series);
        salesTrendLineChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("statistics.sales_trend"));
        salesTrendLineChart.getXAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.DATE));
        salesTrendLineChart.getYAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.SERIES_SALES));
        salesTrendLineChart.setCreateSymbols(true);
        salesTrendLineChart.setLegendVisible(true);
    }

    /**
     * 更新分类销售柱状图
     */
    private void updateCategorySalesBarChart(Map<String, Integer> categoryCountMap, Map<String, Double> categoryAmountMap) {
        // 创建数据系列
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(I18nManager.getInstance().get(I18nKeys.Chart.SERIES_SALES));
        
        // 按销售额排序
        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryAmountMap.entrySet());
        sortedCategories.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 限制显示前10个分类
        int maxCategories = Math.min(sortedCategories.size(), 10);
        for (int i = 0; i < maxCategories; i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        
        categorySalesBarChart.getData().clear();
        categorySalesBarChart.getData().add(series);
        categorySalesBarChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.chart.category_sales"));
        categorySalesBarChart.getXAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.CATEGORY));
        categorySalesBarChart.getYAxis().setLabel(I18nManager.getInstance().get(I18nKeys.Chart.SERIES_SALES));
        categorySalesBarChart.setLegendVisible(true);
    }

    /**
     * 统计记录内部类
     */
    private static class StatisticsRecord {
        String name;
        int count;
        double amount;

        public StatisticsRecord(String name, int count, double amount) {
            this.name = name;
            this.count = count;
            this.amount = amount;
        }
    }
}
