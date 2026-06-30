package com.cashier.controller;

import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.service.ReturnService;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.StatusBarManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 创建退货订单对话框控制器
 */
public class CreateReturnOrderDialogController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(CreateReturnOrderDialogController.class);

    @FXML private Label transactionIdLabel;
    @FXML private Label transactionAmountLabel;
    @FXML private Label transactionDateLabel;
    @FXML private Label paymentMethodLabel;
    @FXML private Label memberNameLabel;

    @FXML private CheckBox selectAllCheckBox;
    @FXML private TableView<ReturnItem> returnItemTable;
    @FXML private TableColumn<ReturnItem, Boolean> selectColumn;
    @FXML private TableColumn<ReturnItem, String> productCodeColumn;
    @FXML private TableColumn<ReturnItem, String> productNameColumn;
    @FXML private TableColumn<ReturnItem, Integer> originalQuantityColumn;
    @FXML private TableColumn<ReturnItem, Integer> returnQuantityColumn;
    @FXML private TableColumn<ReturnItem, Double> unitPriceColumn;
    @FXML private TableColumn<ReturnItem, Double> returnAmountColumn;
    @FXML private TableColumn<ReturnItem, String> conditionColumn;

    @FXML private TextField returnReasonField;
    @FXML private ComboBox<String> refundMethodComboBox;
    @FXML private TextArea notesArea;
    @FXML private Label totalReturnAmountLabel;

    private ObservableList<ReturnItem> returnItems = FXCollections.observableArrayList();
    private Transaction originalTransaction;
    private Stage dialogStage;
    private boolean submitted = false;
    private User currentUser;

    // 退货商品临时类
    public static class ReturnItem {
        private BooleanProperty selected = new SimpleBooleanProperty(false);
        public int productId;
        public String productCode;
        public String productName;
        public int originalQuantity;
        public int returnQuantity;
        public double unitPrice;
        public String condition = "GOOD";

        public ReturnItem() {
            selected.addListener((obs, oldVal, newVal) -> {
                if (!newVal) {
                    returnQuantity = 0;
                }
            });
        }

        public BooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        // Getter方法供PropertyValueFactory使用
        public int getProductId() {
            return productId;
        }

        public String getProductCode() {
            return productCode;
        }

        public String getProductName() {
            return productName;
        }

        public int getOriginalQuantity() {
            return originalQuantity;
        }

        public int getReturnQuantity() {
            return returnQuantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public String getCondition() {
            return condition;
        }

        public double getReturnAmount() {
            return returnQuantity * unitPrice;
        }
    }

    @FXML
    public void initialize() {
        logger.info("初始化创建退货订单对话框");

        // 初始化退款方式下拉框
        refundMethodComboBox.setItems(FXCollections.observableArrayList(
            "CASH", "WECHAT", "ALIPAY", "CARD"
        ));
        com.cashier.util.I18nUiUtils.configureComboBox(
            refundMethodComboBox, com.cashier.util.I18nUiUtils::paymentMethod);
        refundMethodComboBox.setValue("CASH");

        // 初始化表格列
        initializeTableColumns();

        // 初始化全选复选框
        selectAllCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (ReturnItem item : returnItems) {
                item.setSelected(newVal);
            }
            returnItemTable.refresh();
            calculateTotal();
        });
    }

    private void initializeTableColumns() {
        // 选择列（复选框）
        selectColumn.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        // 商品编号
        productCodeColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProductCode()));

        // 商品名称
        productNameColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProductName()));

        // 原数量
        originalQuantityColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getOriginalQuantity()));

        // 退货数量（可编辑）
        returnQuantityColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getReturnQuantity()));
        returnQuantityColumn.setCellFactory(column -> new TableCell<ReturnItem, Integer>() {
            private Spinner<Integer> spinner;

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                ReturnItem returnItem = getTableRow().getItem();

                if (spinner == null) {
                    spinner = new Spinner<>(0, returnItem.originalQuantity, returnItem.returnQuantity);
                    spinner.setEditable(true);
                    spinner.setPrefWidth(80);

                    spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (returnItem.isSelected()) {
                            returnItem.returnQuantity = newVal;
                            calculateTotal();
                        }
                    });

                    returnItem.selectedProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal) {
                            spinner.setDisable(false);
                        } else {
                            spinner.setDisable(true);
                            spinner.getValueFactory().setValue(0);
                        }
                    });
                }

                spinner.getValueFactory().setValue(returnItem.returnQuantity);
                spinner.setDisable(!returnItem.isSelected());
                setGraphic(spinner);
            }
        });

        // 单价
        unitPriceColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getUnitPrice()));
        unitPriceColumn.setCellFactory(column -> new TableCell<ReturnItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CurrencyUtil.format(item));
                }
            }
        });

        // 退货金额
        returnAmountColumn.setCellValueFactory(cellData ->
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getReturnAmount()));
        returnAmountColumn.setCellFactory(column -> new TableCell<ReturnItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(CurrencyUtil.format(item));
                }
            }
        });

        // 商品状态（可编辑）
        conditionColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.ReadOnlyObjectWrapper<>(cellData.getValue().getCondition()));
        conditionColumn.setCellFactory(column -> new TableCell<ReturnItem, String>() {
            private ComboBox<String> comboBox;

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                ReturnItem returnItem = getTableRow().getItem();

                if (comboBox == null) {
                    comboBox = new ComboBox<>(FXCollections.observableArrayList(
                        "GOOD", "DAMAGED", "OPENED"
                    ));
                    com.cashier.util.I18nUiUtils.configureComboBox(
                        comboBox, com.cashier.util.I18nUiUtils::itemCondition);
                    comboBox.setMinWidth(118);
                    comboBox.setPrefWidth(128);

                    comboBox.setOnAction(event -> {
                        returnItem.condition = comboBox.getValue();
                    });
                }

                comboBox.setValue(returnItem.condition);
                setGraphic(comboBox);
            }
        });

        returnItemTable.setItems(returnItems);
    }

    /**
     * 设置原交易信息
     */
    public void setOriginalTransaction(Transaction transaction, List<Product> items) {
        this.originalTransaction = transaction;

        // 显示原交易信息
        transactionIdLabel.setText(transaction.transactionId);
        transactionAmountLabel.setText(CurrencyUtil.format(transaction.finalAmount.doubleValue()));
        transactionDateLabel.setText(transaction.timestamp);  // timestamp已经是格式化的字符串
        paymentMethodLabel.setText(com.cashier.util.I18nUiUtils.paymentMethod(transaction.paymentMethod));
        memberNameLabel.setText(transaction.memberName != null ? transaction.memberName : com.cashier.i18n.I18nManager.getInstance().get("statistics.no_data"));

        // 设置退款方式
        refundMethodComboBox.setValue(transaction.paymentMethod);

        // 加载退货商品
        loadReturnItems(items);
    }

    /**
     * 加载退货商品列表
     */
    private void loadReturnItems(List<Product> items) {
        returnItems.clear();

        for (Product product : items) {
            ReturnItem returnItem = new ReturnItem();
            returnItem.productId = product.id;
            returnItem.productCode = product.productCode != null ? product.productCode : "";
            returnItem.productName = product.name;
            returnItem.originalQuantity = product.quantity;
            returnItem.returnQuantity = product.quantity;
            returnItem.unitPrice = product.getPrice().doubleValue();
            returnItems.add(returnItem);
        }

        returnItemTable.refresh();
        calculateTotal();
    }

    /**
     * 计算退货总金额
     */
    private void calculateTotal() {
        double total = 0;
        for (ReturnItem item : returnItems) {
            if (item.isSelected()) {
                total += item.getReturnAmount();
            }
        }
        totalReturnAmountLabel.setText(CurrencyUtil.format(total));
    }

    /**
     * 处理全选
     */
    @FXML
    public void handleSelectAll() {
        // 逻辑已在 initialize() 中实现
    }

    /**
     * 处理取消
     */
    @FXML
    public void handleCancel() {
        submitted = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * 验证退货商品 - 防止重复退货
     */
    private String validateReturnItems() {
        try {
            // 查询该交易的所有已存在退货订单（不包括已拒绝的）
            List<ReturnOrder> existingReturns = ReturnOrderDAO.findByOriginalTransactionId(
                originalTransaction.transactionId
            );

            if (existingReturns.isEmpty()) {
                return null;  // 没有退货记录，允许创建
            }

            // 统计所有已退货的商品和数量
            java.util.Map<Integer, Integer> returnedQuantities = new java.util.HashMap<>();
            for (ReturnOrder returnOrder : existingReturns) {
                List<ReturnOrderItem> items = ReturnOrderItemDAO.findByReturnOrderId(returnOrder.returnOrderId);
                for (ReturnOrderItem item : items) {
                    returnedQuantities.put(item.productId,
                        returnedQuantities.getOrDefault(item.productId, 0) + item.returnQuantity);
                }
            }

            // 检查当前要退货的商品是否会超过原交易数量
            for (ReturnItem item : returnItems) {
                if (item.isSelected() && item.returnQuantity > 0) {
                    int returnedQty = returnedQuantities.getOrDefault(item.productId, 0);
                    int totalReturnQty = returnedQty + item.returnQuantity;

                    if (totalReturnQty > item.originalQuantity) {
                        return com.cashier.i18n.I18nManager.getInstance().get(
                            "runtime.return_quantity_exceeded", item.productName,
                            item.originalQuantity, returnedQty, item.returnQuantity);
                    }
                }
            }

            return null;  // 验证通过

        } catch (Exception e) {
            logger.error("验证退货商品失败", e);
            return com.cashier.i18n.I18nManager.getInstance().get("runtime.return_validation_error", e.getMessage());
        }
    }

    /**
     * 处理提交
     */
    @FXML
    public void handleSubmit() {
        // 验证输入
        String returnReason = returnReasonField.getText().trim();
        if (returnReason.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get("inventory_alert.info"), com.cashier.i18n.I18nManager.getInstance().get("return_order.return_reason_hint"));
            return;
        }

        // 检查是否选择了退货商品
        boolean hasSelectedItem = false;
        for (ReturnItem item : returnItems) {
            if (item.isSelected() && item.returnQuantity > 0) {
                hasSelectedItem = true;
                break;
            }
        }

        if (!hasSelectedItem) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get("inventory_alert.info"), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_item_required"));
            return;
        }

        // 验证退货订单 - 防止重复退货
        String validationResult = validateReturnItems();
        if (validationResult != null) {
            showAlert(Alert.AlertType.WARNING, com.cashier.i18n.I18nManager.getInstance().get("runtime.return_validation_failed"), validationResult);
            return;
        }

        // 获取会员ID（根据手机号查询）
        Integer memberId = null;
        if (originalTransaction.memberPhone != null && !originalTransaction.memberPhone.trim().isEmpty()) {
            try {
                Member member = MemberDAO.findByPhone(originalTransaction.memberPhone);
                if (member != null) {
                    memberId = member.id;
                }
            } catch (SQLException e) {
                logger.error("查询会员信息失败: {}", e.getMessage());
            }
        }

        // 创建退货订单
        ReturnOrder returnOrder = new ReturnOrder();
        returnOrder.originalTransactionId = originalTransaction.transactionId;
        returnOrder.memberId = memberId;  // 可以为null
        returnOrder.memberName = originalTransaction.memberName;
        returnOrder.returnReason = returnReason;
        returnOrder.paymentMethod = getPaymentMethodCode(refundMethodComboBox.getValue());
        returnOrder.operatorName = getOperatorName();
        returnOrder.notes = notesArea.getText().trim();

        // 创建退货订单明细
        List<ReturnOrderItem> items = new ArrayList<>();
        for (ReturnItem item : returnItems) {
            if (item.isSelected() && item.returnQuantity > 0) {
                ReturnOrderItem returnItem = new ReturnOrderItem();
                returnItem.productId = item.productId;
                returnItem.productCode = item.productCode;
                returnItem.productName = item.productName;
                returnItem.returnQuantity = item.returnQuantity;
                returnItem.unitPrice = BigDecimal.valueOf(item.unitPrice);
                returnItem.returnAmount = BigDecimal.valueOf(item.getReturnAmount());
                returnItem.condition = getConditionCode(item.condition);
                returnItem.reason = returnReason;
                returnItem.calculateAmount();
                items.add(returnItem);
            }
        }

        // 计算总金额
        returnOrder.totalAmount = items.stream()
            .map(ReturnOrderItem::getReturnAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 保存退货订单
        boolean result = ReturnService.createReturnOrder(returnOrder, items);

        if (result) {
            submitted = true;
            showAlert(Alert.AlertType.INFORMATION, com.cashier.i18n.I18nManager.getInstance().get("label.success"),
                com.cashier.i18n.I18nManager.getInstance().get("runtime.return_create_success",
                    returnOrder.returnOrderId, String.format("%.2f", returnOrder.totalAmount)));
            logger.info("退货订单创建成功: {}", returnOrder.returnOrderId);

            if (dialogStage != null) {
                dialogStage.close();
            }
        } else {
            showAlert(Alert.AlertType.ERROR, com.cashier.i18n.I18nManager.getInstance().get("label.failed"), com.cashier.i18n.I18nManager.getInstance().get("runtime.return_create_failed"));
        }
    }

    /**
     * 获取支付方式代码
     */
    private String getPaymentMethodCode(String text) {
        return text == null ? "CASH" : text;
    }

    /**
     * 获取商品状态代码
     */
    private String getConditionCode(String text) {
        return text == null ? "GOOD" : text;
    }

    /**
     * 获取操作员名称
     */
    private String getOperatorName() {
        return currentUser != null ? currentUser.name : "admin";
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * 设置对话框舞台
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * 是否已提交
     */
    public boolean isSubmitted() {
        return submitted;
    }

    /**
     * 显示提示
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        updateStatusForAlert(type, message);
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateStatusForAlert(Alert.AlertType type, String message) {
        if (type == Alert.AlertType.ERROR) {
            StatusBarManager.updateError(message);
        } else if (type == Alert.AlertType.WARNING) {
            StatusBarManager.updateWarning(message);
        } else if (type == Alert.AlertType.INFORMATION) {
            StatusBarManager.updateSuccess(message);
        }
    }
}
