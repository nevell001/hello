package com.cashier.controller;

import com.cashier.service.DataService;
import com.cashier.model.Promotion;
import com.cashier.util.StatusBarManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 促销管理控制器
 * 处理促销活动的增删改查
 */
public class PromotionController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PromotionController.class);

    @FXML
    private TableView<Promotion> promotionTable;

    @FXML
    private TableColumn<Promotion, String> nameColumn;

    @FXML
    private TableColumn<Promotion, String> typeColumn;

    @FXML
    private TableColumn<Promotion, String> descriptionColumn;

    @FXML
    private TableColumn<Promotion, String> periodColumn;

    @FXML
    private TableColumn<Promotion, String> usageColumn;

    @FXML
    private TableColumn<Promotion, String> statusColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> typeFilterComboBox;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private Label countLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button enableButton;

    @FXML
    private Button disableButton;

    private ObservableList<Promotion> promotionList;
    private List<Promotion> allPromotions;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化类型筛选下拉框
        typeFilterComboBox.setItems(FXCollections.observableArrayList(
            "全部",
            "满减",
            "打折",
            "优惠券"
        ));
        typeFilterComboBox.getSelectionModel().select(0);

        // 初始化状态筛选下拉框
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
            "全部",
            "启用",
            "禁用"
        ));
        statusFilterComboBox.getSelectionModel().select(0);

        // 设置表格列
        setupTableColumns();

        // 加载促销数据
        loadPromotions();

        // 设置表格选择模式
        promotionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 添加表格选择监听
        promotionTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        periodColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            LocalDate startDate = p.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate endDate = p.endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return new SimpleStringProperty(String.format("%s 至 %s",
                startDate.format(formatter),
                endDate.format(formatter)));
        });

        usageColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            String maxUsage = p.maxUsage == -1 ? "无限制" : String.valueOf(p.maxUsage);
            return new SimpleStringProperty(String.format("%d/%s", p.usageCount, maxUsage));
        });

        statusColumn.setCellValueFactory(cellData -> {
            Promotion p = cellData.getValue();
            String status = p.enabled ? "启用" : "禁用";
            String validity = p.isValid() ? "有效" : "已过期";
            return new SimpleStringProperty(status + " (" + validity + ")");
        });
    }

    /**
     * 加载促销数据
     */
    private void loadPromotions() {
        logger.info("PromotionController: 开始加载促销数据...");
        allPromotions = DataService.loadPromotions();
        promotionList = FXCollections.observableArrayList(allPromotions);
        promotionTable.setItems(promotionList);
        updateCountLabel();
        logger.info("PromotionController: 加载了 {} 条促销记录", allPromotions.size());
    }

    /**
     * 更新促销数量标签
     */
    private void updateCountLabel() {
        countLabel.setText("促销数量: " + promotionList.size());
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = !promotionTable.getSelectionModel().getSelectedItems().isEmpty();
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        enableButton.setDisable(!hasSelection);
        disableButton.setDisable(!hasSelection);
    }

    /**
     * 处理添加促销
     */
    @FXML
    public void handleAddPromotion() {
        showPromotionDialog(null);
    }

    /**
     * 处理编辑促销
     */
    @FXML
    public void handleEditPromotion() {
        Promotion selected = promotionTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showPromotionDialog(selected);
        }
    }

    /**
     * 显示促销对话框
     * @param promotion 要编辑的促销，null表示添加新促销
     */
    private void showPromotionDialog(Promotion promotion) {
        Dialog<Promotion> dialog = new Dialog<>();
        dialog.setTitle(promotion == null ? "添加促销" : "编辑促销");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(760);
        dialog.getDialogPane().getStyleClass().add("promotion-dialog");
        if (promotionTable.getScene() != null) {
            dialog.initOwner(promotionTable.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(promotionTable.getScene().getStylesheets());
        }

        // 创建对话框内容
        VBox content = new VBox(14);
        content.getStyleClass().add("dialog-content");

        Label titleLabel = new Label(promotion == null ? "添加促销" : "编辑促销");
        titleLabel.getStyleClass().add("view-title");

        GridPane grid = new GridPane();
        grid.getStyleClass().addAll("form-grid", "dialog-form-grid");
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 0, 0));
        grid.getColumnConstraints().addAll(
            createLabelColumn(),
            createFieldColumn(),
            createLabelColumn(),
            createFieldColumn()
        );

        TextField promotionCodeField = new TextField();
        TextField nameField = new TextField();
        ComboBox<String> typeComboBox = new ComboBox<>();
        TextField thresholdField = new TextField();
        TextField discountField = new TextField();
        TextArea descriptionArea = new TextArea();
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField maxUsageField = new TextField();
        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.getStyleClass().add("error-label");

        stylePromotionDialogControls(
            promotionCodeField,
            nameField,
            typeComboBox,
            thresholdField,
            discountField,
            descriptionArea,
            startDatePicker,
            endDatePicker,
            maxUsageField
        );

        typeComboBox.setItems(FXCollections.observableArrayList("满减", "打折", "优惠券"));
        typeComboBox.setButtonCell(createPromotionTypeCell());
        typeComboBox.setCellFactory(listView -> createPromotionTypeCell());
        typeComboBox.getSelectionModel().select("满减");
        thresholdField.setPromptText("例如: 100，表示消费满100元");
        discountField.setPromptText("满减填减免金额，例如: 10");
        maxUsageField.setPromptText("空着表示不限制");
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(30));

        if (promotion != null) {
            promotionCodeField.setText(promotion.promotionCode);
            promotionCodeField.setDisable(true);  // 编辑时禁用促销编号
            nameField.setText(promotion.name);
            typeComboBox.getSelectionModel().select(promotion.type);
            thresholdField.setText(String.valueOf(promotion.threshold));
            discountField.setText(String.valueOf(promotion.discount));
            descriptionArea.setText(promotion.description);
            startDatePicker.setValue(promotion.startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            endDatePicker.setValue(promotion.endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            maxUsageField.setText(promotion.maxUsage == -1 ? "" : String.valueOf(promotion.maxUsage));
        } else {
            // 新建促销时自动生成编号
            promotionCodeField.setText(generatePromotionCode());
            promotionCodeField.setDisable(true);  // 禁用促销编号字段
            thresholdField.setText("0");
        }

        typeComboBox.valueProperty().addListener((obs, oldType, newType) -> {
            updatePromotionFieldHints(newType, thresholdField, discountField);
        });
        updatePromotionFieldHints(typeComboBox.getSelectionModel().getSelectedItem(), thresholdField, discountField);

        grid.add(createFormLabel("促销编号:"), 0, 0);
        grid.add(promotionCodeField, 1, 0);
        grid.add(createFormLabel("促销名称:*"), 2, 0);
        grid.add(nameField, 3, 0);
        grid.add(createFormLabel("促销类型:*"), 0, 1);
        grid.add(typeComboBox, 1, 1);
        grid.add(createFormLabel("最大次数:"), 2, 1);
        grid.add(maxUsageField, 3, 1);
        grid.add(createFormLabel("门槛金额:*"), 0, 2);
        grid.add(thresholdField, 1, 2);
        grid.add(createFormLabel("优惠值:*"), 2, 2);
        grid.add(discountField, 3, 2);
        grid.add(createFormLabel("开始日期:*"), 0, 3);
        grid.add(startDatePicker, 1, 3);
        grid.add(createFormLabel("结束日期:*"), 2, 3);
        grid.add(endDatePicker, 3, 3);
        grid.add(createFormLabel("描述:"), 0, 4);
        grid.add(descriptionArea, 1, 4, 3, 1);
        grid.add(errorLabel, 1, 5, 3, 1);

        content.getChildren().addAll(titleLabel, grid);
        dialog.getDialogPane().setContent(content);

        ButtonType okButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.getStyleClass().addAll("primary-button", "button-normal");
        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().addAll("secondary-button", "button-normal");
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Promotion result = buildPromotionFromForm(
                    promotion,
                    promotionCodeField,
                    nameField,
                    typeComboBox,
                    thresholdField,
                    discountField,
                    descriptionArea,
                    startDatePicker,
                    endDatePicker,
                    maxUsageField
                );
                errorLabel.setText("");
                dialog.setResult(result);
                dialog.close();
                event.consume();
            } catch (IllegalArgumentException e) {
                logger.info("促销数据验证失败: {}", e.getMessage());
                errorLabel.setText(e.getMessage());
                event.consume();
            } catch (Exception e) {
                logger.error("保存促销失败", e);
                errorLabel.setText("保存促销时发生错误，请检查输入后重试。原因: " + e.getMessage());
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                if (promotion == null) {
                    allPromotions.add(result);
                }
                DataService.savePromotions(allPromotions);
                loadPromotions();
                updateStatus(promotion == null ? "促销添加成功" : "促销更新成功");
            } catch (Exception e) {
                logger.error("保存促销失败", e);
                showAlert("保存失败", "保存促销时发生错误: " + e.getMessage());
            }
        });
    }

    /**
     * 生成促销编号
     */
    private String generatePromotionCode() {
        return "P" + System.currentTimeMillis();
    }

    private ColumnConstraints createLabelColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(92);
        column.setPrefWidth(92);
        column.setHgrow(Priority.NEVER);
        return column;
    }

    private ColumnConstraints createFieldColumn() {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(220);
        column.setPrefWidth(240);
        column.setHgrow(Priority.ALWAYS);
        return column;
    }

    private Label createFormLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private void stylePromotionDialogControls(TextField promotionCodeField,
                                              TextField nameField,
                                              ComboBox<String> typeComboBox,
                                              TextField thresholdField,
                                              TextField discountField,
                                              TextArea descriptionArea,
                                              DatePicker startDatePicker,
                                              DatePicker endDatePicker,
                                              TextField maxUsageField) {
        TextField[] textFields = {
            promotionCodeField, nameField, thresholdField, discountField, maxUsageField
        };
        for (TextField field : textFields) {
            field.getStyleClass().add("form-input");
            field.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(field, Priority.ALWAYS);
        }

        thresholdField.getStyleClass().add("input-amount");
        discountField.getStyleClass().add("input-amount");
        maxUsageField.getStyleClass().add("input-number");
        promotionCodeField.getStyleClass().add("input-code");

        typeComboBox.getStyleClass().add("form-combo");
        typeComboBox.setMinWidth(200);
        typeComboBox.setPrefWidth(220);
        typeComboBox.setMinHeight(36);
        typeComboBox.setPrefHeight(36);
        typeComboBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(typeComboBox, Priority.ALWAYS);

        DatePicker[] datePickers = {startDatePicker, endDatePicker};
        for (DatePicker picker : datePickers) {
            picker.getStyleClass().add("promotion-date-picker");
            picker.setMinWidth(200);
            picker.setPrefWidth(220);
            picker.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(picker, Priority.ALWAYS);
        }

        descriptionArea.getStyleClass().add("form-textarea");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(descriptionArea, Priority.ALWAYS);
    }

    private ListCell<String> createPromotionTypeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setAlignment(Pos.CENTER_LEFT);
                setMinHeight(34);
                setPrefHeight(34);
            }
        };
    }

    private void updatePromotionFieldHints(String type, TextField thresholdField, TextField discountField) {
        if ("打折".equals(type)) {
            thresholdField.setPromptText("例如: 100，表示满100元可打折");
            discountField.setPromptText("请输入0到1之间的小数，例如0.9表示9折");
        } else if ("优惠券".equals(type)) {
            thresholdField.setPromptText("优惠券不设门槛时填0");
            discountField.setPromptText("请输入优惠券面额，例如: 20");
            if (thresholdField.getText().trim().isEmpty()) {
                thresholdField.setText("0");
            }
        } else {
            thresholdField.setPromptText("例如: 100，表示消费满100元");
            discountField.setPromptText("请输入减免金额，例如: 10");
        }
    }

    private Promotion buildPromotionFromForm(Promotion promotion,
                                             TextField promotionCodeField,
                                             TextField nameField,
                                             ComboBox<String> typeComboBox,
                                             TextField thresholdField,
                                             TextField discountField,
                                             TextArea descriptionArea,
                                             DatePicker startDatePicker,
                                             DatePicker endDatePicker,
                                             TextField maxUsageField) {
        String code = promotionCodeField.getText().trim();
        String name = nameField.getText().trim();
        String type = typeComboBox.getSelectionModel().getSelectedItem();
        String thresholdText = thresholdField.getText().trim();
        String discountText = discountField.getText().trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("请填写促销名称，例如“满100减10”或“会员日9折”。");
        }
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("请选择促销类型：满减、打折或优惠券。");
        }
        if (thresholdText.isEmpty()) {
            throw new IllegalArgumentException("请填写门槛金额。优惠券不设门槛时请填0。");
        }
        if (discountText.isEmpty()) {
            throw new IllegalArgumentException(getDiscountRequiredMessage(type));
        }
        if (startDatePicker.getValue() == null) {
            throw new IllegalArgumentException("请选择开始日期。");
        }
        if (endDatePicker.getValue() == null) {
            throw new IllegalArgumentException("请选择结束日期。");
        }
        if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            throw new IllegalArgumentException("结束日期不能早于开始日期，请调整促销有效期。");
        }

        BigDecimal threshold = parseDecimal(thresholdText, "门槛金额");
        BigDecimal discount = parseDecimal(discountText, "优惠值");

        if (threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("门槛金额不能为负数。优惠券不设门槛时请填0。");
        }
        validateDiscountValue(type, threshold, discount);

        String maxUsageText = maxUsageField.getText().trim();
        int maxUsage = -1;
        if (!maxUsageText.isEmpty()) {
            try {
                maxUsage = FormValidator.parseInt(maxUsageText);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("最大使用次数必须是整数，例如100；不限制次数时请留空。");
            }
            if (maxUsage <= 0) {
                throw new IllegalArgumentException("最大使用次数必须大于0；不限制次数时请留空。");
            }
        }

        Promotion newPromotion = promotion != null ? promotion : new Promotion();
        newPromotion.promotionCode = code.isEmpty() ? generatePromotionCode() : code;
        newPromotion.name = name;
        newPromotion.type = type;
        newPromotion.threshold = threshold;
        newPromotion.discount = discount;
        newPromotion.description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
        newPromotion.startDate = java.sql.Date.valueOf(startDatePicker.getValue());
        newPromotion.endDate = java.sql.Date.valueOf(endDatePicker.getValue());
        newPromotion.maxUsage = maxUsage;

        return newPromotion;
    }

    private BigDecimal parseDecimal(String text, String fieldName) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须是数字，请不要输入中文、空格或货币符号。");
        }
    }

    private String getDiscountRequiredMessage(String type) {
        if ("打折".equals(type)) {
            return "请填写折扣率，例如0.9表示9折，0.85表示8.5折。";
        }
        if ("优惠券".equals(type)) {
            return "请填写优惠券面额，例如20表示减20元。";
        }
        return "请填写减免金额，例如10表示减10元。";
    }

    private void validateDiscountValue(String type, BigDecimal threshold, BigDecimal discount) {
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(getDiscountRequiredMessage(type));
        }
        if ("打折".equals(type)) {
            if (discount.compareTo(BigDecimal.ONE) >= 0) {
                throw new IllegalArgumentException("打折促销的折扣率必须大于0且小于1，例如0.9表示9折。");
            }
        } else if ("满减".equals(type) && threshold.compareTo(BigDecimal.ZERO) > 0
                && discount.compareTo(threshold) > 0) {
            throw new IllegalArgumentException("满减金额不能大于门槛金额，例如满100最多减100。");
        }
    }

    /**
     * 显示警告对话框
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 处理删除促销
     */
    @FXML
    public void handleDeletePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText(null);
        alert.setContentText("确定要删除选中的 " + selected.size() + " 个促销吗？");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            allPromotions.removeAll(selected);
            DataService.savePromotions(allPromotions);
            loadPromotions();
            updateStatus("促销删除成功");
        }
    }

    /**
     * 处理启用促销
     */
    @FXML
    public void handleEnablePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        for (Promotion p : selected) {
            p.enabled = true;
        }
        DataService.savePromotions(allPromotions);
        loadPromotions();
        updateStatus("促销已启用");
    }

    /**
     * 处理禁用促销
     */
    @FXML
    public void handleDisablePromotion() {
        List<Promotion> selected = promotionTable.getSelectionModel().getSelectedItems();
        for (Promotion p : selected) {
            p.enabled = false;
        }
        DataService.savePromotions(allPromotions);
        loadPromotions();
        updateStatus("促销已禁用");
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
        searchField.clear();
        typeFilterComboBox.getSelectionModel().select(0);
        statusFilterComboBox.getSelectionModel().select(0);
        applyFilters();
    }

    /**
     * 应用筛选条件
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String typeFilter = typeFilterComboBox.getSelectionModel().getSelectedItem();
        String statusFilter = statusFilterComboBox.getSelectionModel().getSelectedItem();

        promotionList.setAll(allPromotions.stream()
            .filter(p -> {
                // 类型筛选
                if (!"全部".equals(typeFilter) && !typeFilter.equals(p.type)) {
                    return false;
                }

                // 状态筛选
                if (!"全部".equals(statusFilter)) {
                    boolean enabled = "启用".equals(statusFilter);
                    if (p.enabled != enabled) {
                        return false;
                    }
                }

                // 搜索文本筛选
                if (!searchText.isEmpty()) {
                    return p.name.toLowerCase().contains(searchText) ||
                           p.description.toLowerCase().contains(searchText);
                }

                return true;
            })
            .toList());

        updateCountLabel();
    }

    /**
     * 更新状态
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    /**
     * 刷新促销列表
     */
    public void refreshPromotions() {
        loadPromotions();
    }
}
