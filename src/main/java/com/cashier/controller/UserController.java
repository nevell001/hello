package com.cashier.controller;

import com.cashier.dao.UserDAO;
import com.cashier.i18n.I18nManager;
import com.cashier.model.User;
import com.cashier.util.PasswordUtil;
import com.cashier.util.StatusBarManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import com.cashier.util.FormValidator;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * 用户管理控制器
 * 处理用户的增删改查
 */
public class UserController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(UserController.class);

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> createTimeColumn;

    @FXML
    private TableColumn<User, String> lastLoginColumn;

    @FXML
    private TableColumn<User, String> statusColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> roleFilterComboBox;

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
    private Button resetPasswordButton;

    @FXML
    private Button activateButton;

    @FXML
    private Button deactivateButton;

    private ObservableList<User> userList;
    private Map<String, User> users;
    private User currentUser;

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 初始化角色筛选下拉框
        roleFilterComboBox.setItems(FXCollections.observableArrayList(
            "all",
            "admin",
            "cashier",
            "finance"
        ));
        roleFilterComboBox.setButtonCell(createUserFilterCell(true));
        roleFilterComboBox.setCellFactory(listView -> createUserFilterCell(true));
        roleFilterComboBox.getSelectionModel().select(0);

        // 初始化状态筛选下拉框
        statusFilterComboBox.setItems(FXCollections.observableArrayList(
            "all",
            "active",
            "inactive"
        ));
        statusFilterComboBox.setButtonCell(createUserFilterCell(false));
        statusFilterComboBox.setCellFactory(listView -> createUserFilterCell(false));
        statusFilterComboBox.getSelectionModel().select(0);

        // 设置表格列
        setupTableColumns();

        // 加载用户数据
        loadUsers();

        // 设置表格选择模式
        userTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 添加表格选择监听
        userTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> updateButtonStates()
        );
    }

    /**
     * 设置表格列
     */
    private void setupTableColumns() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        roleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(localizeRole(cellData.getValue().role)));
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        createTimeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(sdf.format(cellData.getValue().createTime)));
        lastLoginColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(sdf.format(cellData.getValue().lastLoginTime)));
        statusColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(I18nManager.getInstance().get(
                cellData.getValue().active ? "user.enabled" : "user.disabled")));
    }

    /**
     * 加载用户数据
     */
    private void loadUsers() {
        logger.info("UserController: 开始加载用户数据...");
        try {
            List<User> userListData = UserDAO.findAll();
            users = new java.util.HashMap<>();
            for (User user : userListData) {
                users.put(user.username, user);
            }
        } catch (SQLException e) {
            logger.error("加载用户数据失败", e);
            showError(I18nManager.getInstance().get("runtime.user_load_failed", e.getMessage()));
            users = new java.util.HashMap<>();
        }
        userList = FXCollections.observableArrayList(users.values());
        userTable.setItems(userList);
        updateCountLabel();
        logger.info("UserController: 加载了 {} 个用户", users.size());
    }

    /**
     * 更新用户数量标签
     */
    private void updateCountLabel() {
        countLabel.setText(I18nManager.getInstance().get("runtime.user_count", userList.size()));
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonStates() {
        boolean hasSelection = !userTable.getSelectionModel().getSelectedItems().isEmpty();
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        resetPasswordButton.setDisable(!hasSelection);
        activateButton.setDisable(!hasSelection);
        deactivateButton.setDisable(!hasSelection);
    }

    /**
     * 处理添加用户
     */
    @FXML
    public void handleAddUser() {
        showUserDialog(null);
    }

    /**
     * 处理编辑用户
     */
    @FXML
    public void handleEditUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showUserDialog(selected);
        }
    }

    /**
     * 显示用户对话框
     * @param user 要编辑的用户，null表示添加新用户
     */
    private void showUserDialog(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(user == null ? com.cashier.i18n.I18nManager.getInstance().get("user.add") : com.cashier.i18n.I18nManager.getInstance().get("runtime.user_edit"));
        dialog.setHeaderText(null);
        dialog.getDialogPane().setPrefWidth(620);
        dialog.getDialogPane().getStyleClass().add("user-dialog");
        if (userTable.getScene() != null) {
            dialog.initOwner(userTable.getScene().getWindow());
            dialog.getDialogPane().getStylesheets().addAll(userTable.getScene().getStylesheets());
        }

        VBox content = new VBox(14);
        content.getStyleClass().add("dialog-content");
        Label titleLabel = new Label(user == null
            ? I18nManager.getInstance().get("user.add")
            : I18nManager.getInstance().get("runtime.user_edit"));
        titleLabel.getStyleClass().add("view-title");

        // 创建对话框内容
        GridPane grid = new GridPane();
        grid.getStyleClass().addAll("form-grid", "dialog-form-grid");
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));
        javafx.scene.layout.ColumnConstraints labelColumn = new javafx.scene.layout.ColumnConstraints();
        labelColumn.setMinWidth(120);
        labelColumn.setPrefWidth(135);
        javafx.scene.layout.ColumnConstraints fieldColumn = new javafx.scene.layout.ColumnConstraints();
        fieldColumn.setMinWidth(380);
        fieldColumn.setPrefWidth(420);
        fieldColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, fieldColumn);

        TextField idField = new TextField();
        CheckBox autoIdCheckBox = new CheckBox(I18nManager.getInstance().get("product.edit.auto_generate"));
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField nameField = new TextField();
        ComboBox<String> roleComboBox = new ComboBox<>();

        for (TextField field : new TextField[]{idField, usernameField, passwordField, nameField}) {
            field.getStyleClass().add("form-input");
            field.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(field, Priority.ALWAYS);
        }
        roleComboBox.getStyleClass().addAll("form-combo", "user-role-combo");
        roleComboBox.setMaxWidth(Double.MAX_VALUE);
        roleComboBox.setMinWidth(380);
        roleComboBox.setPrefWidth(420);
        GridPane.setHgrow(roleComboBox, Priority.ALWAYS);

        roleComboBox.setItems(FXCollections.observableArrayList("admin", "cashier", "finance"));
        roleComboBox.setButtonCell(createRoleCell());
        roleComboBox.setCellFactory(listView -> createRoleCell());

        // 设置ID输入框和复选框的默认状态
        if (user == null) {
            autoIdCheckBox.setSelected(true);
            idField.setDisable(true);
        } else {
            idField.setText(String.valueOf(user.id));
            idField.setDisable(true);
            autoIdCheckBox.setDisable(true);
        }

        // 添加复选框监听器
        autoIdCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            idField.setDisable(newVal);
        });

        if (user != null) {
            usernameField.setText(user.username);
            usernameField.setDisable(true); // 用户名不可修改
            passwordField.setPromptText(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_unchanged_hint"));
            nameField.setText(user.name);
            roleComboBox.getSelectionModel().select(user.role);
        } else {
            roleComboBox.getSelectionModel().select("cashier");
        }

        // 创建ID的HBox
        HBox idBox = new HBox(10);
        idBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        idBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(idBox, Priority.ALWAYS);
        idBox.getChildren().addAll(idField, autoIdCheckBox);
        HBox.setHgrow(idField, Priority.ALWAYS);

        grid.add(createUserFormLabel("ID:"), 0, 0);
        grid.add(idBox, 1, 0);
        grid.add(createUserFormLabel(com.cashier.i18n.I18nManager.getInstance().get("runtime.username")), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(createUserFormLabel(com.cashier.i18n.I18nManager.getInstance().get("runtime.password")), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(createUserFormLabel(com.cashier.i18n.I18nManager.getInstance().get("runtime.name")), 0, 3);
        grid.add(nameField, 1, 3);
        grid.add(createUserFormLabel(com.cashier.i18n.I18nManager.getInstance().get("runtime.role")), 0, 4);
        grid.add(roleComboBox, 1, 4);

        content.getChildren().addAll(titleLabel, grid);
        dialog.getDialogPane().setContent(content);

        ButtonType okButtonType = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get("common.ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(I18nManager.getInstance().get("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
        dialog.getDialogPane().lookupButton(okButtonType).getStyleClass().addAll("primary-button", "button-normal");
        dialog.getDialogPane().lookupButton(cancelButtonType).getStyleClass().addAll("secondary-button", "button-normal");

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                User newUser = user != null ? user : new User();

                // 验证ID（仅当手动输入时）
                if (user == null && !autoIdCheckBox.isSelected() && !idField.getText().trim().isEmpty()) {
                    try {
                        int id = FormValidator.parseInt(idField.getText().trim());
                        if (id <= 0) {
                            showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.id_positive"));
                            return null;
                        }
                        newUser.id = id;
                    } catch (NumberFormatException e) {
                        showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.id_invalid"));
                        return null;
                    }
                }

                if (user == null) {
                    newUser.username = usernameField.getText().trim();
                }

                // 只有在新用户或输入了新密码时才哈希密码
                String passwordInput = passwordField.getText().trim();
                if (user == null || !passwordInput.isEmpty()) {
                    newUser.password = PasswordUtil.hashPassword(passwordInput);
                }

                newUser.name = nameField.getText().trim();

                newUser.role = roleComboBox.getSelectionModel().getSelectedItem();

                return newUser;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                if (user == null) {
                    // 添加新用户
                    if (UserDAO.insert(result)) {
                        audit("USER_CREATED", result.username);
                        users.put(result.username, result);
                        loadUsers();
                        updateStatus(I18nManager.getInstance().get("user.added"));
                    } else {
                        showError(I18nManager.getInstance().get("runtime.user_add_failed"));
                    }
                } else {
                    // 更新现有用户
                    if (UserDAO.update(result)) {
                        audit("USER_UPDATED", result.username);
                        users.put(result.username, result);
                        loadUsers();
                        updateStatus(I18nManager.getInstance().get("user.updated"));
                    } else {
                        showError(I18nManager.getInstance().get("runtime.user_update_failed"));
                    }
                }
            } catch (SQLException e) {
                logger.error("保存用户失败", e);
                showError(I18nManager.getInstance().get("runtime.user_save_failed", e.getMessage()));
            }
        });
    }

    private Label createUserFormLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private ListCell<String> createRoleCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : localizeRole(item));
                setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                setMinHeight(36);
                setPrefHeight(36);
                setMaxWidth(Double.MAX_VALUE);
            }
        };
    }

    private ListCell<String> createUserFilterCell(boolean roleFilter) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else if ("all".equals(item)) {
                    setText(I18nManager.getInstance().get("user.filter.all"));
                } else if (roleFilter) {
                    setText(localizeRole(item));
                } else {
                    setText(I18nManager.getInstance().get(
                        "active".equals(item) ? "user.enabled" : "user.disabled"));
                }
            }
        };
    }

    private String localizeRole(String role) {
        return I18nManager.getInstance().get(switch (role) {
            case "admin" -> "user.role.admin";
            case "finance" -> "user.role.finance";
            default -> "user.role.cashier";
        });
    }

    /**
     * 处理删除用户
     */
    @FXML
    public void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 不允许删除admin用户
            if ("admin".equals(selected.username)) {
                showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.admin_delete_forbidden"));
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get("common.confirm"));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.user_delete_confirm", selected.name));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    if (UserDAO.deleteByUsername(selected.username)) {
                        audit("USER_DELETED", selected.username);
                        users.remove(selected.username);
                        loadUsers();
                        updateStatus(I18nManager.getInstance().get("user.deleted"));
                    } else {
                        showError(I18nManager.getInstance().get("runtime.user_delete_result_failed"));
                    }
                } catch (SQLException e) {
                    logger.error("删除用户失败", e);
                    showError(I18nManager.getInstance().get("runtime.user_delete_failed", e.getMessage()));
                }
            }
        }
    }

    /**
     * 处理重置密码
     */
    @FXML
    public void handleResetPassword() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get("password_reset.title"));
            dialog.setHeaderText(null);
            dialog.setContentText(I18nManager.getInstance().get("runtime.enter_new_password"));

            dialog.showAndWait().ifPresent(newPassword -> {
                if (newPassword.trim().isEmpty()) {
                    showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.password_required"));
                    return;
                }

                selected.password = PasswordUtil.hashPassword(newPassword.trim());
                try {
                    if (UserDAO.update(selected)) {
                        audit("USER_PASSWORD_RESET", selected.username);
                        loadUsers();
                        updateStatus(I18nManager.getInstance().get("user.password_reset"));
                    } else {
                        showError(I18nManager.getInstance().get("runtime.password_reset_failed"));
                    }
                } catch (SQLException e) {
                    logger.error("重置密码失败", e);
                    showError(I18nManager.getInstance().get("runtime.user_reset_failed", e.getMessage()));
                }
            });
        }
    }

    /**
     * 处理激活用户
     */
    @FXML
    public void handleActivateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.active = true;
            try {
                if (UserDAO.update(selected)) {
                    audit("USER_ACTIVATED", selected.username);
                    loadUsers();
                    updateStatus(I18nManager.getInstance().get("user.activated"));
                } else {
                    showError(I18nManager.getInstance().get("runtime.user_activate_result_failed"));
                }
            } catch (SQLException e) {
                logger.error("激活用户失败", e);
                showError(I18nManager.getInstance().get("runtime.user_activate_failed", e.getMessage()));
            }
        }
    }

    /**
     * 处理禁用用户
     */
    @FXML
    public void handleDeactivateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 不允许禁用admin用户
            if ("admin".equals(selected.username)) {
                showError(com.cashier.i18n.I18nManager.getInstance().get("runtime.admin_disable_forbidden"));
                return;
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(I18nManager.getInstance().get("common.confirm"));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.user_disable_confirm", selected.name));

            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                selected.active = false;
                try {
                if (UserDAO.update(selected)) {
                    audit("USER_DEACTIVATED", selected.username);
                        loadUsers();
                        updateStatus(I18nManager.getInstance().get("user.deactivated"));
                    } else {
                        showError(I18nManager.getInstance().get("runtime.user_disable_result_failed"));
                    }
                } catch (SQLException e) {
                    logger.error("禁用用户失败", e);
                    showError(I18nManager.getInstance().get("runtime.user_disable_failed", e.getMessage()));
                }
            }
        }
    }

    private void audit(String operation, String targetUsername) {
        com.cashier.service.AuditService.success(
            currentUser == null ? null : currentUser.username,
            "USER", operation, "目标用户=" + targetUsername, 1);
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
        roleFilterComboBox.getSelectionModel().select(0);
        statusFilterComboBox.getSelectionModel().select(0);
        applyFilters();
    }

    /**
     * 应用筛选条件
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        String roleFilter = roleFilterComboBox.getSelectionModel().getSelectedItem();
        String statusFilter = statusFilterComboBox.getSelectionModel().getSelectedItem();

        userList.setAll(users.values().stream()
            .filter(u -> {
                // 角色筛选
                if (!"all".equals(roleFilter)) {
                    if (!roleFilter.equals(u.role)) {
                        return false;
                    }
                }

                // 状态筛选
                if (!"all".equals(statusFilter)) {
                    boolean active = "active".equals(statusFilter);
                    if (u.active != active) {
                        return false;
                    }
                }

                // 搜索文本筛选
                if (!searchText.isEmpty()) {
                    return u.username.toLowerCase().contains(searchText) ||
                           u.name.toLowerCase().contains(searchText);
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
     * 显示错误信息
     * @param message 错误消息
     */
    private void showError(String message) {
        com.cashier.util.StatusBarManager.updateError(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nManager.getInstance().get("label.error"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 刷新用户列表
     */
    public void refreshUsers() {
        loadUsers();
    }
}
