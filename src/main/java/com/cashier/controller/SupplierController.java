package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.dao.SupplierDAO;
import com.cashier.i18n.I18nManager;
import com.cashier.model.Supplier;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 供应商管理控制器
 * 处理供应商的增删改查
 */
public class SupplierController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(SupplierController.class);
    private static final int SUPPLIER_LIST_LIMIT = 500;

    @FXML
    private TableView<Supplier> supplierTable;

    @FXML
    private TableColumn<Supplier, String> codeColumn;

    @FXML
    private TableColumn<Supplier, String> nameColumn;

    @FXML
    private TableColumn<Supplier, String> contactColumn;

    @FXML
    private TableColumn<Supplier, String> phoneColumn;

    @FXML
    private TableColumn<Supplier, String> rankColumn;

    @FXML
    private TableColumn<Supplier, String> statusColumn;

    @FXML
    private TableColumn<Supplier, String> addressColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Label countLabel;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    private ObservableList<Supplier> supplierList;
    private Map<Integer, Supplier> suppliers;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 设置表格列
        setupTableColumns();

        // 加载供应商数据
        loadSuppliers();

        // 设置表格选择模式
        supplierTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // 添加表格选择监听
        supplierTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("supplierCode"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contactPerson"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        rankColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getRankDisplayName()));
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getStatusDisplayName()));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
    }

    /**
     * 加载供应商数据
     */
    private void loadSuppliers() {
        try {
            setSupplierData(SupplierDAO.findRecent(SUPPLIER_LIST_LIMIT));
        } catch (SQLException e) {
            logger.error("加载供应商数据失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            suppliers = new HashMap<>();
            supplierList = FXCollections.observableArrayList();
            supplierTable.setItems(supplierList);
        }
        updateCountLabel();
    }

    private void setSupplierData(List<Supplier> supplierData) {
        suppliers = new HashMap<>();
        for (Supplier supplier : supplierData) {
            suppliers.put(supplier.id, supplier);
        }
        supplierList = FXCollections.observableArrayList(supplierData);
        supplierTable.setItems(supplierList);
    }

    /**
     * 更新供应商数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(I18nManager.getInstance().get("runtime.supplier_count", supplierList.size()));
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = supplierTable.getSelectionModel().getSelectedItem() != null;
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
    }

    /**
     * 处理添加供应商
     */
    @FXML
    public void handleAddSupplier() {
        showSupplierDialog(null);
    }

    /**
     * 处理编辑供应商
     */
    @FXML
    public void handleEditSupplier() {
        Supplier selected = supplierTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showSupplierDialog(selected);
        }
    }

    /**
     * 显示供应商对话框
     */
    private void showSupplierDialog(Supplier supplier) {
        try {
            // 创建对话框内容
            GridPane gridPane = new GridPane();
            gridPane.setHgap(15);
            gridPane.setVgap(15);
            gridPane.setPadding(new javafx.geometry.Insets(25));

            // 设置列约束：标签列宽度120px，输入框列自动填充
            javafx.scene.layout.ColumnConstraints labelCol = new javafx.scene.layout.ColumnConstraints();
            labelCol.setPrefWidth(120);
            labelCol.setMinWidth(110);
            labelCol.setMaxWidth(130);

            javafx.scene.layout.ColumnConstraints fieldCol = new javafx.scene.layout.ColumnConstraints();
            fieldCol.setPrefWidth(300);
            fieldCol.setMinWidth(250);
            fieldCol.setHgrow(javafx.scene.layout.Priority.ALWAYS);

            gridPane.getColumnConstraints().addAll(labelCol, fieldCol);

            // 表单字段
            TextField codeField = new TextField();
            codeField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ProductEdit.AUTO_GENERATE));
            codeField.setEditable(false);
            codeField.setPrefWidth(300);

            TextField nameField = new TextField();
            nameField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_name_input_required"));
            nameField.setPrefWidth(300);

            TextField contactField = new TextField();
            contactField.setPromptText(I18nManager.getInstance().get("runtime.contact_hint"));
            contactField.setPrefWidth(300);

            TextField phoneField = new TextField();
            phoneField.setPromptText(I18nManager.getInstance().get("runtime.phone_hint"));
            phoneField.setPrefWidth(300);

            TextField addressField = new TextField();
            addressField.setPromptText(I18nManager.getInstance().get("runtime.address_hint"));
            addressField.setPrefWidth(300);

            ComboBox<String> rankCombo = new ComboBox<>();
            rankCombo.getItems().addAll("A", "B", "C");
            rankCombo.setValue("C");
            rankCombo.setPrefWidth(300);

            TextArea remarkArea = new TextArea();
            remarkArea.setPromptText(I18nManager.getInstance().get("runtime.notes_hint"));
            remarkArea.setPrefRowCount(3);
            remarkArea.setPrefWidth(300);

            // 如果是编辑模式，填充数据
            boolean isEdit = supplier != null;
            if (isEdit) {
                codeField.setText(supplier.supplierCode);
                nameField.setText(supplier.name);
                contactField.setText(supplier.contactPerson);
                phoneField.setText(supplier.phone);
                addressField.setText(supplier.address);
                rankCombo.setValue(supplier.rank);
                remarkArea.setText(supplier.remark);
            } else {
                // 自动生成供应商编号
                String newCode = generateSupplierCode();
                codeField.setText(newCode);
            }

            // 添加表单元素
            gridPane.add(new Label(I18nManager.getInstance().get("runtime.supplier_code")), 0, 0);
            gridPane.add(codeField, 1, 0);
            gridPane.add(new Label(I18nManager.getInstance().get("runtime.supplier_name_required_label")), 0, 1);
            gridPane.add(nameField, 1, 1);
            gridPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.contact")), 0, 2);
            gridPane.add(contactField, 1, 2);
            gridPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("settings.store_phone_label")), 0, 3);
            gridPane.add(phoneField, 1, 3);
            gridPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get("runtime.address")), 0, 4);
            gridPane.add(addressField, 1, 4);
            gridPane.add(new Label(I18nManager.getInstance().get("runtime.supplier_level")), 0, 5);
            gridPane.add(rankCombo, 1, 5);
            gridPane.add(new Label(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrderList.NOTES_LABEL)), 0, 6);
            gridPane.add(remarkArea, 1, 6);

            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.setTitle(isEdit ? com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_edit") : com.cashier.i18n.I18nManager.getInstance().get("supplier.add"));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(supplierTable.getScene().getWindow());
            dialogStage.setResizable(false);

            // 按钮
            Button saveButton = new Button(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Shortcut.SAVE));
            saveButton.setPrefWidth(80);
            saveButton.setDefaultButton(true);

            Button cancelButton = new Button(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrder.CANCEL));
            cancelButton.setPrefWidth(80);
            cancelButton.setCancelButton(true);

            saveButton.setOnAction(e -> {
                if (nameField.getText().trim().isEmpty()) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.supplier_name_input_required"));
                    return;
                }

                Supplier newSupplier = new Supplier();
                newSupplier.supplierCode = codeField.getText();
                newSupplier.name = nameField.getText().trim();
                newSupplier.contactPerson = contactField.getText().trim();
                newSupplier.phone = phoneField.getText().trim();
                newSupplier.address = addressField.getText().trim();
                newSupplier.rank = rankCombo.getValue();
                newSupplier.status = true;
                newSupplier.remark = remarkArea.getText().trim();

                if (isEdit) {
                    newSupplier.id = supplier.id;
                    try {
                        SupplierDAO.update(newSupplier);
                        loadSuppliers();
                        updateStatus("供应商更新成功: " + newSupplier.name);
                        dialogStage.close();
                    } catch (SQLException ex) {
                        logger.error("更新供应商失败", ex);
                        showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.SAVE_DATA) + ": " + ex.getMessage());
                    }
                } else {
                    try {
                        SupplierDAO.insert(newSupplier);
                        loadSuppliers();
                        updateStatus("供应商添加成功: " + newSupplier.name);
                        dialogStage.close();
                    } catch (SQLException ex) {
                        logger.error("添加供应商失败", ex);
                        showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.SAVE_DATA) + ": " + ex.getMessage());
                    }
                }
            });

            cancelButton.setOnAction(e -> dialogStage.close());

            HBox buttonBox = new HBox(15, saveButton, cancelButton);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            buttonBox.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));

            VBox root = new VBox(15, gridPane, buttonBox);
            root.setPadding(new javafx.geometry.Insets(20));
            root.setPrefWidth(500);

            Scene scene = new Scene(root);
            com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.error("显示供应商对话框失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    /**
     * 生成供应商编号
     */
    private String generateSupplierCode() {
        String dateStr = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
            .format(com.cashier.util.DateTimeFormats.COMPACT_DATE);
        String prefix = "S" + dateStr;

        int count = 0;
        try {
            count = SupplierDAO.countBySupplierCodePrefix(prefix);
        } catch (SQLException e) {
            logger.warn("统计供应商编号前缀失败: {}", prefix, e);
        }

        return prefix + String.format("%04d", count + 1);
    }

    /**
     * 处理删除供应商
     */
    @FXML
    public void handleDeleteSupplier() {
        Supplier selected = supplierTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.supplier_delete_confirm", selected.name));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    SupplierDAO.delete(selected.id);
                    suppliers.remove(selected.id);
                    supplierList.remove(selected);
                    updateCountLabel();
                    updateStatus("供应商删除成功: " + selected.name);
                } catch (SQLException e) {
                    logger.error("删除供应商失败", e);
                    showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.DELETE_DATA) + ": " + e.getMessage());
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
            loadSuppliers();
        } else {
            try {
                setSupplierData(SupplierDAO.search(searchText, SUPPLIER_LIST_LIMIT));
            } catch (SQLException e) {
                logger.error("搜索供应商失败", e);
                showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
            }
        }
        updateCountLabel();
    }

    /**
     * 处理清除搜索
     */
    @FXML
    public void handleClearSearch() {
        searchField.clear();
        loadSuppliers();
    }

    /**
     * 刷新供应商列表
     */
    public void refreshSuppliers() {
        loadSuppliers();
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
        com.cashier.util.StatusBarManager.updateError(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nManager.getInstance().get(I18nKeys.Label.ERROR));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
