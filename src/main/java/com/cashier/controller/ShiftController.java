package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.dao.ShiftDAO;
import com.cashier.i18n.I18nManager;
import com.cashier.dao.TransactionDAO;
import com.cashier.model.Shift;
import com.cashier.model.Transaction;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.DateTimeFormats;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.ZoneId;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.*;

/**
 * 交接班控制器
 * 处理交接班记录的查询和显示
 */
public class ShiftController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(ShiftController.class);
    private static final String BEGINNING_OF_TIME = "0000-01-01 00:00:00";

    @FXML
    private TableView<Shift> shiftTable;

    @FXML
    private TableColumn<Shift, String> shiftIdColumn;

    @FXML
    private TableColumn<Shift, String> operatorColumn;

    @FXML
    private TableColumn<Shift, String> timeColumn;

    @FXML
    private TableColumn<Shift, String> durationColumn;

    @FXML
    private TableColumn<Shift, String> transactionColumn;

    @FXML
    private TableColumn<Shift, String> revenueColumn;

    @FXML
    private TableColumn<Shift, String> paymentColumn;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField searchField;

    @FXML
    private Label countLabel;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private Label totalTransactionLabel;

    @FXML
    private BarChart<String, Number> shiftRevenueBarChart;

    @FXML
    private PieChart paymentMethodPieChart;

    @FXML
    private Button viewDetailButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button startShiftButton;

    @FXML
    private Button endShiftButton;

    private ObservableList<Shift> shiftList;
    private List<Shift> allShifts;
    private com.cashier.model.User currentUser;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置默认日期范围（今天）
        java.time.LocalDate today = java.time.LocalDate.now();
        startDatePicker.setValue(today);
        endDatePicker.setValue(today);

        // 设置表格列
        setupTableColumns();

        // 初始化图表
        initializeCharts();

        // 加载交接班数据
        loadShifts();

        // 设置表格选择模式
        shiftTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 添加表格选择监听
        shiftTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );

        // 设置行点击事件
        shiftTable.setRowFactory(tv -> {
            TableRow<Shift> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Shift shift = row.getItem();
                    if (shift != null) {
                        showShiftDetail(shift);
                    }
                }
            });
            return row;
        });

        // 更新开班/交班按钮状态
        updateShiftButtonStates();
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        shiftIdColumn.setCellValueFactory(new PropertyValueFactory<>("shiftId"));
        operatorColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().operatorName));
        timeColumn.setCellValueFactory(cellData -> {
            Shift s = cellData.getValue();
            java.time.format.DateTimeFormatter sdf = com.cashier.util.DateTimeFormats.SHIFT_TIME;
            return new SimpleStringProperty(I18nManager.getInstance().get("shift.time_range",
                sdf.format(s.startTime != null ? s.startTime.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : java.time.LocalDateTime.now()),
                sdf.format(s.endTime != null ? s.endTime.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : java.time.LocalDateTime.now())));
        });
        durationColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDurationText()));
        transactionColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().shiftTransactionCount)));
        revenueColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(CurrencyUtil.format(cellData.getValue().shiftRevenue.doubleValue())));
        paymentColumn.setCellValueFactory(cellData -> {
            Shift s = cellData.getValue();
            return new SimpleStringProperty(I18nManager.getInstance().get("shift.payment_summary",
                CurrencyUtil.format(s.cashRevenue.doubleValue()),
                CurrencyUtil.format(s.wechatRevenue.doubleValue()),
                CurrencyUtil.format(s.alipayRevenue.doubleValue()),
                CurrencyUtil.format(s.cardRevenue.doubleValue())));
        });
    }

    /**
     * 初始化图表
     */
    private void initializeCharts() {
        // 班次收入对比柱状图
        shiftRevenueBarChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("shift.revenue_chart"));
        shiftRevenueBarChart.getXAxis().setLabel(I18nManager.getInstance().get("chart.shift"));
        shiftRevenueBarChart.getYAxis().setLabel(I18nManager.getInstance().get("chart.revenue"));
        shiftRevenueBarChart.setLegendVisible(false);

        // 支付方式分布饼图
        paymentMethodPieChart.setTitle(com.cashier.i18n.I18nManager.getInstance().get("statistics.payment_distribution"));
        paymentMethodPieChart.setLegendSide(javafx.geometry.Side.RIGHT);
    }

    /**
     * 加载交接班数据
     */
    private void loadShifts() {
        logger.info("ShiftController: 开始加载交接班数据...");
        try {
            allShifts = ShiftDAO.findAll();
        } catch (SQLException e) {
            logger.error("加载交接班数据失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            allShifts = new java.util.ArrayList<>();
        }
        shiftList = FXCollections.observableArrayList(allShifts);
        shiftTable.setItems(shiftList);
        updateStatistics();
        logger.info("ShiftController: 加载了 {} 条交接班记录", allShifts.size());
    }

    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        countLabel.setText(I18nManager.getInstance().get("runtime.shift_count", shiftList.size()));

        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalTransaction = 0;

        for (Shift s : shiftList) {
            // 只统计有效的正数数据，忽略负数或无效数据
            if (s.getShiftRevenue().compareTo(BigDecimal.ZERO) > 0) {
                totalRevenue = totalRevenue.add(s.getShiftRevenue());
            }
            if (s.shiftTransactionCount > 0) {
                totalTransaction += s.shiftTransactionCount;
            }
        }

        totalRevenueLabel.setText(I18nManager.getInstance().get("runtime.shift_total_revenue", CurrencyUtil.format(totalRevenue.doubleValue())));
        totalTransactionLabel.setText(I18nManager.getInstance().get("runtime.shift_total_transactions", totalTransaction));

        // 更新图表
        updateCharts(shiftList);
    }

    /**
     * 更新图表
     */
    private void updateCharts(ObservableList<Shift> shifts) {
        // 更新班次收入柱状图
        updateShiftRevenueChart(shifts);

        // 更新支付方式饼图
        updatePaymentMethodPieChart(shifts);
    }

    /**
     * 更新班次收入柱状图
     */
    private void updateShiftRevenueChart(ObservableList<Shift> shifts) {
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();

        // 限制显示最近的10个班次
        int limit = Math.min(10, shifts.size());
        for (int i = 0; i < limit; i++) {
            Shift shift = shifts.get(i);
            String label = I18nManager.getInstance().get("shift.chart_item", i + 1);
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(label, shift.shiftRevenue));
        }

        shiftRevenueBarChart.getData().clear();
        shiftRevenueBarChart.getData().add(series);
    }

    /**
     * 更新支付方式饼图
     */
    private void updatePaymentMethodPieChart(ObservableList<Shift> shifts) {
        BigDecimal totalCash = BigDecimal.ZERO;
        BigDecimal totalWechat = BigDecimal.ZERO;
        BigDecimal totalAlipay = BigDecimal.ZERO;
        BigDecimal totalCard = BigDecimal.ZERO;

        for (Shift shift : shifts) {
            totalCash = totalCash.add(shift.getCashRevenue().compareTo(BigDecimal.ZERO) > 0 ? shift.getCashRevenue() : BigDecimal.ZERO);
            totalWechat = totalWechat.add(shift.getWechatRevenue().compareTo(BigDecimal.ZERO) > 0 ? shift.getWechatRevenue() : BigDecimal.ZERO);
            totalAlipay = totalAlipay.add(shift.getAlipayRevenue().compareTo(BigDecimal.ZERO) > 0 ? shift.getAlipayRevenue() : BigDecimal.ZERO);
            totalCard = totalCard.add(shift.getCardRevenue().compareTo(BigDecimal.ZERO) > 0 ? shift.getCardRevenue() : BigDecimal.ZERO);
        }

        javafx.collections.ObservableList<javafx.scene.chart.PieChart.Data> pieChartData =
            javafx.collections.FXCollections.observableArrayList();

        if (totalCash.compareTo(BigDecimal.ZERO) > 0) {
            pieChartData.add(new javafx.scene.chart.PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_CASH), totalCash.doubleValue()));
        }
        if (totalWechat.compareTo(BigDecimal.ZERO) > 0) {
            pieChartData.add(new javafx.scene.chart.PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_WECHAT), totalWechat.doubleValue()));
        }
        if (totalAlipay.compareTo(BigDecimal.ZERO) > 0) {
            pieChartData.add(new javafx.scene.chart.PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_ALIPAY), totalAlipay.doubleValue()));
        }
        if (totalCard.compareTo(BigDecimal.ZERO) > 0) {
            pieChartData.add(new javafx.scene.chart.PieChart.Data(I18nManager.getInstance().get(I18nKeys.Runtime.PAYMENT_CARD), totalCard.doubleValue()));
        }

        paymentMethodPieChart.setData(pieChartData);
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = shiftTable.getSelectionModel().getSelectedItem() != null;
        viewDetailButton.setDisable(!hasSelection);
    }

    /**
     * 处理查看详情
     */
    @FXML
    public void handleViewDetail() {
        Shift selected = shiftTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showShiftDetail(selected);
        }
    }

    /**
     * 显示交接班详情
     * @param shift 交接班记录
     */
    private void showShiftDetail(Shift shift) {
        java.time.format.DateTimeFormatter sdf = com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME;
        I18nManager i18n = I18nManager.getInstance();

        StringBuilder detail = new StringBuilder();
        detail.append(i18n.get("shift.detail_title")).append("\n\n");
        detail.append(i18n.get("shift.detail_id")).append(shift.shiftId).append("\n");
        detail.append(i18n.get("shift.detail_operator")).append(shift.operatorName).append("\n");
        detail.append(i18n.get("shift.detail_start")).append(shift.startTime != null ? shift.startTime.atZone(java.time.ZoneId.systemDefault()).format(sdf) : i18n.get("shift.not_started")).append("\n");
        detail.append(i18n.get("shift.detail_end")).append(shift.endTime != null ? shift.endTime.atZone(java.time.ZoneId.systemDefault()).format(sdf) : i18n.get("shift.not_ended")).append("\n");
        detail.append(i18n.get("shift.detail_duration")).append(shift.getDurationText()).append("\n\n");

        detail.append(i18n.get("shift.revenue_stats")).append("\n");
        detail.append(i18n.get("shift.opening_revenue")).append(CurrencyUtil.format(shift.openingRevenue.doubleValue())).append("\n");
        detail.append(i18n.get("shift.closing_revenue")).append(CurrencyUtil.format(shift.closingRevenue.doubleValue())).append("\n");
        detail.append(i18n.get("shift.current_revenue")).append(CurrencyUtil.format(shift.shiftRevenue.doubleValue())).append("\n\n");

        detail.append(i18n.get("shift.transaction_stats")).append("\n");
        detail.append(i18n.get("shift.opening_transactions")).append(shift.openingTransactionCount).append("\n");
        detail.append(i18n.get("shift.closing_transactions")).append(shift.closingTransactionCount).append("\n");
        detail.append(i18n.get("shift.current_transactions")).append(shift.shiftTransactionCount).append("\n\n");

        detail.append(i18n.get("shift.payment_stats")).append("\n");
        detail.append(i18n.get("shift.cash_revenue")).append(CurrencyUtil.format(shift.cashRevenue.doubleValue())).append("\n");
        detail.append(i18n.get("shift.wechat_revenue")).append(CurrencyUtil.format(shift.wechatRevenue.doubleValue())).append("\n");
        detail.append(i18n.get("shift.alipay_revenue")).append(CurrencyUtil.format(shift.alipayRevenue.doubleValue())).append("\n");
        detail.append(i18n.get("shift.card_revenue")).append(CurrencyUtil.format(shift.cardRevenue.doubleValue())).append("\n\n");

        if (shift.notes != null && !shift.notes.isEmpty()) {
            detail.append(i18n.get("shift.notes_label")).append(shift.notes).append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Label.TRANSACTION_DETAIL));
        alert.setHeaderText(null);
        alert.setContentText(detail.toString());
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    /**
     * 处理搜索
     */
    @FXML
    public void handleSearch() {
        applyFilters();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
        startDatePicker.setValue(java.time.LocalDate.now());
        endDatePicker.setValue(java.time.LocalDate.now());
        searchField.clear();
        applyFilters();
    }

    /**
     * 应用筛选条件
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();

        shiftList.setAll(allShifts.stream()
            .filter(s -> {
                // 日期筛选
                if (startDatePicker.getValue() != null || endDatePicker.getValue() != null) {
                    java.time.LocalDate shiftDate = s.startTime != null
                        ? s.startTime.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        : null;

                    if (startDatePicker.getValue() != null && shiftDate.isBefore(startDatePicker.getValue())) {
                        return false;
                    }
                    if (endDatePicker.getValue() != null && shiftDate.isAfter(endDatePicker.getValue())) {
                        return false;
                    }
                }

                // 搜索文本筛选（操作员姓名）
                if (!searchText.isEmpty()) {
                    return s.operatorName.toLowerCase().contains(searchText) ||
                           s.shiftId.toLowerCase().contains(searchText);
                }

                return true;
            })
            .toList());

        updateStatistics();
    }

    /**
     * 处理导出
     */
    @FXML
    public void handleExport() {
        if (shiftList.isEmpty()) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_export_shifts"));
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

            exportShifts(exportFormat);
        });
    }

    /**
     * 导出交接班记录
     */
    private void exportShifts(com.cashier.util.ExportUtil.ExportFormat format) {
        try {
            // 准备表头
            java.util.List<String> headers = java.util.Arrays.asList(
                "班次编号", "操作员", "开始时间", "结束时间", "班次时长", "交易数量",
                "总收入", "现金收入", "微信收入", "支付宝收入", "银行卡收入", "备注"
            );

            // 准备数据
            java.util.List<String[]> data = new java.util.ArrayList<>();
            java.time.format.DateTimeFormatter sdf = com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME;

            for (Shift s : shiftList) {
                // 格式化时间，处理 NULL 值
                String startTimeStr = (s.startTime != null) ? sdf.format(s.startTime.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()) : "未开始";
                String endTimeStr = (s.endTime != null) ? sdf.format(s.endTime.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()) : "未结束";
                
                // 计算班次时长，处理 NULL 值
                String durationText = "未完成";
                if (s.startTime != null && s.endTime != null) {
                    durationText = s.getDurationText();
                }

                data.add(new String[]{
                    s.shiftId,
                    s.operatorName,
                    startTimeStr,
                    endTimeStr,
                    durationText,
                    String.valueOf(s.shiftTransactionCount),
                    CurrencyUtil.format(s.shiftRevenue.doubleValue()),
                    CurrencyUtil.format(s.cashRevenue.doubleValue()),
                    CurrencyUtil.format(s.wechatRevenue.doubleValue()),
                    CurrencyUtil.format(s.alipayRevenue.doubleValue()),
                    CurrencyUtil.format(s.cardRevenue.doubleValue()),
                    s.notes == null || s.notes.isEmpty() ? "无" : s.notes
                });
            }

            // 导出数据
            String filePath = com.cashier.util.ExportUtil.export(
                "交接班报表",
                headers,
                data,
                format,
                "交接班记录"
            );

            if (filePath != null) {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Success.EXPORT));
                successAlert.setHeaderText(null);
                successAlert.setContentText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_SUCCESS_PATH) + "\n" + filePath);
                successAlert.showAndWait();
                updateStatus("导出成功");
            } else {
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.EXPORT_FAILED));
            }
        } catch (Exception e) {
            logger.error("导出交接班记录失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.EXPORT_FAILED_DETAIL, e.getMessage()));
        }
    }

    /**
     * 处理刷新
     */
    @FXML
    public void handleRefresh() {
        loadShifts();
        updateStatus("已刷新");
    }

    /**
     * 更新状态
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    /**
     * 刷新交接班列表
     */
    public void refreshShifts() {
        loadShifts();
    }

    /**
     * 更新开班/交班按钮状态
     */
    private void updateShiftButtonStates() {
        boolean hasActiveShift = false;
        try {
            hasActiveShift = ShiftDAO.hasActiveShift();
        } catch (SQLException e) {
            logger.error("检查活跃班次失败", e);
            hasActiveShift = false;
        }
        startShiftButton.setDisable(hasActiveShift);
        endShiftButton.setDisable(!hasActiveShift);
    }

    /**
     * 处理开班
     */
    @FXML
    public void handleStartShift() {
        // 检查是否已有活跃班次
        try {
            if (ShiftDAO.hasActiveShift()) {
                showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.shift_already_active"));
                return;
            }
        } catch (SQLException e) {
            logger.error("检查活跃班次失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.shift_check_failed"));
            return;
        }

        // 确认开班
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
        alert.setHeaderText(null);
        alert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.shift_start_confirm"));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // 获取当前用户
            if (currentUser == null) {
                showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.current_user_missing"));
                return;
            }

            // 获取当前累计营业额和交易数，数据库侧聚合，避免加载全量交易对象。
            BigDecimal totalRevenue;
            int totalTransactions;
            try {
                String now = java.time.LocalDateTime.now().format(DateTimeFormats.STANDARD_DATE_TIME);
                totalRevenue = BigDecimal.valueOf(TransactionDAO.getTotalRevenue(BEGINNING_OF_TIME, now));
                totalTransactions = TransactionDAO.getTransactionCount(BEGINNING_OF_TIME, now);
            } catch (SQLException e) {
                logger.error("加载交易统计失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
                return;
            }

            // 生成班次ID
            String shiftId = "SHIFT" + java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
                .format(com.cashier.util.DateTimeFormats.COMPACT_DATE_TIME);

            // 创建新班次
            Shift shift = new Shift(
                shiftId,
                currentUser.username,
                currentUser.name,
                java.time.Instant.now(),
                totalRevenue,
                totalTransactions
            );

            // 保存班次到数据库
            try {
                ShiftDAO.insert(shift);
            } catch (SQLException e) {
                logger.error("保存班次失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.SAVE_DATA) + ": " + e.getMessage());
                return;
            }

            // 刷新列表
            loadShifts();
            updateShiftButtonStates();

            showSuccess(I18nManager.getInstance().get("runtime.shift_started", shiftId));

            // 更新主界面的班次信息
            MainController.updateShiftInfoGlobal();

        } catch (Exception e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED) + ": " + e.getMessage());
            logger.error("开班失败", e);
        }
    }

    /**
     * 处理交班
     */
    @FXML
    public void handleEndShift() {
        // 检查是否有活跃班次
        Shift activeShift = null;
try {
            activeShift = ShiftDAO.findActiveShift();
        } catch (SQLException e) {
            logger.error("获取活跃班次失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED) + ": " + e.getMessage());
            return;
        }

        if (activeShift == null) {
            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.no_active_shift_short"));
            return;
        }

        // 确认交班
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
        alert.setHeaderText(null);
        alert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.shift_end_confirm"));

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            // 只加载本班次开始后的交易记录。
            List<Transaction> shiftTransactions;
            try {
                shiftTransactions = TransactionDAO.findByDateRange(
                    activeShift.startTime.atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                        .format(DateTimeFormats.STANDARD_DATE_TIME),
                    java.time.LocalDateTime.now().format(DateTimeFormats.STANDARD_DATE_TIME)
                );
            } catch (SQLException e) {
                logger.error("加载交易记录失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
                return;
            }

            BigDecimal cashRevenue = BigDecimal.ZERO;
            BigDecimal wechatRevenue = BigDecimal.ZERO;
            BigDecimal alipayRevenue = BigDecimal.ZERO;
            BigDecimal cardRevenue = BigDecimal.ZERO;
            BigDecimal totalRevenue = BigDecimal.ZERO;

            for (Transaction t : shiftTransactions) {
                totalRevenue = totalRevenue.add(t.getFinalAmount());

                // 按支付方式分类统计
                if ("现金".equals(t.paymentMethod) || "CASH".equals(t.paymentMethod)) {
                    cashRevenue = cashRevenue.add(t.getFinalAmount());
                } else if ("微信".equals(t.paymentMethod) || "WECHAT".equals(t.paymentMethod)) {
                    wechatRevenue = wechatRevenue.add(t.getFinalAmount());
                } else if ("支付宝".equals(t.paymentMethod) || "ALIPAY".equals(t.paymentMethod)) {
                    alipayRevenue = alipayRevenue.add(t.getFinalAmount());
                } else if ("银行卡".equals(t.paymentMethod) || "CARD".equals(t.paymentMethod)) {
                    cardRevenue = cardRevenue.add(t.getFinalAmount());
                }
            }

            // 结束班次
            // 计算班次结束时的累计总营业额和总交易数
            BigDecimal closingRevenue = activeShift.getOpeningRevenue().add(totalRevenue);
            int closingTransactionCount = activeShift.openingTransactionCount + shiftTransactions.size();
            activeShift.endShift(closingRevenue, closingTransactionCount, cashRevenue, wechatRevenue, alipayRevenue, cardRevenue);

            // 保存班次到数据库
            try {
                ShiftDAO.update(activeShift);
            } catch (SQLException e) {
                logger.error("更新班次失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.SAVE_DATA) + ": " + e.getMessage());
                return;
            }

            // 刷新列表
            loadShifts();
            updateShiftButtonStates();

            // 显示交班详情
            I18nManager i18n = I18nManager.getInstance();
            String sym = CurrencyUtil.getSymbol();
            String detail = String.format(
                i18n.get(I18nKeys.Success.SHIFT_END) + "\n\n" +
                i18n.get("label.shift_id") + ": %s\n" +
                i18n.get("label.operator") + ": %s\n" +
                i18n.get("label.shift_duration") + ": %s\n" +
                i18n.get("label.transaction_count") + ": %d\n" +
                i18n.get("label.revenue") + ": " + sym + "%.2f\n\n" +
                i18n.get("label.payment_detail") + ":\n" +
                i18n.get("label.cash") + ": " + sym + "%.2f\n" +
                i18n.get("label.wechat") + ": " + sym + "%.2f\n" +
                i18n.get("label.alipay") + ": " + sym + "%.2f\n" +
                i18n.get("label.card") + ": " + sym + "%.2f",
                activeShift.shiftId,
                activeShift.operatorName,
                activeShift.getDurationText(),
                activeShift.shiftTransactionCount,
                activeShift.shiftRevenue,
                cashRevenue,
                wechatRevenue,
                alipayRevenue,
                cardRevenue
            );

            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle(i18n.get(I18nKeys.Success.SHIFT_END));
            successAlert.setHeaderText(null);
            successAlert.setContentText(detail);
            successAlert.getDialogPane().setPrefWidth(500);
            successAlert.showAndWait();

            // 更新主界面的班次信息
            MainController.updateShiftInfoGlobal();

            // 退出登录
            handleLogout();

        } catch (Exception e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED) + ": " + e.getMessage());
            logger.error("交班失败", e);
        }
    }

    /**
     * 设置当前用户
     * @param user 用户
     */
    public void setCurrentUser(com.cashier.model.User user) {
        this.currentUser = user;
    }

    /**
     * 处理退出登录
     */
    public void handleLogout() {
        try {
            // 返回登录界面
            javafx.application.Platform.runLater(() -> {
                com.cashier.CashierSystemFXApplication application = com.cashier.CashierSystemFXApplication.getInstance();
                if (application != null) {
                    application.logoutToLoginView();
                }
            });
        } catch (Exception e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Message.OPERATION_FAILED) + ": " + e.getMessage());
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
     * 显示成功信息
     * @param message 成功消息
     */
    private void showSuccess(String message) {
        com.cashier.util.StatusBarManager.updateSuccess(message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Label.SUCCESS));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
