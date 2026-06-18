package com.cashier.controller;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.*;
import com.cashier.model.*;
import com.cashier.util.CurrencyUtil;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.IntegerStringConverter;

/**
 * 采购订单控制器
 * 处理采购订单的创建、编辑、提交审批
 */
@SuppressWarnings("unchecked")
public class PurchaseOrderController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PurchaseOrderController.class);
    private final com.cashier.dao.ProductDAORefactored productDAO = com.cashier.dao.DAOFactory.getInstance().getProductDAO();

    @FXML
    private TableView<PurchaseOrder> orderTable;

    @FXML
    private TableColumn<PurchaseOrder, String> orderNoColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> supplierColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> purchaseDateColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> totalAmountColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> statusColumn;

    @FXML
    private TableColumn<PurchaseOrder, String> purchaserColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilterCombo;

    @FXML
    private Label countLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button viewDetailButton;

    @FXML
    private Button submitApprovalButton;

    private ObservableList<PurchaseOrder> orderList;
    private Map<Integer, PurchaseOrder> orders;
    private Map<Integer, Supplier> suppliers;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置表格列
        setupTableColumns();

        // 设置状态筛选
        statusFilterCombo.getItems().addAll("全部", "待审批", "已审批", "已拒绝", "已完成");
        statusFilterCombo.setValue("全部");

        // 加载数据
        loadSuppliers();
        loadOrders();

        // 设置表格选择模式
        orderTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 添加表格选择监听
        orderTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        orderNoColumn.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        purchaseDateColumn.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        totalAmountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalAmount)));
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatusDisplayName()));
        purchaserColumn.setCellValueFactory(new PropertyValueFactory<>("purchaser"));
    }

    /**
     * 加载供应商数据
     */
    private void loadSuppliers() {
        try {
            List<Supplier> supplierData = SupplierDAO.findAll();
            suppliers = new HashMap<>();
            for (Supplier supplier : supplierData) {
                suppliers.put(supplier.id, supplier);
            }
        } catch (SQLException e) {
            logger.error("加载供应商数据失败", e);
            suppliers = new HashMap<>();
        }
    }

    /**
     * 加载采购订单数据
     */
    private void loadOrders() {
        try {
            List<PurchaseOrder> orderData = PurchaseOrderDAO.findAll();
            orders = new HashMap<>();
            for (PurchaseOrder order : orderData) {
                orders.put(order.id, order);
            }
        } catch (SQLException e) {
            logger.error("加载采购订单数据失败", e);
            showError(I18nManager.getInstance().get("runtime.purchase_order_load_failed", e.getMessage()));
            orders = new HashMap<>();
        }
        filterOrders();
    }

    /**
     * 筛选订单
     */
    private void filterOrders() {
        String statusFilter = statusFilterCombo.getValue();
        List<PurchaseOrder> filtered = orders.values().stream()
            .filter(order -> {
                if ("全部".equals(statusFilter)) return true;
                switch (statusFilter) {
                    case "待审批": return "pending".equals(order.status);
                    case "已审批": return "approved".equals(order.status);
                    case "已拒绝": return "rejected".equals(order.status);
                    case "已完成": return "completed".equals(order.status);
                    default: return true;
                }
            })
            .collect(Collectors.toList());

        orderList = FXCollections.observableArrayList(filtered);
        orderTable.setItems(orderList);
        updateCountLabel();
    }

    /**
     * 更新订单数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(I18nManager.getInstance().get("runtime.purchase_order_count", orderList.size()));
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        boolean hasSelection = selected != null;
        boolean canEdit = hasSelection && selected.canEdit();

        editButton.setDisable(!canEdit);
        deleteButton.setDisable(!canEdit);
        viewDetailButton.setDisable(!hasSelection);
        submitApprovalButton.setDisable(!canEdit);
    }

    /**
     * 处理添加订单
     */
    @FXML
    public void handleAddOrder() {
        showOrderDialog(null);
    }

    /**
     * 处理编辑订单
     */
    @FXML
    public void handleEditOrder() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showOrderDialog(selected);
        }
    }

    /**
     * 显示订单对话框
     */
    private void showOrderDialog(PurchaseOrder order) {
        try {
            // 创建对话框内容
            VBox root = new VBox(15);
            root.setPadding(new javafx.geometry.Insets(20));
            root.getStyleClass().add("surface-muted");

            // 表单字段
            GridPane gridPane = new GridPane();
            gridPane.setHgap(15);
            gridPane.setVgap(15);
            gridPane.setStyle("-fx-background-color: transparent;");

            // 设置列约束
            javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
            col1.setPrefWidth(100);
            col1.setMinWidth(90);
            
            javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
            col2.setPrefWidth(180);
            col2.setMinWidth(150);
            col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            
            javafx.scene.layout.ColumnConstraints col3 = new javafx.scene.layout.ColumnConstraints();
            col3.setPrefWidth(100);
            col3.setMinWidth(90);
            
            javafx.scene.layout.ColumnConstraints col4 = new javafx.scene.layout.ColumnConstraints();
            col4.setPrefWidth(180);
            col4.setMinWidth(150);
            col4.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            
            gridPane.getColumnConstraints().addAll(col1, col2, col3, col4);

            TextField orderNoField = new TextField();
            orderNoField.setEditable(false);
            orderNoField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("product.edit.auto_generate"));

            ComboBox<Supplier> supplierCombo = new ComboBox<>();
            supplierCombo.getItems().setAll(suppliers.values());
            supplierCombo.setConverter(new javafx.util.StringConverter<Supplier>() {
                @Override
                public String toString(Supplier supplier) {
                    if (supplier == null) {
                        return "";
                    }
                    // 显示格式：编号 - 名称 (等级)
                    return String.format("%s - %s (%s级)", 
                        supplier.supplierCode, 
                        supplier.name, 
                        supplier.rank);
                }
                @Override
                public Supplier fromString(String string) {
                    // 从字符串中提取供应商名称（格式：编号 - 名称 (等级)）
                    if (string == null || string.isEmpty()) {
                        return null;
                    }
                    String name = string;
                    int dashIndex = string.indexOf(" - ");
                    int spaceIndex = string.lastIndexOf(" (");
                    if (dashIndex != -1 && spaceIndex != -1 && spaceIndex > dashIndex) {
                        name = string.substring(dashIndex + 3, spaceIndex);
                    }
                    final String finalName = name;
                    return suppliers.values().stream()
                        .filter(s -> s.name.equals(finalName))
                        .findFirst()
                        .orElse(null);
                }
            });

            DatePicker purchaseDatePicker = new DatePicker();
            purchaseDatePicker.setValue(java.time.LocalDate.now());

            DatePicker expectedDatePicker = new DatePicker();
            expectedDatePicker.setValue(java.time.LocalDate.now().plusDays(7));

            TextField purchaserField = new TextField();
            purchaserField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("purchase_inbound.purchaser"));

            TextArea remarkArea = new TextArea();
            remarkArea.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("restock.reason"));
            remarkArea.setPrefRowCount(2);
            remarkArea.setPrefHeight(50);

            // 如果是编辑模式，填充数据
            boolean isEdit = order != null;
            if (isEdit) {
                orderNoField.setText(order.orderNo);
                Supplier supplier = suppliers.get(order.supplierId);
                if (supplier != null) {
                    supplierCombo.setValue(supplier);
                }
                purchaseDatePicker.setValue(java.time.LocalDate.parse(order.purchaseDate));
                if (order.expectedDate != null && !order.expectedDate.isEmpty()) {
                    expectedDatePicker.setValue(java.time.LocalDate.parse(order.expectedDate));
                }
                purchaserField.setText(order.purchaser);
                remarkArea.setText(order.remark);
            } else {
                // 自动生成订单号
                orderNoField.setText(generateOrderNo());
            }

            // 创建对话框Stage（需要在按钮回调之前声明）
            final Stage dialogStage = new Stage();
            dialogStage.setTitle(isEdit ? com.cashier.i18n.I18nManager.getInstance().get("runtime.purchase_order_edit") : com.cashier.i18n.I18nManager.getInstance().get("runtime.purchase_order_new"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            // 第一行：订单号和供应商
            gridPane.add(createLabel(I18nManager.getInstance().get("runtime.order_no")), 0, 0);
            gridPane.add(orderNoField, 1, 0);
            gridPane.add(createLabel(I18nManager.getInstance().get("runtime.supplier_required")), 2, 0);
            HBox supplierBox = new HBox(10);
            supplierBox.getChildren().add(supplierCombo);
            Button newSupplierButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_new"));
            newSupplierButton.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
            newSupplierButton.setOnAction(e -> showSupplierManagementDialog(dialogStage, supplierCombo));
            supplierBox.getChildren().add(newSupplierButton);
            gridPane.add(supplierBox, 3, 0);

            // 供应商详细信息显示区域
            Label supplierInfoLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_info"));
            supplierInfoLabel.getStyleClass().add("text-muted");
            supplierInfoLabel.setStyle("-fx-font-weight: bold;");
            Label supplierDetailLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_select"));
            supplierDetailLabel.getStyleClass().add("text-muted");
            supplierDetailLabel.setStyle("-fx-font-size: 12px;");
            supplierDetailLabel.setWrapText(true);

            // 监听供应商选择变化
            supplierCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    String info = I18nManager.getInstance().get("runtime.supplier_summary",
                            newVal.contactPerson != null ? newVal.contactPerson : "-",
                            newVal.phone != null ? newVal.phone : "-");
                    if (newVal.address != null && !newVal.address.isEmpty()) {
                        info = I18nManager.getInstance().get("runtime.supplier_summary_address", info, newVal.address);
                    }
                    supplierDetailLabel.setText(info);
                    supplierDetailLabel.getStyleClass().removeAll("text-muted", "text-default");
                    supplierDetailLabel.getStyleClass().add("text-default");
                } else {
                    supplierDetailLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_select"));
                    supplierDetailLabel.getStyleClass().removeAll("text-muted", "text-default");
                    supplierDetailLabel.getStyleClass().add("text-muted");
                }
            });

            // 第二行：采购日期和预计到货日期
            gridPane.add(createLabel(I18nManager.getInstance().get("runtime.purchase_date")), 0, 1);
            gridPane.add(purchaseDatePicker, 1, 1);
            gridPane.add(createLabel(I18nManager.getInstance().get("runtime.expected_date_full")), 2, 1);
            gridPane.add(expectedDatePicker, 3, 1);

            // 第三行：采购人和备注
            gridPane.add(createLabel(I18nManager.getInstance().get("runtime.purchaser")), 0, 2);
            gridPane.add(purchaserField, 1, 2);
            gridPane.add(createLabel(I18nManager.getInstance().get("runtime.notes")), 2, 2);
            gridPane.add(remarkArea, 3, 2);

            // 供应商详细信息显示行
            gridPane.add(supplierInfoLabel, 0, 3);
            gridPane.add(supplierDetailLabel, 1, 3, 3, 1); // 跨越3列

            // 商品列表表格
            TableView<PurchaseOrderItem> itemTable = new TableView<>();
            itemTable.setEditable(true);
            itemTable.setStyle("-fx-font-size: 13px;");
            itemTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<PurchaseOrderItem, String> productNameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("return_approval.product_name"));
            productNameCol.setPrefWidth(200);
            productNameCol.setStyle("-fx-font-weight: bold;");
            productNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<PurchaseOrderItem, Integer> quantityCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("cart.quantity"));
            quantityCol.setPrefWidth(100);
            quantityCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            quantityCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            quantityCol.setOnEditCommit(e -> {
                e.getRowValue().quantity = e.getNewValue();
                e.getRowValue().totalPrice = e.getRowValue().unitPrice.multiply(BigDecimal.valueOf(e.getNewValue()));
                itemTable.refresh();
                updateItemTotal(itemTable);
            });

            TableColumn<PurchaseOrderItem, BigDecimal> unitPriceCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("return_approval.unit_price"));
            unitPriceCol.setPrefWidth(100);
            unitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
            unitPriceCol.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
            unitPriceCol.setOnEditCommit(e -> {
                e.getRowValue().unitPrice = e.getNewValue();
                e.getRowValue().totalPrice = e.getRowValue().quantity > 0
                    ? e.getNewValue().multiply(BigDecimal.valueOf(e.getRowValue().quantity))
                    : BigDecimal.ZERO;
                itemTable.refresh();
                updateItemTotal(itemTable);
            });

            TableColumn<PurchaseOrderItem, String> totalPriceCol = new TableColumn<>(I18nManager.getInstance().get("runtime.subtotal"));
            totalPriceCol.setPrefWidth(100);
            totalPriceCol.setStyle("-fx-font-weight: bold;");
            totalPriceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalPrice)));

            TableColumn<PurchaseOrderItem, String> actionCol = new TableColumn<>(I18nManager.getInstance().get("runtime.action"));
            actionCol.setPrefWidth(80);
            actionCol.setCellFactory(col -> new TableCell<PurchaseOrderItem, String>() {
                private final Button deleteBtn = new Button(com.cashier.i18n.I18nManager.getInstance().get("inventory_check.delete"));
                {
                    deleteBtn.getStyleClass().add("danger-button");
                    deleteBtn.setStyle("-fx-font-weight: bold;");
                    deleteBtn.setOnAction(e -> {
                        PurchaseOrderItem item = getTableView().getItems().get(getIndex());
                        itemTable.getItems().remove(item);
                        updateItemTotal(itemTable);
                    });
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(deleteBtn);
                    }
                }
            });

            itemTable.getColumns().addAll(productNameCol, quantityCol, unitPriceCol, totalPriceCol, actionCol);

            ObservableList<PurchaseOrderItem> items = FXCollections.observableArrayList();
            itemTable.setItems(items);

            // 如果是编辑模式，加载订单明细
            if (isEdit) {
                try {
                    List<PurchaseOrderItem> orderItems = PurchaseOrderItemDAO.findByOrderId(order.id);
                    items.addAll(orderItems);
                } catch (SQLException ex) {
                    logger.error("加载订单明细失败", ex);
                }
                updateItemTotal(itemTable);
            }

            // 添加商品按钮
            Button addProductButton = new Button(I18nManager.getInstance().get("runtime.add_product"));
            addProductButton.getStyleClass().add("primary-button");
            addProductButton.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            addProductButton.setOnAction(e -> showProductSelector(itemTable));

            // 总金额标签
            Label totalLabel = new Label(I18nManager.getInstance().get("runtime.total_amount_value", CurrencyUtil.format(0)));
            totalLabel.getStyleClass().add("text-default");
            totalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            // 商品明细标签
            Label itemLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.product_details"));
            itemLabel.getStyleClass().add("text-default");
            itemLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            // 按钮
            Button saveButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("shortcut.save"));
            Button cancelButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("return_order.cancel"));

            saveButton.setOnAction(e -> {
                if (supplierCombo.getValue() == null) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_select"));
                    return;
                }
                if (items.isEmpty()) {
                    showError(I18nManager.getInstance().get("runtime.purchase_order_product_required"));
                    return;
                }

                Supplier supplier = supplierCombo.getValue();
                PurchaseOrder newOrder = new PurchaseOrder();
                newOrder.orderNo = orderNoField.getText();
                newOrder.supplierId = supplier.id;
                newOrder.supplierName = supplier.name;
                newOrder.purchaseDate = purchaseDatePicker.getValue().toString();
                newOrder.expectedDate = expectedDatePicker.getValue().toString();
                newOrder.totalAmount = calculateTotalAmount(items);
                newOrder.status = "pending";
                newOrder.purchaser = purchaserField.getText().trim();
                newOrder.remark = remarkArea.getText().trim();

                try {
                    if (isEdit) {
                        newOrder.id = order.id;
                        PurchaseOrderDAO.update(newOrder);
                        // 删除旧明细并插入新明细
                        PurchaseOrderItemDAO.deleteByOrderId(order.id);
                        for (PurchaseOrderItem item : items) {
                            item.orderId = order.id;
                            PurchaseOrderItemDAO.insert(item);
                        }
                        updateStatus("采购订单更新成功");
                    } else {
                        PurchaseOrderDAO.insert(newOrder);
                        for (PurchaseOrderItem item : items) {
                            item.orderId = newOrder.id;
                            PurchaseOrderItemDAO.insert(item);
                        }
                        updateStatus("采购订单创建成功");
                    }
                    loadOrders();
                    dialogStage.close();
                } catch (SQLException ex) {
                    logger.error("保存采购订单失败", ex);
                    showError(I18nManager.getInstance().get("runtime.purchase_order_save_failed", ex.getMessage()));
                }
            });

            cancelButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(10, saveButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(gridPane, itemLabel, addProductButton, itemTable, totalLabel, buttonBox);

            Scene scene = new Scene(root, 750, 550);
            applyCurrentTheme(scene);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.error("显示订单对话框失败", e);
            showError(I18nManager.getInstance().get("runtime.dialog_load_failed", e.getMessage()));
        }
    }

    /**
     * 显示商品选择器
     */
    private void showProductSelector(TableView<PurchaseOrderItem> itemTable) {
        try {
            Stage selectorStage = new Stage();
            selectorStage.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.select_product"));
            selectorStage.initModality(Modality.WINDOW_MODAL);
            selectorStage.initOwner(orderTable.getScene().getWindow());
            selectorStage.setWidth(650);
            selectorStage.setHeight(500);

            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(15));
            root.getStyleClass().add("surface-muted");

            // 搜索框和分类筛选
            TextField searchField = new TextField();
            searchField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.search_product_hint"));
            searchField.setPrefWidth(200);
            
            Label searchLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("purchase_inbound.search_label"));
            searchLabel.getStyleClass().add("text-default");
            searchLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            // 分类筛选
            ComboBox<String> categoryCombo = new ComboBox<>();
            categoryCombo.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.all_categories"));
            categoryCombo.setPrefWidth(150);
            categoryCombo.setStyle("-fx-border-radius: 4px; -fx-padding: 5px 10px;");
            
            Label categoryLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("product.edit.category"));
            categoryLabel.getStyleClass().add("text-default");
            categoryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            HBox searchBox = new HBox(10, searchLabel, searchField, categoryLabel, categoryCombo);
            searchBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // 加载分类数据
            List<Category> categories = CategoryDAO.findAll();
            ObservableList<String> categoryList = FXCollections.observableArrayList("全部分类");
            for (Category category : categories) {
                categoryList.add(category.name);
            }
            categoryCombo.setItems(categoryList);

            // 商品表格
            TableView<Product> productTable = new TableView<>();
            productTable.setStyle("-fx-font-size: 13px;");
            productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            productTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            
            // 添加复选框列
            TableColumn<Product, Boolean> selectColumn = new TableColumn<>();
            selectColumn.setPrefWidth(50);
            selectColumn.setSortable(false);
            selectColumn.setCellValueFactory(param -> new SimpleBooleanProperty(true)); // 总是显示复选框
            selectColumn.setCellFactory(col -> new TableCell<Product, Boolean>() {
                private final CheckBox checkBox = new CheckBox();
                
                {
                    // 设置复选框点击事件
                    checkBox.setOnAction(e -> {
                        if (!isEmpty()) {
                            Product product = getTableView().getItems().get(getIndex());
                            if (checkBox.isSelected()) {
                                getTableView().getSelectionModel().select(product);
                            } else {
                                getTableView().getSelectionModel().clearSelection(getIndex());
                            }
                        }
                    });
                }
                
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        // 根据当前选择状态更新复选框
                        Product product = getTableView().getItems().get(getIndex());
                        checkBox.setSelected(productTable.getSelectionModel().getSelectedItems().contains(product));
                        setGraphic(checkBox);
                    }
                }
            });
            productTable.getColumns().add(selectColumn);
            
            TableColumn<Product, String> nameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("return_approval.product_name"));
            nameCol.setPrefWidth(200);
            nameCol.setStyle("-fx-font-weight: bold;");
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            
            TableColumn<Product, String> barcodeCol = new TableColumn<>(I18nManager.getInstance().get("runtime.barcode"));
            barcodeCol.setPrefWidth(150);
            barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));
            
            TableColumn<Product, Number> costCol = new TableColumn<>(I18nManager.getInstance().get("runtime.cost_price"));
            costCol.setPrefWidth(100);
            costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));
            
            TableColumn<Product, Number> stockCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("product.stock"));
            stockCol.setPrefWidth(80);
            stockCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            
            productTable.getColumns().addAll(nameCol, barcodeCol, costCol, stockCol);
            
            // 加载商品数据
            List<Product> allProducts = productDAO.findAll();
            ObservableList<Product> productList = FXCollections.observableArrayList(allProducts);
            productTable.setItems(productList);

            // 监听选择状态变化，刷新表格以更新复选框显示
            productTable.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<Product>) c -> {
                Platform.runLater(() -> productTable.refresh());
            });

            // 搜索和分类筛选功能
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                filterProducts(productTable, allProducts, productList, newVal, categoryCombo.getValue());
            });
            
            categoryCombo.setOnAction(e -> {
                filterProducts(productTable, allProducts, productList, searchField.getText(), categoryCombo.getValue());
            });

            // 全选/取消全选按钮
            Button selectAllButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("shortcut.select_all"));
            selectAllButton.setOnAction(e -> {
                productTable.getSelectionModel().selectAll();
            });

            Button deselectAllButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("runtime.deselect_all"));
            deselectAllButton.setOnAction(e -> {
                productTable.getSelectionModel().clearSelection();
            });

            HBox selectButtonsBox = new HBox(10, selectAllButton, deselectAllButton);

            // 添加按钮
            Button addButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("runtime.add_selected_products"));
            addButton.getStyleClass().add("primary-button");
            addButton.setStyle("-fx-font-weight: bold;");
            addButton.setOnAction(e -> {
                ObservableList<Product> selectedProducts = productTable.getSelectionModel().getSelectedItems();
                if (selectedProducts == null || selectedProducts.isEmpty()) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.select_product_first"));
                    return;
                }
                
                int addedCount = 0;
                for (Product selected : selectedProducts) {
                    // 检查是否已经添加过
                    boolean exists = itemTable.getItems().stream()
                        .anyMatch(item -> item.productId == selected.id);
                    
                    if (!exists) {
                        PurchaseOrderItem item = new PurchaseOrderItem();
                        item.productId = selected.id;
                        item.productName = selected.name != null ? selected.name : "";
                        item.quantity = 1;
                        item.unitPrice = selected.getCost().compareTo(BigDecimal.ZERO) > 0 ? selected.getCost() : selected.getPrice();
                        item.totalPrice = item.unitPrice.multiply(BigDecimal.valueOf(item.quantity));
                        item.inboundQuantity = 0;
                        
                        itemTable.getItems().add(item);
                        addedCount++;
                    }
                }
                
                if (addedCount > 0) {
                    itemTable.refresh();
                    updateItemTotal(itemTable);
                    selectorStage.close();
                    logger.info("成功添加 {} 个商品到采购订单", addedCount);
                } else {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.product_already_added"));
                }
            });

            Button cancelButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("return_order.cancel"));
            cancelButton.getStyleClass().add("secondary-button");
            cancelButton.setStyle("-fx-font-weight: bold;");
            cancelButton.setOnAction(e -> selectorStage.close());

            HBox buttonBox = new HBox(10, addButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            VBox selectBox = new VBox(5, selectButtonsBox, buttonBox);
            selectBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(searchBox, productTable, selectBox);

            Scene scene = new Scene(root);
            applyCurrentTheme(scene);
            selectorStage.setScene(scene);
            selectorStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载商品选择器失败", e);
            showError(I18nManager.getInstance().get("runtime.product_load_short_failed", e.getMessage()));
        }
    }

    /**
     * 过滤商品列表
     */
    private void filterProducts(TableView<Product> productTable, List<Product> allProducts, 
                                ObservableList<Product> productList, String searchText, String category) {
        String searchLower = searchText.toLowerCase();
        String categoryLower = category != null ? category.toLowerCase() : "";
        
        List<Product> filtered = allProducts.stream()
            .filter(p -> {
                // 分类筛选
                if (!"全部分类".equals(categoryLower) && !"".equals(categoryLower)) {
                    if (p.category == null || !p.category.toLowerCase().contains(categoryLower)) {
                        return false;
                    }
                }
                // 搜索筛选
                if (!searchLower.isEmpty()) {
                    return p.name.toLowerCase().contains(searchLower) || 
                           (p.barcode != null && p.barcode.toLowerCase().contains(searchLower));
                }
                return true;
            })
            .collect(Collectors.toList());
        
        productTable.setItems(FXCollections.observableArrayList(filtered));
    }

    /**
     * 更新商品明细总金额
    /**
     * 更新商品明细总金额
     */
    private void updateItemTotal(TableView<PurchaseOrderItem> itemTable) {
        BigDecimal total = calculateTotalAmount(itemTable.getItems());
        // 更新总金额显示
        VBox parent = (VBox) itemTable.getParent();
        if (parent != null) {
            for (javafx.scene.Node node : parent.getChildren()) {
                if (node instanceof Label && ((Label) node).getText().startsWith("总金额:")) {
                    ((Label) node).setText(I18nManager.getInstance().get("runtime.total_amount_value", CurrencyUtil.format(total.doubleValue())));
                    break;
                }
            }
        }
    }

    /**
     * 计算总金额
     */
    private BigDecimal calculateTotalAmount(ObservableList<PurchaseOrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItem item : items) {
            total = total.add(item.totalPrice);
        }
        return total;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        String prefix = "PO" + dateStr;

        int maxSeq = 0;
        try {
            for (PurchaseOrder order : orders.values()) {
                if (order.orderNo != null && order.orderNo.startsWith(prefix)) {
                    String seqStr = order.orderNo.substring(prefix.length());
                    try {
                        int seq = FormValidator.parseInt(seqStr);
                        if (seq > maxSeq) {
                            maxSeq = seq;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return prefix + String.format("%04d", maxSeq + 1);
    }

    /**
     * 处理删除订单
     */
    @FXML
    public void handleDeleteOrder() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get("common.confirm"));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.purchase_order_delete_confirm", selected.orderNo));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    PurchaseOrderItemDAO.deleteByOrderId(selected.id);
                    PurchaseOrderDAO.delete(selected.id);
                    orders.remove(selected.id);
                    filterOrders();
                    updateStatus("采购订单删除成功");
                } catch (SQLException e) {
                    logger.error("删除采购订单失败", e);
                    showError(I18nManager.getInstance().get("runtime.purchase_order_delete_failed", e.getMessage()));
                }
            }
        }
    }

    /**
     * 处理查看详情
     */
    @FXML
    public void handleViewDetail() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showOrderDetailDialog(selected);
        }
    }

    /**
     * 显示订单详情对话框
     */
    private void showOrderDetailDialog(PurchaseOrder order) {
        try {
            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(20));

            // 订单信息
            GridPane infoPane = new GridPane();
            infoPane.setHgap(10);
            infoPane.setVgap(10);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("checkout.order_number")), 0, 0);
            infoPane.add(new Label(order.orderNo), 1, 0);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("product.edit.supplier")), 0, 1);
            infoPane.add(new Label(order.supplierName), 1, 1);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.purchase_date")), 0, 2);
            infoPane.add(new Label(order.purchaseDate), 1, 2);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.expected_date")), 0, 3);
            infoPane.add(new Label(order.expectedDate != null ? order.expectedDate : "-"), 1, 3);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.purchaser")), 0, 4);
            infoPane.add(new Label(order.purchaser != null ? order.purchaser : "-"), 1, 4);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("return_order_list.status_label")), 0, 5);
            infoPane.add(new Label(order.getStatusDisplayName()), 1, 5);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.total_amount")), 0, 6);
            infoPane.add(new Label(CurrencyUtil.format(order.totalAmount.doubleValue())), 1, 6);

            // 商品明细
            TableView<PurchaseOrderItem> itemTable = new TableView<>();
            TableColumn<PurchaseOrderItem, String> nameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("return_approval.product_name"));
            nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<PurchaseOrderItem, Number> qtyCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("cart.quantity"));
            qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            TableColumn<PurchaseOrderItem, Number> priceCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("return_approval.unit_price"));
            priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

            TableColumn<PurchaseOrderItem, String> totalCol = new TableColumn<>(I18nManager.getInstance().get("runtime.subtotal"));
            totalCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.2f", cellData.getValue().totalPrice)));

            itemTable.getColumns().addAll(nameCol, qtyCol, priceCol, totalCol);

            List<PurchaseOrderItem> items = PurchaseOrderItemDAO.findByOrderId(order.id);
            itemTable.setItems(FXCollections.observableArrayList(items));

            // 创建对话框Stage（需要在按钮回调之前声明）
            final Stage dialogStage = new Stage();
            dialogStage.setTitle(I18nManager.getInstance().get("runtime.purchase_order_detail_title", order.orderNo));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(orderTable.getScene().getWindow());

            Button closeButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("inventory_alert.close"));
            closeButton.setOnAction(e -> dialogStage.close());

            root.getChildren().addAll(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.order_info")), infoPane, new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.product_details")), itemTable, closeButton);

            Scene scene = new Scene(root, 600, 500);
            applyCurrentTheme(scene);

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载订单详情失败", e);
            showError(I18nManager.getInstance().get("runtime.purchase_order_detail_failed", e.getMessage()));
        }
    }

    /**
     * 处理提交审批
     */
    @FXML
    public void handleSubmitApproval() {
        PurchaseOrder selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get("common.confirm"));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.purchase_order_submit_confirm", selected.orderNo));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    // 订单状态已经是pending，不需要改变
                    updateStatus("订单已提交审批: " + selected.orderNo);
                } catch (Exception e) {
                    logger.error("提交审批失败", e);
                    showError(I18nManager.getInstance().get("runtime.purchase_order_submit_failed", e.getMessage()));
                }
            }
        }
    }

    /**
     * 处理搜索
     */
    @FXML
    public void handleSearch() {
        String searchText = searchField.getText().trim().toLowerCase();
        if (searchText.isEmpty()) {
            filterOrders();
        } else {
            String statusFilter = statusFilterCombo.getValue();
            List<PurchaseOrder> filtered = orders.values().stream()
                .filter(order -> {
                    if ("全部".equals(statusFilter)) return true;
                    switch (statusFilter) {
                        case "待审批": return "pending".equals(order.status);
                        case "已审批": return "approved".equals(order.status);
                        case "已拒绝": return "rejected".equals(order.status);
                        case "已完成": return "completed".equals(order.status);
                        default: return true;
                    }
                })
                .filter(order -> order.orderNo.toLowerCase().contains(searchText) ||
                         order.supplierName.toLowerCase().contains(searchText))
                .collect(Collectors.toList());

            orderList = FXCollections.observableArrayList(filtered);
            orderTable.setItems(orderList);
            updateCountLabel();
        }
    }

    /**
     * 处理状态筛选
     */
    @FXML
    public void handleStatusFilter() {
        filterOrders();
    }

    /**
     * 刷新订单列表
     */
    public void refreshOrders() {
        loadOrders();
    }

    /**
     * 更新状态
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    /**
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nManager.getInstance().get("label.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 创建带样式的标签
     * @param text 标签文字
     * @return Label对象
     */
    /**
     * 显示供应商管理对话框
     */
    private void showSupplierManagementDialog(Stage parentStage, ComboBox<Supplier> supplierCombo) {
        try {
            // 简化的供应商添加对话框
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_new"));
            dialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_add"));

            // 创建表单
            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            TextField codeField = new TextField();
            codeField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_code_hint"));

            TextField nameField = new TextField();
            nameField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("purchase_report.supplier_name"));

            TextField contactField = new TextField();
            contactField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("supplier.contact"));

            TextField phoneField = new TextField();
            phoneField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("supplier.phone"));

            TextField addressField = new TextField();
            addressField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("supplier.address"));

            ComboBox<String> rankCombo = new ComboBox<>();
            rankCombo.getItems().addAll("A", "B", "C");
            rankCombo.setValue("C");

            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_code_required")), 0, 0);
            grid.add(codeField, 1, 0);
            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_name_required")), 0, 1);
            grid.add(nameField, 1, 1);
            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.contact")), 0, 2);
            grid.add(contactField, 1, 2);
            grid.add(new Label(I18nManager.getInstance().get("runtime.phone")), 0, 3);
            grid.add(phoneField, 1, 3);
            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.address")), 0, 4);
            grid.add(addressField, 1, 4);
            grid.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.level")), 0, 5);
            grid.add(rankCombo, 1, 5);

            dialog.getDialogPane().setContent(grid);

            // 添加按钮
            dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK,
                ButtonType.CANCEL
            );

            // 验证输入
            Runnable validate = () -> {
                boolean valid = !codeField.getText().trim().isEmpty() 
                             && !nameField.getText().trim().isEmpty();
                dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
            };

            codeField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());
            nameField.textProperty().addListener((obs, oldVal, newVal) -> validate.run());

            // 显示对话框
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initOwner(parentStage);

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        Supplier supplier = new Supplier();
                        supplier.supplierCode = codeField.getText().trim();
                        supplier.name = nameField.getText().trim();
                        supplier.contactPerson = contactField.getText().trim();
                        supplier.phone = phoneField.getText().trim();
                        supplier.address = addressField.getText().trim();
                        supplier.rank = rankCombo.getValue();
                        supplier.status = true;

                        SupplierDAO.insert(supplier);
                        
                        // 刷新供应商列表
                        loadSuppliers();
                        supplierCombo.getItems().setAll(suppliers.values());
                        
                        // 选择新添加的供应商
                        Supplier newSupplier = suppliers.values().stream()
                            .filter(s -> s.supplierCode.equals(supplier.supplierCode))
                            .findFirst()
                            .orElse(null);
                        if (newSupplier != null) {
                            supplierCombo.setValue(newSupplier);
                        }
                        
                        updateStatus("供应商添加成功");
                        
                    } catch (SQLException e) {
                        logger.error("添加供应商失败", e);
                        showError(I18nManager.getInstance().get("runtime.supplier_add_failed", e.getMessage()));
                    }
                }
            });

        } catch (Exception e) {
            logger.error("显示供应商管理对话框失败", e);
            showError(I18nManager.getInstance().get("runtime.supplier_dialog_failed", e.getMessage()));
        }
    }

    /**
     * 创建标签
     */
    private Label createLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("text-default");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        return label;
    }

    private void applyCurrentTheme(Scene scene) {
        com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());
    }
}
