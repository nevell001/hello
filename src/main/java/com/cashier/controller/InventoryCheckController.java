package com.cashier.controller;

import com.cashier.i18n.I18nManager;
import com.cashier.dao.DAOFactory;
import com.cashier.dao.InventoryCheckDAO;
import com.cashier.dao.InventoryCheckItemDAO;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.*;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import javafx.application.Platform;

/**
 * 库存盘点控制器
 * 处理库存盘点操作
 */
@SuppressWarnings("unchecked")
public class InventoryCheckController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(InventoryCheckController.class);
    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();

    @FXML
    private TableView<InventoryCheck> checkTable;

    @FXML
    private TableColumn<InventoryCheck, String> checkNoColumn;

    @FXML
    private TableColumn<InventoryCheck, String> checkDateColumn;

    @FXML
    private TableColumn<InventoryCheck, String> checkTypeColumn;

    @FXML
    private TableColumn<InventoryCheck, String> totalItemsColumn;

    @FXML
    private TableColumn<InventoryCheck, String> diffItemsColumn;

    @FXML
    private TableColumn<InventoryCheck, String> statusColumn;

    @FXML
    private TableColumn<InventoryCheck, String> operatorColumn;

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

    // 差异统计标签（在表单中使用）
    private Label diffLabel;

    @FXML
    private Button viewDetailButton;

    @FXML
    private Button completeButton;

    private ObservableList<InventoryCheck> checkList;
    private Map<Integer, InventoryCheck> checks;

    // 当前用户
    private String currentUser = "admin";

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置表格列
        setupTableColumns();

        // 设置状态筛选
        statusFilterCombo.getItems().addAll("all", "pending", "checking", "completed");
        com.cashier.util.I18nUiUtils.configureComboBox(statusFilterCombo, value ->
            "all".equals(value) ? I18nManager.getInstance().get("filter.all")
                : com.cashier.util.I18nUiUtils.inventoryCheckStatus(value));
        statusFilterCombo.setValue("all");

        // 加载盘点记录
        loadChecks();

        // 设置表格选择模式
        checkTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 添加表格选择监听
        checkTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        checkNoColumn.setCellValueFactory(new PropertyValueFactory<>("checkNo"));
        checkDateColumn.setCellValueFactory(new PropertyValueFactory<>("checkDate"));
        checkTypeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getCheckTypeDisplayName()));
        totalItemsColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().totalItems)));
        diffItemsColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().diffItems)));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
            com.cashier.util.I18nUiUtils.inventoryCheckStatus(cellData.getValue().status)));
        operatorColumn.setCellValueFactory(new PropertyValueFactory<>("operator"));
    }

    /**
     * 加载盘点记录
     */
    private void loadChecks() {
        try {
            List<InventoryCheck> checkData = InventoryCheckDAO.findAll();
            checks = new HashMap<>();
            for (InventoryCheck check : checkData) {
                checks.put(check.id, check);
            }
        } catch (SQLException e) {
            logger.error("加载盘点记录失败", e);
            showError(I18nManager.getInstance().get("runtime.inventory_check_load_failed", e.getMessage()));
            checks = new HashMap<>();
        }
        filterChecks();
    }

    /**
     * 筛选盘点记录
     */
    private void filterChecks() {
        String statusFilter = statusFilterCombo.getValue();
        List<InventoryCheck> filtered = checks.values().stream()
            .filter(check -> {
                if ("all".equals(statusFilter)) return true;
                switch (statusFilter) {
                    case "pending": return "pending".equals(check.status);
                    case "checking": return "checking".equals(check.status);
                    case "completed": return "completed".equals(check.status);
                    default: return true;
                }
            })
            .collect(Collectors.toList());

        checkList = FXCollections.observableArrayList(filtered);
        checkTable.setItems(checkList);
        updateCountLabel();
    }

    /**
     * 更新盘点数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(I18nManager.getInstance().get("runtime.inventory_check_count", checkList.size()));
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        InventoryCheck selected = checkTable.getSelectionModel().getSelectedItem();
        boolean hasSelection = selected != null;
        boolean canEdit = hasSelection && selected.canEdit();
        boolean canComplete = hasSelection && selected.canComplete();

        editButton.setDisable(!canEdit);
        deleteButton.setDisable(!canEdit);
        viewDetailButton.setDisable(!hasSelection);
        completeButton.setDisable(!canComplete);
    }

    /**
     * 处理添加盘点单
     */
    @FXML
    public void handleAddCheck() {
        showCheckDialog(null);
    }

    /**
     * 处理编辑盘点单
     */
    @FXML
    public void handleEditCheck() {
        InventoryCheck selected = checkTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showCheckDialog(selected);
        }
    }

    /**
     * 显示盘点对话框
     */
    private void showCheckDialog(InventoryCheck check) {
        try {
            Stage dialogStage = new Stage();
            dialogStage.setTitle(check == null ? com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_new") : com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_edit"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(checkTable.getScene().getWindow());

            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(20));
            root.getStyleClass().addAll("dialog-content", "edit-view-container");

            // 表单字段
            GridPane gridPane = new GridPane();
            gridPane.setHgap(10);
            gridPane.setVgap(10);
            gridPane.getStyleClass().add("form-grid");

            TextField checkNoField = new TextField();
            checkNoField.setEditable(false);
            checkNoField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("product.edit.auto_generate"));
            checkNoField.getStyleClass().add("form-input");

            DatePicker checkDatePicker = new DatePicker();
            checkDatePicker.setValue(java.time.LocalDate.now());
            checkDatePicker.getStyleClass().add("form-date-picker");

            ComboBox<String> checkTypeCombo = new ComboBox<>();
            checkTypeCombo.getItems().addAll("full", "partial");
            com.cashier.util.I18nUiUtils.configureComboBox(
                checkTypeCombo, com.cashier.util.I18nUiUtils::inventoryCheckType);
            checkTypeCombo.setValue("full");
            checkTypeCombo.getStyleClass().addAll("form-combo", "inventory-check-type-combo");
            checkTypeCombo.setMinWidth(280);
            checkTypeCombo.setPrefWidth(320);
            checkTypeCombo.setMaxWidth(Double.MAX_VALUE);
            checkTypeCombo.setMinHeight(38);
            checkTypeCombo.setPrefHeight(38);
            GridPane.setHgrow(checkTypeCombo, javafx.scene.layout.Priority.ALWAYS);

            TextArea remarkArea = new TextArea();
            remarkArea.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("restock.reason"));
            remarkArea.setPrefRowCount(2);
            remarkArea.getStyleClass().add("form-text-area");

            // 商品列表表格
            TableView<CheckItemWrapper> itemTable = new TableView<>();
            itemTable.setEditable(true);
            itemTable.setPlaceholder(new Label(I18nManager.getInstance().get("message.data.empty")));

            TableColumn<CheckItemWrapper, String> productNameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("return_approval.product_name"));
            productNameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProductName()));

            TableColumn<CheckItemWrapper, Integer> bookQtyCol = new TableColumn<>(I18nManager.getInstance().get("runtime.book_quantity"));
            bookQtyCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().bookQuantity).asObject());

            TableColumn<CheckItemWrapper, Integer> actualQtyCol = new TableColumn<>(I18nManager.getInstance().get("runtime.actual_quantity"));
            actualQtyCol.setPrefWidth(100);
            actualQtyCol.setCellValueFactory(cellData -> cellData.getValue().actualQuantityProperty().asObject());
            actualQtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            actualQtyCol.setOnEditCommit(e -> {
                int bookQty = e.getRowValue().bookQuantity;
                int actualQty = e.getNewValue();
                e.getRowValue().actualQuantity.set(actualQty);
                e.getRowValue().diffQuantity.set(actualQty - bookQty);
                itemTable.refresh();
            });

            TableColumn<CheckItemWrapper, Integer> diffQtyCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("shift.difference"));
            diffQtyCol.setCellValueFactory(cellData -> cellData.getValue().diffQuantityProperty().asObject());

            TableColumn<CheckItemWrapper, String> diffReasonCol = new TableColumn<>(I18nManager.getInstance().get("runtime.difference_reason"));
            diffReasonCol.setPrefWidth(150);
            diffReasonCol.setCellValueFactory(new PropertyValueFactory<>("diffReason"));
            diffReasonCol.setCellFactory(TextFieldTableCell.forTableColumn());
            diffReasonCol.setOnEditCommit(e -> {
                e.getRowValue().diffReason.set(e.getNewValue());
            });

            TableColumn<CheckItemWrapper, String> actionCol = new TableColumn<>(I18nManager.getInstance().get("runtime.action"));
            actionCol.setPrefWidth(80);
            actionCol.setCellFactory(col -> new TableCell<CheckItemWrapper, String>() {
                private final Button deleteBtn = new Button(com.cashier.i18n.I18nManager.getInstance().get("inventory_check.delete"));
                {
                    deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                    deleteBtn.setOnAction(e -> {
                        CheckItemWrapper item = getTableView().getItems().get(getIndex());
                        itemTable.getItems().remove(item);
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

            itemTable.getColumns().addAll(productNameCol, bookQtyCol, actualQtyCol, diffQtyCol, diffReasonCol, actionCol);

            ObservableList<CheckItemWrapper> items = FXCollections.observableArrayList();
            itemTable.setItems(items);

            // 监听列表变化，更新差异统计
            items.addListener((javafx.collections.ListChangeListener<CheckItemWrapper>) change -> {
                int diffCount = (int) items.stream().filter(item -> item.diffQuantity.get() != 0).count();
                diffLabel.setText(I18nManager.getInstance().get("runtime.inventory_check_diff_count", diffCount));
            });

            // 添加商品按钮
            Button addProductButton = new Button(I18nManager.getInstance().get("runtime.add_product"));
            addProductButton.getStyleClass().addAll("primary-button", "button-normal");
            addProductButton.setOnAction(e -> showProductSelector(itemTable));

            // 差异统计
            diffLabel = new Label(I18nManager.getInstance().get("runtime.inventory_check_diff_count", 0));

            // 如果是编辑模式，填充数据
            boolean isEdit = check != null;
            if (isEdit) {
                checkNoField.setText(check.checkNo);
                checkDatePicker.setValue(java.time.LocalDate.parse(check.checkDate));
                checkTypeCombo.setValue(check.checkType);
                remarkArea.setText(check.remark);

                try {
                    List<InventoryCheckItem> checkItems = InventoryCheckItemDAO.findByCheckId(check.id);
                    for (InventoryCheckItem item : checkItems) {
                        items.add(new CheckItemWrapper(item));
                    }
                } catch (SQLException ex) {
                    logger.error("加载盘点明细失败", ex);
                }
            } else {
                checkNoField.setText(generateCheckNo());
            }

            // 添加表单元素
            gridPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_no")), 0, 0);
            gridPane.add(checkNoField, 1, 0);
            gridPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_date")), 0, 1);
            gridPane.add(checkDatePicker, 1, 1);
            gridPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_type")), 0, 2);
            gridPane.add(checkTypeCombo, 1, 2);
            gridPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("return_order_list.notes_label")), 0, 3);
            gridPane.add(remarkArea, 1, 3);

            // 按钮
            Button saveButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("shortcut.save"));
            Button cancelButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("return_order.cancel"));
            saveButton.getStyleClass().addAll("primary-button", "button-normal");
            cancelButton.getStyleClass().addAll("secondary-button", "button-normal");
            saveButton.setMinWidth(110);
            cancelButton.setMinWidth(110);

            saveButton.setOnAction(e -> {
                saveButton.setDisable(true);
                InventoryCheck newCheck = new InventoryCheck();
                newCheck.checkNo = checkNoField.getText();
                newCheck.checkDate = checkDatePicker.getValue().toString();
                newCheck.checkType = checkTypeCombo.getValue();
                newCheck.totalItems = items.size();
                newCheck.diffItems = (int) items.stream().filter(item -> item.diffQuantity.get() != 0).count();
                newCheck.status = "checking";
                newCheck.operator = currentUser;
                newCheck.remark = remarkArea.getText().trim();

                try {
                    if (isEdit) {
                        newCheck.id = check.id;
                        InventoryCheckDAO.update(newCheck);
                        // 删除旧明细并插入新明细
                        InventoryCheckItemDAO.deleteByCheckId(check.id);
                        for (CheckItemWrapper wrapper : items) {
                            InventoryCheckItem item = new InventoryCheckItem();
                            item.checkId = check.id;
                            item.productId = wrapper.productId;
                            item.productName = wrapper.productName;
                            item.bookQuantity = wrapper.bookQuantity;
                            item.actualQuantity = wrapper.actualQuantity.get();
                            item.diffQuantity = wrapper.diffQuantity.get();
                            item.diffReason = wrapper.diffReason.get();
                            InventoryCheckItemDAO.insert(item);
                        }
                        updateStatus(I18nManager.getInstance().get("runtime.inventory_check_updated"));
                    } else {
                        newCheck.checkNo = InventoryCheckDAO.generateNextCheckNo(newCheck.checkDate);
                        checkNoField.setText(newCheck.checkNo);
                        InventoryCheckDAO.insert(newCheck);
                        for (CheckItemWrapper wrapper : items) {
                            InventoryCheckItem item = new InventoryCheckItem();
                            item.checkId = newCheck.id;
                            item.productId = wrapper.productId;
                            item.productName = wrapper.productName;
                            item.bookQuantity = wrapper.bookQuantity;
                            item.actualQuantity = wrapper.actualQuantity.get();
                            item.diffQuantity = wrapper.diffQuantity.get();
                            item.diffReason = wrapper.diffReason.get();
                            InventoryCheckItemDAO.insert(item);
                        }
                        updateStatus(I18nManager.getInstance().get("runtime.inventory_check_created"));
                    }
                    loadChecks();
                    dialogStage.close();

                } catch (SQLException ex) {
                    saveButton.setDisable(false);
                    logger.error("保存盘点单失败", ex);
                    showError(I18nManager.getInstance().get("runtime.inventory_check_save_failed", ex.getMessage()));
                } catch (Exception ex) {
                    saveButton.setDisable(false);
                    logger.error("保存盘点单失败", ex);
                    showError(I18nManager.getInstance().get("runtime.inventory_check_save_failed", ex.getMessage()));
                }
            });

            cancelButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(10, saveButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(
                gridPane,
                new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.product_details")),
                addProductButton,
                itemTable,
                diffLabel,
                buttonBox
            );

            Scene scene = new Scene(root, 700, 600);
            com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.error("显示盘点对话框失败", e);
            showError(I18nManager.getInstance().get("runtime.dialog_load_failed", e.getMessage()));
        }
    }

    /**
     * 显示商品选择器
     */
    private void showProductSelector(TableView<CheckItemWrapper> itemTable) {
        try {
            Stage selectorStage = new Stage();
            selectorStage.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.select_product"));
            selectorStage.initModality(Modality.WINDOW_MODAL);
            selectorStage.initOwner(checkTable.getScene().getWindow());

            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(10));
            root.getStyleClass().add("dialog-content");

            // 分类筛选
            ComboBox<String> categoryCombo = new ComboBox<>();
            categoryCombo.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.all_categories"));
            categoryCombo.setPrefWidth(150);
            
            // 搜索框
            TextField searchField = new TextField();
            searchField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.search_product_hint"));
            HBox filterBox = new HBox(10, new Label(com.cashier.i18n.I18nManager.getInstance().get("product.edit.category")), categoryCombo, new Label(com.cashier.i18n.I18nManager.getInstance().get("purchase_inbound.search_label")), searchField);

            // 商品表格
            TableView<Product> productTable = new TableView<>();
            productTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            productTable.setPlaceholder(new Label(I18nManager.getInstance().get("message.data.empty")));
            
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
                        Product product = getTableView().getItems().get(getIndex());
                        checkBox.setSelected(productTable.getSelectionModel().getSelectedItems().contains(product));
                        setGraphic(checkBox);
                    }
                }
            });
            
            // 监听选择状态变化，刷新表格以更新复选框显示
            productTable.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<Product>) c -> {
                Platform.runLater(() -> productTable.refresh());
            });
            
            TableColumn<Product, String> nameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("return_approval.product_name"));
            nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

            TableColumn<Product, String> barcodeCol = new TableColumn<>(I18nManager.getInstance().get("runtime.barcode"));
            barcodeCol.setCellValueFactory(new PropertyValueFactory<>("barcode"));

            TableColumn<Product, String> categoryCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("return_report.category"));
            categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

            TableColumn<Product, Number> stockCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("product.stock"));
            stockCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

            TableColumn<Product, Number> costCol = new TableColumn<>(I18nManager.getInstance().get("runtime.cost_price"));
            costCol.setCellValueFactory(new PropertyValueFactory<>("cost"));

            productTable.getColumns().addAll(selectColumn, nameCol, barcodeCol, categoryCol, stockCol, costCol);
            
            // 设置行工厂，使选中行背景色更明显
            productTable.setRowFactory(tv -> {
                TableRow<Product> row = new TableRow<>();
                ChangeListener<Boolean> changeListener = (obs, wasSelected, isNowSelected) -> {
                    if (isNowSelected) {
                        row.setStyle("-fx-background-color: linear-gradient(to right, rgba(63, 81, 181, 0.25), rgba(63, 81, 181, 0.35));");
                    } else {
                        row.setStyle("");
                    }
                };
                row.selectedProperty().addListener(changeListener);
                return row;
            });

            // 加载商品数据
            List<Product> products = productDAO.findAll();
            ObservableList<Product> productList = FXCollections.observableArrayList(products);
            productTable.setItems(productList);

            // 加载分类列表
            Set<String> categories = products.stream()
                .map(p -> p.category)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toSet());
            ObservableList<String> categoryList = FXCollections.observableArrayList();
            String allCategories = I18nManager.getInstance().get("filter.all_categories");
            categoryList.add(allCategories);
            categoryList.addAll(categories);
            categoryCombo.setItems(categoryList);
            categoryCombo.setValue(allCategories);

            // 分类筛选和全选功能
            categoryCombo.setOnAction(e -> {
                String selectedCategory = categoryCombo.getValue();
                if (allCategories.equals(selectedCategory)) {
                    productTable.setItems(productList);
                } else {
                    List<Product> filtered = productList.stream()
                        .filter(p -> selectedCategory.equals(p.category))
                        .collect(Collectors.toList());
                    productTable.setItems(FXCollections.observableArrayList(filtered));
                }
                productTable.getSelectionModel().clearSelection();
            });

            // 搜索功能
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                String searchText = newVal.toLowerCase();
                String selectedCategory = categoryCombo.getValue();
                List<Product> filtered = productList.stream()
                    .filter(p -> {
                        boolean matchCategory = allCategories.equals(selectedCategory) || selectedCategory.equals(p.category);
                        boolean matchSearch = searchText.isEmpty() || p.name.toLowerCase().contains(searchText);
                        return matchCategory && matchSearch;
                    })
                    .collect(Collectors.toList());
                productTable.setItems(FXCollections.observableArrayList(filtered));
                productTable.getSelectionModel().clearSelection();
            });

            // 全选/取消全选按钮
            Button selectAllButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("shortcut.select_all"));
            selectAllButton.setOnAction(e -> {
                ObservableList<Product> items = productTable.getItems();
                productTable.getSelectionModel().selectAll();
            });

            Button deselectAllButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("runtime.deselect_all"));
            deselectAllButton.setOnAction(e -> {
                productTable.getSelectionModel().clearSelection();
            });

            HBox selectButtonsBox = new HBox(10, selectAllButton, deselectAllButton);

            // 添加按钮
            Button addButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("runtime.add_selected_products"));
            addButton.getStyleClass().addAll("primary-button", "button-normal");
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
                        logger.debug("选中的商品 - ID: {}, 名称: {}", selected.id, selected.name);
                        CheckItemWrapper wrapper = new CheckItemWrapper(selected);
                        itemTable.getItems().add(wrapper);
                        addedCount++;
                    }
                }
                
                if (addedCount > 0) {
                    itemTable.refresh();
                    logger.info("成功添加 {} 个商品", addedCount);
                    selectorStage.close();
                } else {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.product_already_added"));
                }
            });

            Button cancelButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("return_order.cancel"));
            cancelButton.getStyleClass().addAll("secondary-button", "button-normal");
            cancelButton.setOnAction(e -> selectorStage.close());

            HBox buttonBox = new HBox(10, addButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            root.getChildren().addAll(filterBox, selectButtonsBox, productTable, buttonBox);

            Scene scene = new Scene(root, 600, 450);
            com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

            selectorStage.setScene(scene);
            selectorStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载商品数据失败", e);
            showError(I18nManager.getInstance().get("runtime.product_load_failed", e.getMessage()));
        }
    }

    /**
     * 盘点项包装类
     */
    private static class CheckItemWrapper {
        int productId;
        String productName;
        int bookQuantity;
        javafx.beans.property.IntegerProperty actualQuantity = new javafx.beans.property.SimpleIntegerProperty(0);
        javafx.beans.property.IntegerProperty diffQuantity = new javafx.beans.property.SimpleIntegerProperty(0);
        javafx.beans.property.StringProperty diffReason = new javafx.beans.property.SimpleStringProperty("");

        public CheckItemWrapper(Product product) {
            this.productId = product.id;
            this.productName = product.name;
            this.bookQuantity = product.quantity;
            this.actualQuantity.set(product.quantity);
            this.diffQuantity.set(0);
        }

        public CheckItemWrapper(InventoryCheckItem item) {
            this.productId = item.productId;
            this.productName = item.productName;
            this.bookQuantity = item.bookQuantity;
            this.actualQuantity.set(item.actualQuantity);
            this.diffQuantity.set(item.diffQuantity);
            this.diffReason.set(item.diffReason != null ? item.diffReason : "");
        }

        public String getProductName() { return productName; }
        public int getBookQuantity() { return bookQuantity; }
        public int getActualQuantity() { return actualQuantity.get(); }
        public javafx.beans.property.IntegerProperty actualQuantityProperty() { return actualQuantity; }
        public int getDiffQuantity() { return diffQuantity.get(); }
        public javafx.beans.property.IntegerProperty diffQuantityProperty() { return diffQuantity; }
        public String getDiffReason() { return diffReason.get(); }
        public javafx.beans.property.StringProperty diffReasonProperty() { return diffReason; }
    }

    /**
     * 生成盘点单号
     */
    private String generateCheckNo() {
        try {
            return InventoryCheckDAO.generateNextCheckNo(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        } catch (SQLException ex) {
            logger.warn("生成盘点单号失败，使用本地时间兜底", ex);
            return "IC" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date()) + "0001";
        }
    }

    /**
     * 处理删除盘点单
     */
    @FXML
    public void handleDeleteCheck() {
        InventoryCheck selected = checkTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.confirm_delete"));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.inventory_check_delete_confirm", selected.checkNo));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    InventoryCheckItemDAO.deleteByCheckId(selected.id);
                    InventoryCheckDAO.delete(selected.id);
                    checks.remove(selected.id);
                    filterChecks();
                    updateStatus(I18nManager.getInstance().get("runtime.inventory_check_deleted"));
                } catch (SQLException e) {
                    logger.error("删除盘点单失败", e);
                    showError(I18nManager.getInstance().get("runtime.inventory_check_delete_failed", e.getMessage()));
                }
            }
        }
    }

    /**
     * 处理查看详情
     */
    @FXML
    public void handleViewDetail() {
        InventoryCheck selected = checkTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showCheckDetailDialog(selected);
        }
    }

    /**
     * 显示盘点详情对话框
     */
    private void showCheckDetailDialog(InventoryCheck check) {
        try {
            VBox root = new VBox(10);
            root.setPadding(new javafx.geometry.Insets(20));

            // 盘点信息
            GridPane infoPane = new GridPane();
            infoPane.setHgap(10);
            infoPane.setVgap(10);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_no")), 0, 0);
            infoPane.add(new Label(check.checkNo), 1, 0);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_date")), 0, 1);
            infoPane.add(new Label(check.checkDate), 1, 1);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_type")), 0, 2);
            infoPane.add(new Label(check.getCheckTypeDisplayName()), 1, 2);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_operator")), 0, 3);
            infoPane.add(new Label(check.operator), 1, 3);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("return_order_list.status_label")), 0, 4);
            infoPane.add(new Label(check.getStatusDisplayName()), 1, 4);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("cart.total_quantity")), 0, 5);
            infoPane.add(new Label(String.valueOf(check.totalItems)), 1, 5);
            infoPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_diff")), 0, 6);
            infoPane.add(new Label(String.valueOf(check.diffItems)), 1, 6);

            // 商品明细
            TableView<InventoryCheckItem> itemTable = new TableView<>();
            TableColumn<InventoryCheckItem, String> nameCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("return_approval.product_name"));
            nameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));

            TableColumn<InventoryCheckItem, Number> bookCol = new TableColumn<>(I18nManager.getInstance().get("runtime.book_quantity"));
            bookCol.setCellValueFactory(new PropertyValueFactory<>("bookQuantity"));

            TableColumn<InventoryCheckItem, Number> actualCol = new TableColumn<>(I18nManager.getInstance().get("runtime.actual_quantity"));
            actualCol.setCellValueFactory(new PropertyValueFactory<>("actualQuantity"));

            TableColumn<InventoryCheckItem, Number> diffCol = new TableColumn<>(com.cashier.i18n.I18nManager.getInstance().get("shift.difference"));
            diffCol.setCellValueFactory(new PropertyValueFactory<>("diffQuantity"));

            TableColumn<InventoryCheckItem, String> reasonCol = new TableColumn<>(I18nManager.getInstance().get("runtime.difference_reason"));
            reasonCol.setCellValueFactory(new PropertyValueFactory<>("diffReason"));

            itemTable.getColumns().addAll(nameCol, bookCol, actualCol, diffCol, reasonCol);

            List<InventoryCheckItem> items = InventoryCheckItemDAO.findByCheckId(check.id);
            itemTable.setItems(FXCollections.observableArrayList(items));

            Button closeButton = new Button(com.cashier.i18n.I18nManager.getInstance().get("inventory_alert.close"));

            root.getChildren().addAll(
                new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_info")),
                infoPane,
                new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.product_details")),
                itemTable,
                closeButton
            );

            Scene scene = new Scene(root, 600, 500);
            com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

            Stage dialogStage = new Stage();
            dialogStage.setTitle(I18nManager.getInstance().get("runtime.inventory_check_detail_title", check.checkNo));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(checkTable.getScene().getWindow());
            dialogStage.setScene(scene);

            // 设置关闭按钮操作（必须在 dialogStage 声明之后）
            closeButton.setOnAction(e -> dialogStage.close());

            dialogStage.showAndWait();

        } catch (SQLException e) {
            logger.error("加载盘点详情失败", e);
            showError(I18nManager.getInstance().get("runtime.inventory_check_detail_failed", e.getMessage()));
        }
    }

    /**
     * 处理完成盘点
     */
    @FXML
    public void handleComplete() {
        InventoryCheck selected = checkTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.inventory_check_complete_confirm"));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.inventory_check_complete_message", selected.checkNo));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    // 更新盘点单状态
                    InventoryCheckDAO.complete(selected.id, currentUser);

                    // 根据盘点结果调整库存
                    List<InventoryCheckItem> items = InventoryCheckItemDAO.findByCheckId(selected.id);
                    for (InventoryCheckItem item : items) {
                        if (item.diffQuantity != 0) {
                            productDAO.updateQuantity(item.productId, item.diffQuantity);
                        }
                    }

                    updateStatus(I18nManager.getInstance().get("runtime.inventory_check_completed", selected.checkNo));
                    loadChecks();

                } catch (SQLException e) {
                    logger.error("完成盘点失败", e);
                    showError(I18nManager.getInstance().get("runtime.inventory_check_complete_failed", e.getMessage()));
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
            filterChecks();
        } else {
            String statusFilter = statusFilterCombo.getValue();
            List<InventoryCheck> filtered = checks.values().stream()
                .filter(check -> {
                    if ("all".equals(statusFilter)) return true;
                    switch (statusFilter) {
                        case "pending": return "pending".equals(check.status);
                        case "checking": return "checking".equals(check.status);
                        case "completed": return "completed".equals(check.status);
                        default: return true;
                    }
                })
                .filter(check -> check.checkNo.toLowerCase().contains(searchText) ||
                         check.operator.toLowerCase().contains(searchText))
                .collect(Collectors.toList());

            checkList = FXCollections.observableArrayList(filtered);
            checkTable.setItems(checkList);
            updateCountLabel();
        }
    }

    /**
     * 处理状态筛选
     */
    @FXML
    public void handleStatusFilter() {
        filterChecks();
    }

    /**
     * 刷新盘点列表
     */
    public void refreshChecks() {
        loadChecks();
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
}
