package com.cashier.controller;

import com.cashier.constant.SystemPropertyKeys;

import com.cashier.i18n.I18nKeys;

import com.cashier.CashierSystemFXApplication;
import com.cashier.constant.AppConstants;
import com.cashier.dao.ShiftDAO;
import com.cashier.model.Shift;
import com.cashier.model.User;
import com.cashier.service.DataService;
import com.cashier.util.FXMLUtils;
import com.cashier.util.FXUtils;
import com.cashier.util.StatusBarManager;
import com.cashier.i18n.I18nManager;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 主控制器
 * 处理主界面的导航和功能
 */
public class MainController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(MainController.class);

    // 静态引用，用于外部更新班次信息
    private static MainController instance;

    @FXML
    private Label currentUserLabel;

    @FXML
    private Label currentTimeLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Label avatarLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label currentShiftLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private TabPane tabPane;

    @FXML
    private MenuItem dataBackupItem;

    @FXML
    private MenuItem dataRestoreItem;

    @FXML
    private Button inventoryBtn;

    @FXML
    private Button cartBtn;

    @FXML
    private Button checkoutBtn;

    @FXML
    private Button transactionsBtn;

    @FXML
    private Button membersBtn;

    @FXML
    private Button supplierBtn;

    @FXML
    private Button purchaseOrderBtn;

    @FXML
    private Button purchaseApprovalBtn;

    @FXML
    private Button purchaseInboundBtn;

    @FXML
    private Button inventoryCheckBtn;

    @FXML
    private Button statisticsBtn;

    @FXML
    private Button purchaseReportBtn;

    @FXML
    private Button inventoryReportBtn;

@FXML
private Button profitReportBtn;

@FXML
private Button returnReportBtn;

@FXML
private Button promotionsBtn;

@FXML
private Button shiftBtn;

    @FXML
    private Button userManagementBtn;

    @FXML
    private Button settingsBtn;

    @FXML
    private Button auditLogBtn;

    @FXML
    private Button returnOrderBtn;

    @FXML
    private Button returnApprovalBtn;

    private CashierSystemFXApplication application;
    private User currentUser;
    private Timeline timeTimeline;
    private Button activeButton;
    private Map<String, Tab> openTabs = new HashMap<>(); // 管理打开的标签页
    private StackPane loadingOverlay;

    /**
     * 初始化方法
     */
    @FXML
    private void initialize() {
        // 保存实例引用
        instance = this;

        // 启动时间更新
        startTimeUpdate();

        // 设置初始激活按钮
        activeButton = inventoryBtn;

        // 绑定状态栏到 StatusBarManager
        statusLabel.textProperty().bind(StatusBarManager.statusProperty());
        StatusBarManager.statusLevelProperty().addListener((obs, oldLevel, newLevel) ->
            applyStatusLevelStyle(newLevel)
        );
        applyStatusLevelStyle(StatusBarManager.getStatusLevel());

        // 更新状态
        StatusBarManager.updateStatus("就绪");
        updateDate();
        updateShiftInfo();

        // 创建加载覆盖层
        createLoadingOverlay();

        // 设置快捷键
        setupShortcuts();
    }

    private void applyStatusLevelStyle(StatusBarManager.StatusLevel level) {
        statusLabel.getStyleClass().removeAll("text-success", "text-warning", "text-danger");

        StatusBarManager.StatusLevel nextLevel = level != null ? level : StatusBarManager.StatusLevel.NORMAL;
        switch (nextLevel) {
            case SUCCESS -> statusLabel.getStyleClass().add("text-success");
            case WARNING -> statusLabel.getStyleClass().add("text-warning");
            case ERROR -> statusLabel.getStyleClass().add("text-danger");
            case NORMAL -> {
                // 默认状态只保留 status-text
            }
            default -> logger.warn("未知状态栏级别: {}", nextLevel);
        }
    }

    /**
     * 设置快捷键
     */
    private void setupShortcuts() {
        // 获取场景
        if (tabPane.getScene() == null) {
            // 如果场景还未设置，延迟设置快捷键
            tabPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupSceneShortcuts(newScene);
                }
            });
        } else {
            setupSceneShortcuts(tabPane.getScene());
        }
    }

    /**
     * 为场景设置快捷键
     * @param scene 场景
     */
    private void setupSceneShortcuts(javafx.scene.Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleFunctionShortcut);
        scene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleControlShortcut);
    }

    private void handleFunctionShortcut(KeyEvent event) {
        switch (event.getCode()) {
            case F1:
                handleInventory();
                event.consume();
                break;
            case F5:
                refreshCurrentTab();
                event.consume();
                break;
            case F7:
                handleMembers();
                event.consume();
                break;
            case F8:
                handleCheckout();
                event.consume();
                break;
            case F9:
                handlePromotions();
                event.consume();
                break;
            case F10:
                handleInventoryAlert();
                event.consume();
                break;
            case F11:
                handleDataBackup();
                event.consume();
                break;
            case F12:
                handleDataRestore();
                event.consume();
                break;
            case ESCAPE:
                closeCurrentTabIfPossible(event);
                break;
            default:
                break;
        }
    }

    private void handleControlShortcut(KeyEvent event) {
        if (!event.isControlDown()) {
            return;
        }

        if (handleControlNavigationShortcut(event)) {
            return;
        }

        handleControlActionShortcut(event);
    }

    private boolean handleControlNavigationShortcut(KeyEvent event) {
        switch (event.getCode()) {
            case N, DIGIT1 -> consumeShortcut(event, this::handleInventory);
            case M -> consumeShortcut(event, this::handleMembers);
            case T -> consumeShortcut(event, this::handleStatistics);
            case DIGIT2 -> consumeShortcut(event, this::handleCart);
            case DIGIT3 -> consumeShortcut(event, this::handleTransactions);
            case DIGIT4 -> consumeShortcut(event, this::handleSettings);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleControlActionShortcut(KeyEvent event) {
        switch (event.getCode()) {
            case S -> consumeShortcut(event, () -> updateStatus("数据已保存"));
            case F -> consumeShortcut(event, event.isShiftDown()
                ? this::handleGlobalSearch
                : () -> showPlaceholder("搜索", "🔍", "搜索功能正在开发中..."));
            case D -> consumeShortcut(event, this::handleExportData);
            case R -> consumeShortcut(event, () -> updateStatus("已刷新"));
            case Q -> consumeShortcut(event, this::handleExit);
            case A -> event.consume();
            case E -> consumeShortcut(event, () -> showPlaceholder("编辑", "✏️", "编辑功能正在开发中..."));
            case B -> consumeShortcut(event, () -> showPlaceholder("批量操作", "📋", "批量操作功能正在开发中..."));
            case SLASH -> consumeShortcut(event, this::handleShortcutHelp);
            default -> {
            }
        }
    }

    private void closeCurrentTabIfPossible(KeyEvent event) {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        String welcomeTab = I18nManager.getInstance().get("main.welcome");
        if (selectedTab != null && !selectedTab.getText().equals(welcomeTab)) {
            tabPane.getTabs().remove(selectedTab);
            openTabs.remove(selectedTab.getText());
            event.consume();
        }
    }

    private void consumeShortcut(KeyEvent event, Runnable action) {
        action.run();
        event.consume();
    }

    /**
     * 设置应用程序引用
     * @param application 应用程序实例
     */
    public void setApplication(CashierSystemFXApplication application) {
        this.application = application;
    }

    /**
         * 设置当前用户
         * @param user 当前用户
         */
        public void setCurrentUser(User user) {
            this.currentUser = user;

            // 加载用户特定的语言偏好
            String userLanguage = com.cashier.service.DataService.loadLanguagePreference(user.username);
            com.cashier.i18n.I18nManager.getInstance().setLocale(userLanguage);

            // 默认管理员名称跟随当前语言；自定义姓名保持用户录入内容。
            String roleDisplayName = user.getRoleDisplayName();
            String displayName = "系统管理员".equals(user.name) ? roleDisplayName : user.name;
            currentUserLabel.setText(displayName + " (" + roleDisplayName + ")");
            userNameLabel.setText(displayName);
            userRoleLabel.setText(roleDisplayName);
            configurePermissions();

            // 设置头像（显示用户名的首字母）
            if (displayName != null && !displayName.isEmpty()) {
                avatarLabel.setText(displayName.substring(0, 1).toUpperCase());
            }
        }

    private void configurePermissions() {
        setButtonAccess(cartBtn, User.PERMISSION_CHECKOUT);
        setButtonAccess(checkoutBtn, User.PERMISSION_CHECKOUT);
        setButtonAccess(shiftBtn, User.PERMISSION_MANAGE_SHIFT);
        setButtonAccess(inventoryBtn, User.PERMISSION_VIEW_INVENTORY);
        setButtonAccess(inventoryCheckBtn, User.PERMISSION_MANAGE_INVENTORY);
        setButtonAccess(membersBtn, User.PERMISSION_MANAGE_MEMBERS);
        setButtonAccess(returnOrderBtn, User.PERMISSION_MANAGE_RETURNS);
        setButtonAccess(supplierBtn, User.PERMISSION_MANAGE_PURCHASE);
        setButtonAccess(purchaseOrderBtn, User.PERMISSION_MANAGE_PURCHASE);
        setButtonAccess(purchaseApprovalBtn, User.PERMISSION_MANAGE_PURCHASE);
        setButtonAccess(purchaseInboundBtn, User.PERMISSION_MANAGE_PURCHASE);
        setButtonAccess(transactionsBtn, User.PERMISSION_VIEW_TRANSACTIONS);
        setButtonAccess(statisticsBtn, User.PERMISSION_VIEW_REPORTS);
        setButtonAccess(promotionsBtn, User.PERMISSION_MANAGE_PROMOTIONS);
        setButtonAccess(purchaseReportBtn, User.PERMISSION_VIEW_REPORTS);
        setButtonAccess(inventoryReportBtn, User.PERMISSION_VIEW_REPORTS);
        setButtonAccess(profitReportBtn, User.PERMISSION_VIEW_REPORTS);
        setButtonAccess(returnReportBtn, User.PERMISSION_VIEW_REPORTS);
        setButtonAccess(userManagementBtn, User.PERMISSION_MANAGE_USERS);
        setButtonAccess(returnApprovalBtn, User.PERMISSION_APPROVE_RETURNS);
        setButtonAccess(settingsBtn, User.PERMISSION_MANAGE_SETTINGS);
        setButtonAccess(auditLogBtn, User.PERMISSION_VIEW_AUDIT);

        boolean canBackup = hasPermission(User.PERMISSION_BACKUP_RESTORE);
        if (dataBackupItem != null) {
            dataBackupItem.setVisible(canBackup);
        }
        if (dataRestoreItem != null) {
            dataRestoreItem.setVisible(canBackup);
        }
    }

    private void setButtonAccess(Button button, String permission) {
        if (button == null) {
            return;
        }
        boolean allowed = hasPermission(permission);
        button.setVisible(allowed);
        button.setManaged(allowed);
    }

    private boolean hasPermission(String permission) {
        return currentUser != null && currentUser.hasPermission(permission);
    }

    private boolean requirePermission(String permission) {
        if (hasPermission(permission)) {
            return true;
        }
        showError(I18nManager.getInstance().get("permission.access_denied"));
        return false;
    }
    /**
     * 启动时间更新
     */
    private void startTimeUpdate() {
        timeTimeline = new Timeline(new KeyFrame(
            Duration.seconds(1),
            event -> updateTime()
        ));
        timeTimeline.setCycleCount(Animation.INDEFINITE);
        timeTimeline.play();
    }

    /**
     * 更新时间
     */
    private void updateTime() {
        currentTimeLabel.setText(LocalDateTime.now(ZoneId.systemDefault())
            .format(com.cashier.util.DateTimeFormats.TIME));
        // 同时更新班次信息
        updateShiftInfo();
    }

    /**
     * 更新日期
     */
    private void updateDate() {
        dateLabel.setText(LocalDate.now(ZoneId.systemDefault())
            .format(com.cashier.util.DateTimeFormats.FULL_DATE));
    }

    /**
     * 更新班次信息
     */
    private void updateShiftInfo() {
        try {
            Shift activeShift = ShiftDAO.findActiveShift();
            if (activeShift != null) {
                // 有活跃班次
                String startTime = LocalDateTime.ofInstant(activeShift.startTime, ZoneId.systemDefault())
                    .format(com.cashier.util.DateTimeFormats.TIME_HOUR_MINUTE);
                currentShiftLabel.setText(I18nManager.getInstance().get("runtime.shift_summary",
                        activeShift.shiftId, activeShift.operatorName, startTime));
            } else {
                // 无活跃班次
                currentShiftLabel.setText(com.cashier.i18n.I18nManager.getInstance().get("status.shift_not_started"));
            }
        } catch (Exception e) {
            logger.error("更新班次信息失败", e);
            currentShiftLabel.setText(I18nManager.getInstance().get("runtime.shift_unknown"));
        }
    }

    /**
     * 更新班次信息（公共静态方法，供其他控制器调用）
     */
    public static void updateShiftInfoGlobal() {
        if (instance != null) {
            instance.updateShiftInfo();
        }
    }

    /**
     * 更新状态栏
     * @param status 状态文本
     */
    private void updateStatus(String status) {
        StatusBarManager.updateStatus(status);
    }

    /**
     * 切换导航按钮激活状态
     * @param button 要激活的按钮
     */
    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
        }
        if (button != null) {
            button.getStyleClass().add("nav-button-active");
            activeButton = button;
        }
    }

    // ========== 菜单处理方法 ==========

    @FXML
    public void handleLogout() {
        // 检查是否有活跃班次
        if (com.cashier.service.DataService.hasActiveShift()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.CONFIRM_EXIT));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.logout_active_shift"));
            
            ButtonType yesButton = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get("runtime.end_shift_first"), ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get("runtime.exit_directly"), ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrder.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
            
            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == yesButton) {
                    // 用户选择先交班，跳转到交班管理页面
                    handleShift();
                } else if (buttonType == noButton) {
                    // 用户选择直接退出
                    if (application != null) {
                        application.logoutToLoginView();
                    }
                }
                // 如果选择取消，不做任何操作
            });
        } else {
            // 没有活跃班次，直接退出
            if (FXUtils.showConfirmAlert(I18nManager.getInstance().get(I18nKeys.Runtime.CONFIRM_EXIT), I18nManager.getInstance().get("runtime.logout_confirm"))) {
                if (application != null) {
                    application.logoutToLoginView();
                }
            }
        }
    }

    @FXML
    public void handleExit() {
        // 检查是否有活跃班次
        if (com.cashier.service.DataService.hasActiveShift()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Runtime.CONFIRM_EXIT));
            alert.setHeaderText(null);
            alert.setContentText(I18nManager.getInstance().get("runtime.exit_active_shift"));
            
            ButtonType yesButton = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get("runtime.end_shift_first"), ButtonBar.ButtonData.YES);
            ButtonType noButton = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get("runtime.exit_directly"), ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.ReturnOrder.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
            
            alert.showAndWait().ifPresent(buttonType -> {
                if (buttonType == yesButton) {
                    // 用户选择先交班，跳转到交班管理页面
                    handleShift();
                } else if (buttonType == noButton) {
                    // 用户选择直接退出
                    exitApplication();
                }
                // 如果选择取消，不做任何操作
            });
        } else {
            // 没有活跃班次，直接退出
            if (FXUtils.showConfirmAlert("确认退出", "确定要退出系统吗？")) {
                exitApplication();
            }
        }
    }

    private void exitApplication() {
        if (application != null) {
            application.exitApplication();
        } else {
            javafx.application.Platform.exit();
        }
    }

    @FXML
    public void handleUserManagement() {
        if (!requirePermission(User.PERMISSION_MANAGE_USERS)) return;
        updateStatus("用户管理");
        setActiveButton(userManagementBtn);
        
        try {
            // 加载用户管理界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/UserView.fxml");
            VBox root = loader.load();
            
            // 获取控制器
            UserController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.USER_MANAGEMENT), root);
            
        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleDataBackup() {
        if (!requirePermission(User.PERMISSION_BACKUP_RESTORE)) return;
        updateStatus("数据备份");
        
        try {
            // 创建备份目录
            String timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(com.cashier.util.DateTimeFormats.BACKUP_TIMESTAMP);
            String backupPath = "backup_" + timestamp;
            
            // 执行备份
            DataService.backupData(backupPath);
            
            FXUtils.showInfoAlert("备份成功", "数据备份成功！\n备份位置: " + backupPath);
        } catch (Exception e) {
            FXUtils.showErrorAlert("备份失败", "数据备份失败: " + e.getMessage());
        }
    }
    
    @FXML
    public void handleDataRestore() {
        if (!requirePermission(User.PERMISSION_BACKUP_RESTORE)) return;
        updateStatus("数据恢复");
        
        // 列出可用的备份目录
        File projectDir = new File(System.getProperty(SystemPropertyKeys.USER_DIR));
        File[] backupDirs = projectDir.listFiles((dir, name) -> 
            name.startsWith("backup_") && dir.isDirectory()
        );
        
        if (backupDirs == null || backupDirs.length == 0) {
            FXUtils.showErrorAlert("无备份", "未找到任何备份目录！\n请先进行数据备份。");
            return;
        }
        
        // 按修改时间排序，最新的在前
        Arrays.sort(backupDirs, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        
        // 创建选择对话框
        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.choose_backup"));
        dialog.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get("runtime.choose_backup_header"));
        dialog.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.available_backups"));
        
        // 添加备份选项
        ObservableList<String> options = FXCollections.observableArrayList();
        for (File dir : backupDirs) {
            String timeStr = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(dir.lastModified()), ZoneId.systemDefault())
                .format(com.cashier.util.DateTimeFormats.STANDARD_DATE_TIME);
            options.add(dir.getName() + " (" + timeStr + ")");
        }
        dialog.getItems().addAll(options);
        
        dialog.showAndWait().ifPresent(selected -> {
            // 提取备份目录名
            String backupDirName = selected.split(" \\(")[0];
            
            try {
                // 确认恢复
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.confirm_restore"));
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText(I18nManager.getInstance().get("runtime.restore_confirm_short", backupDirName));
                
                if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    DataService.restoreData(backupDirName);
                    FXUtils.showInfoAlert(I18nManager.getInstance().get("runtime.restore_success_title"), I18nManager.getInstance().get("runtime.restore_success"));
                }
            } catch (Exception e) {
                FXUtils.showErrorAlert("恢复失败", "数据恢复失败: " + e.getMessage());
            }
        });
    }    @FXML
    public void handleExportData() {
        if (!requirePermission(User.PERMISSION_EXPORT_DATA)) return;
        updateStatus("导出数据");
        FXUtils.showInfoAlert("开发中", "导出数据功能正在开发中...");
    }

    /**
     * 显示快捷键帮助对话框
     */
    @FXML
    public void handleShortcutHelp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cashier/view/ShortcutHelpView.fxml"));
            I18nManager i18n = I18nManager.getInstance();
            loader.setResources(i18n.getResourceBundle());

            VBox root = loader.load();
            ShortcutHelpController controller = loader.getController();

            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.NONE);
            dialogStage.initOwner(tabPane.getScene().getWindow());
            dialogStage.setTitle(i18n.get("快捷键.title"));
            dialogStage.setResizable(false);

            Scene scene = new Scene(root, 700, 600);
            com.cashier.util.ThemeUtils.applyCurrentTheme(scene, getClass());

            dialogStage.setScene(scene);
            controller.setStage(dialogStage);

            dialogStage.show();
        } catch (IOException e) {
            logger.error("加载快捷键帮助界面失败", e);
            FXUtils.showErrorAlert(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.ERROR), "无法打开快捷键帮助");
        }
    }

    /**
     * 显示全局搜索对话框
     */
    @FXML
    public void handleGlobalSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cashier/view/SearchView.fxml"));
            I18nManager i18n = I18nManager.getInstance();
            loader.setResources(i18n.getResourceBundle());

            VBox root = loader.load();
            SearchController controller = loader.getController();

            // 创建对话框
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.NONE);
            dialogStage.initOwner(tabPane.getScene().getWindow());
            dialogStage.setTitle(i18n.get("search.title"));
            dialogStage.setResizable(false);

            Scene scene = new Scene(root, 600, 500);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            dialogStage.setScene(scene);
            controller.setStage(dialogStage);

            dialogStage.showAndWait();
        } catch (IOException e) {
            logger.error("加载全局搜索界面失败", e);
            FXUtils.showErrorAlert(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.ERROR), "无法打开全局搜索");
        }
    }

    @FXML
    public void handleLightTheme() {
        if (application != null) {
            application.applyTheme(application.getPrimaryStage().getScene(), "light");
            updateStatus("已切换到浅色主题");
        }
    }

    @FXML
    public void handleDarkTheme() {
        if (application != null) {
            application.applyTheme(application.getPrimaryStage().getScene(), "dark");
            updateStatus("已切换到深色主题");
        }
    }

    @FXML
    public void handleLiSuanTheme() {
        if (application != null) {
            application.applyTheme(application.getPrimaryStage().getScene(), "lisuan");
            updateStatus("已切换到 LiSuan 主题");
        }
    }

    @FXML
    public void handleAbout() {
        String about =
            AppConstants.APP_NAME + "\n\n" +
            "版本: " + AppConstants.FULL_VERSION_STRING + "\n" +
            "开发: " + AppConstants.DEVELOPER + "\n\n" +
            "技术栈:\n" +
            "- JavaFX " + AppConstants.JAVAFX_VERSION + "\n" +
            "- Maven " + AppConstants.MIN_MAVEN_VERSION + "+\n" +
            "- JDK " + AppConstants.MIN_JDK_VERSION + "/21\n\n" +
            "许可证: " + AppConstants.LICENSE;

        showInformationOnlyAlert(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Menu.Help.ABOUT), about);
    }

    private void showInformationOnlyAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========== 导航处理方法 ==========

    @FXML
    public void handleInventory() {
        if (!requirePermission(User.PERMISSION_VIEW_INVENTORY)) return;
        updateStatus("商品管理");
        setActiveButton(inventoryBtn);
        
        try {
            // 加载商品管理界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/InventoryView.fxml");
            VBox root = loader.load();
            
            // 获取控制器
            InventoryController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            
            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.INVENTORY), root);
            
        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleCart() {
        if (!requirePermission(User.PERMISSION_CHECKOUT)) return;
        updateStatus("POS");
        setActiveButton(cartBtn);

        try {
            String title = I18nManager.getInstance().get(I18nKeys.Nav.CART);
            if (selectOpenTab(title)) {
                logger.debug("MainController: 购物车标签页已打开，直接切换");
                focusSelectedCartSearchField();
                return;
            }

            logger.debug("MainController: 开始加载购物车界面...");
            // 加载购物车界面（购物车和结账已合并）
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/CartView.fxml");
            logger.debug("MainController: FXML文件路径: {}", getClass().getResource("/com/cashier/view/CartView.fxml"));
            VBox root = loader.load();

            // 获取控制器
            CartController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            root.getProperties().put("controller", controller);
            logger.debug("MainController: 获取控制器成功");

            // 创建内容标签页
            createContentTab(title, root);
            javafx.application.Platform.runLater(controller::focusSearchField);
            logger.debug("MainController: 购物车界面加载成功");

        } catch (IOException e) {
            logger.error("加载购物车界面失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + getErrorMessage(e));
        } catch (Exception e) {
            logger.error("加载购物车界面时发生异常", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + getErrorMessage(e));
        }
    }

    @FXML
    public void handleCheckout() {
        if (!requirePermission(User.PERMISSION_CHECKOUT)) return;
        updateStatus("POS");
        setActiveButton(checkoutBtn);
        
        try {
            String title = I18nManager.getInstance().get(I18nKeys.Nav.CART);
            if (selectOpenTab(title)) {
                focusSelectedCartSearchField();
                return;
            }

            // 加载购物车界面（购物车和结账已合并）
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/CartView.fxml");
            VBox root = loader.load();
            
            // 获取控制器
            CartController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            root.getProperties().put("controller", controller);
            
            // 创建内容标签页
            createContentTab(title, root);
            javafx.application.Platform.runLater(controller::focusSearchField);
            
        } catch (IOException e) {
            logger.error("加载结账界面失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + getErrorMessage(e));
        } catch (Exception e) {
            logger.error("加载结账界面时发生异常", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + getErrorMessage(e));
        }
    }

    private void focusSelectedCartSearchField() {
        javafx.application.Platform.runLater(() -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null && selectedTab.getContent() != null) {
                javafx.scene.Node searchNode = selectedTab.getContent().lookup("#searchField");
                if (searchNode != null) {
                    searchNode.requestFocus();
                }
            }
        });
    }

    @FXML
    public void handleTransactions() {
        if (!requirePermission(User.PERMISSION_VIEW_TRANSACTIONS)) return;
        updateStatus("交易记录");
        setActiveButton(transactionsBtn);

        try {
            // 加载交易记录界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/TransactionView.fxml");
            VBox root = loader.load();

            // 获取控制器
            TransactionController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.TRANSACTIONS), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleMembers() {
        if (!requirePermission(User.PERMISSION_MANAGE_MEMBERS)) return;
        updateStatus("会员管理");
        setActiveButton(membersBtn);
        
        try {
            // 加载会员管理界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/MemberView.fxml");
            VBox root = loader.load();
            
            // 获取控制器
            MemberController controller = loader.getController();
            
            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.MEMBERS), root);
            
        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleSupplier() {
        if (!requirePermission(User.PERMISSION_MANAGE_PURCHASE)) return;
        updateStatus("供应商管理");
        setActiveButton(supplierBtn);

        try {
            // 加载供应商管理界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/SupplierView.fxml");
            VBox root = loader.load();

            // 获取控制器
            SupplierController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.SUPPLIER), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handlePurchaseOrder() {
        if (!requirePermission(User.PERMISSION_MANAGE_PURCHASE)) return;
        updateStatus("采购订单");
        setActiveButton(purchaseOrderBtn);

        try {
            // 加载采购订单界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/PurchaseOrderView.fxml");
            VBox root = loader.load();

            // 获取控制器
            PurchaseOrderController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.PURCHASE_ORDER), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handlePurchaseApproval() {
        if (!requirePermission(User.PERMISSION_MANAGE_PURCHASE)) return;
        updateStatus("采购审批");
        setActiveButton(purchaseApprovalBtn);

        try {
            // 加载采购审批界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/PurchaseApprovalView.fxml");
            VBox root = loader.load();

            // 获取控制器
            PurchaseApprovalController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.PURCHASE_APPROVAL), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handlePurchaseInbound() {
        if (!requirePermission(User.PERMISSION_MANAGE_PURCHASE)) return;
        updateStatus("采购入库");
        setActiveButton(purchaseInboundBtn);

        try {
            // 加载采购入库界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/PurchaseInboundView.fxml");
            VBox root = loader.load();

            // 获取控制器
            PurchaseInboundController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.PURCHASE_INBOUND), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleInventoryCheck() {
        if (!requirePermission(User.PERMISSION_MANAGE_INVENTORY)) return;
        updateStatus("库存盘点");
        setActiveButton(inventoryCheckBtn);

        try {
            // 加载库存盘点界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/InventoryCheckView.fxml");
            VBox root = loader.load();

            // 获取控制器
            InventoryCheckController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.INVENTORY_CHECK), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleStatistics() {
        if (!requirePermission(User.PERMISSION_VIEW_REPORTS)) return;
        updateStatus("数据统计");
        setActiveButton(statisticsBtn);

        try {
            // 加载数据统计界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/StatisticsView.fxml");
            VBox root = loader.load();

            // 获取控制器
            StatisticsController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.STATISTICS), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleInventoryAlert() {
        if (!requirePermission(User.PERMISSION_VIEW_INVENTORY)) return;
        updateStatus("库存预警");
        setActiveButton(inventoryReportBtn);

        try {
            // 加载库存预警界面（在新窗口中打开）
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/InventoryAlertView.fxml");
            javafx.scene.Parent root = loader.load();

            // 获取控制器
            InventoryAlertController controller = loader.getController();

            // 创建新窗口
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(I18nManager.getInstance().get("runtime.inventory_alert_title", currentUser != null ? currentUser.name : ""));
            stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            stage.setResizable(false);

            // 应用主题
            String username = currentUser != null ? currentUser.username : "default";
            String currentTheme = com.cashier.service.DataService.loadThemePreference(username);
            if (application != null) {
                application.applyTheme(stage.getScene(), currentTheme);
            }

            // 显示窗口
            stage.show();

            // 窗口关闭时清理资源
            stage.setOnHiding(event -> {
                controller.cleanup();
            });

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handlePurchaseReport() {
        if (!requirePermission(User.PERMISSION_VIEW_REPORTS)) return;
        updateStatus("采购报表");
        setActiveButton(purchaseReportBtn);

        try {
            // 加载采购报表界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/PurchaseReportView.fxml");
            VBox root = loader.load();

            // 获取控制器
            PurchaseReportController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.PURCHASE_REPORT), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleInventoryReport() {
        if (!requirePermission(User.PERMISSION_VIEW_REPORTS)) return;
        updateStatus("库存报表");
        setActiveButton(inventoryReportBtn);

        try {
            // 加载库存报表界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/InventoryReportView.fxml");
            VBox root = loader.load();

            // 获取控制器
            InventoryReportController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.INVENTORY_REPORT), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleProfitReport() {
        if (!requirePermission(User.PERMISSION_VIEW_REPORTS)) return;
        updateStatus("利润分析");
        setActiveButton(profitReportBtn);

        try {
            // 加载利润分析界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ProfitReportView.fxml");
            VBox root = loader.load();

            // 获取控制器
            ProfitReportController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.PROFIT_REPORT), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleReturnReport() {
        if (!requirePermission(User.PERMISSION_VIEW_REPORTS)) return;
        updateStatus("退货报表");
        setActiveButton(returnReportBtn);

        try {
            // 加载退货报表界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ReturnReportView.fxml");
            VBox root = loader.load();

            // 获取控制器
            ReturnReportController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get("nav.return_report"), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handlePromotions() {
        if (!requirePermission(User.PERMISSION_MANAGE_PROMOTIONS)) return;
        updateStatus("促销管理");
        setActiveButton(promotionsBtn);

        try {
            // 加载促销管理界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/PromotionView.fxml");
            VBox root = loader.load();

            // 获取控制器
            PromotionController controller = loader.getController();

            // 创建内容标签页
            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.PROMOTIONS), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleShift() {
        if (!requirePermission(User.PERMISSION_MANAGE_SHIFT)) return;
        updateStatus("交接班");
        setActiveButton(shiftBtn);

        try {
            String title = I18nManager.getInstance().get(I18nKeys.Nav.SHIFT);
            if (selectOpenTab(title)) {
                return;
            }

            // 加载交接班界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ShiftView.fxml");
            VBox root = loader.load();

            // 获取控制器
            ShiftController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            // 创建内容标签页
            createContentTab(title, root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleSettings() {
        if (!requirePermission(User.PERMISSION_MANAGE_SETTINGS)) return;
        updateStatus("系统设置");
        setActiveButton(settingsBtn);

        try {
            String title = I18nManager.getInstance().get(I18nKeys.Nav.SETTINGS);
            if (selectOpenTab(title)) {
                return;
            }

            // 加载设置界面
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/SettingsView.fxml");
            VBox root = loader.load();

            // 获取控制器并设置当前用户
            SettingsController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            // 创建内容标签页
            createContentTab(title, root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleAuditLogs() {
        if (!requirePermission(User.PERMISSION_VIEW_AUDIT)) return;
        updateStatus(I18nManager.getInstance().get("nav.audit_logs"));
        setActiveButton(auditLogBtn);
        try {
            String title = I18nManager.getInstance().get("nav.audit_logs");
            if (selectOpenTab(title)) {
                return;
            }
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/AuditLogView.fxml");
            VBox root = loader.load();
            createContentTab(title, root);
        } catch (IOException e) {
            logger.error("加载审计日志页面失败", e);
            showError(I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleReturnOrder() {
        if (!requirePermission(User.PERMISSION_MANAGE_RETURNS)) return;
        updateStatus("退货订单");
        setActiveButton(returnOrderBtn);

        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ReturnOrderView.fxml");
            VBox root = loader.load();

            ReturnOrderController controller = loader.getController();

            createContentTab(I18nManager.getInstance().get(I18nKeys.Nav.RETURN_ORDER), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    @FXML
    public void handleReturnApproval() {
        if (!requirePermission(User.PERMISSION_APPROVE_RETURNS)) return;
        updateStatus("退货审批");
        setActiveButton(returnApprovalBtn);

        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ReturnApprovalView.fxml");
            VBox root = loader.load();

            ReturnApprovalController controller = loader.getController();

            createContentTab(I18nManager.getInstance().get("nav.return_approval"), root);

        } catch (IOException e) {
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    /**
     * 创建自定义关闭按钮
     * 解决 Linux 系统下默认关闭按钮不显示的问题
     * @param tab 要关闭的标签页
     * @return 关闭按钮节点
     */
    private javafx.scene.Node createCloseButton(Tab tab) {
        Label closeButton = new Label("×");
        closeButton.getStyleClass().add("custom-tab-close");
        closeButton.setOnMouseClicked(event -> {
            disposeTabContent(tab);
            // 从标签面板中移除标签页
            tabPane.getTabs().remove(tab);
            // 从打开的标签页映射中移除
            openTabs.values().remove(tab);
        });
        return closeButton;
    }

    private void disposeTabContent(Tab tab) {
        if (tab == null || tab.getContent() == null) {
            return;
        }

        Object controller = tab.getContent().getProperties().get("controller");
        if (controller instanceof CartController cartController) {
            cartController.dispose();
        }
    }

    /**
     * Creates a content-sized tab header so longer localized titles are not
     * compressed into the former fixed-width tab.
     */
    private javafx.scene.Node createTabHeader(Tab tab, String title) {
        javafx.scene.Node closeButton = createCloseButton(tab);
        javafx.scene.layout.HBox tabHeader = new javafx.scene.layout.HBox(1);
        tabHeader.setAlignment(javafx.geometry.Pos.CENTER);
        tabHeader.getStyleClass().add("content-tab-header");
        tabHeader.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        tabHeader.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        Label headerLabel = new Label(title);
        headerLabel.getStyleClass().addAll("text-default", "content-tab-title");
        headerLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        headerLabel.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        headerLabel.setTooltip(new Tooltip(title));

        tabHeader.getChildren().addAll(headerLabel, closeButton);
        return tabHeader;
    }

    private boolean selectOpenTab(String title) {
        Tab tab = openTabs.get(title);
        if (tab == null) {
            return false;
        }
        if (!tabPane.getTabs().contains(tab)) {
            openTabs.remove(title);
            return false;
        }
        tabPane.getSelectionModel().select(tab);
        return true;
    }

    private String getErrorMessage(Exception e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }

    /**
         * 显示占位符内容
         * @param title 标题
         * @param icon 图标
         * @param message 消息
         */
        private void showPlaceholder(String title, String icon, String message) {
            // 检查是否已经打开该标签页
            if (openTabs.containsKey(title)) {
                // 如果已打开，切换到该标签页
                tabPane.getSelectionModel().select(openTabs.get(title));
                return;
            }

            // 创建新的标签页（使用自定义graphic控制宽度）
            Tab tab = new Tab();
            tab.setClosable(false); // 禁用默认关闭按钮

            tab.getStyleClass().add("custom-tab-header");
            tab.setGraphic(createTabHeader(tab, title));
            tab.setText("");

            // 创建占位符内容
            VBox placeholder = new VBox(20);
            placeholder.setAlignment(javafx.geometry.Pos.CENTER);
            placeholder.getStyleClass().add("surface-muted");
            placeholder.setStyle("-fx-padding: 40;");

            Label iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 64px; -fx-font-family: 'Segoe UI Symbol', 'Microsoft YaHei', sans-serif;");

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("text-primary");
            titleLabel.getStyleClass().add("title-2xl");

            Label messageLabel = new Label(message);
            messageLabel.getStyleClass().add("text-muted");
            messageLabel.getStyleClass().add("text-lg");

            placeholder.getChildren().addAll(iconLabel, titleLabel, messageLabel);

            tab.setContent(placeholder);

            // 添加到标签页管理器
            openTabs.put(title, tab);

            // 添加到TabPane
            tabPane.getTabs().add(tab);

            // 切换到新标签页
            tabPane.getSelectionModel().select(tab);
        }
    
        /**
         * 创建内容标签页
         * @param title 标题
         * @param content 内容节点
         */
        private void createContentTab(String title, javafx.scene.Node content) {
            // 检查是否已经打开该标签页
            if (openTabs.containsKey(title)) {
                // 如果已打开，切换到该标签页
                tabPane.getSelectionModel().select(openTabs.get(title));
                return;
            }

            // 创建新的标签页（使用自定义graphic控制宽度）
            Tab tab = new Tab();
            tab.setClosable(false); // 禁用默认关闭按钮

            tab.getStyleClass().add("custom-tab-header");
            tab.setGraphic(createTabHeader(tab, title));
            tab.setText("");
            tab.setContent(content);

            // 添加标签页关闭事件
            tab.setOnClosed(event -> {
                disposeTabContent(tab);
                openTabs.remove(title);
            });

            // 添加到标签页管理器
            openTabs.put(title, tab);

            // 添加到TabPane
            tabPane.getTabs().add(tab);

            // 切换到新标签页
            tabPane.getSelectionModel().select(tab);
        }

    /**
     * 刷新当前标签页
     */
    private void refreshCurrentTab() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null || selectedTab.getText().equals("欢迎")) {
            updateStatus("无需刷新");
            return;
        }

        String title = selectedTab.getText();

        // 关闭当前标签页
        if (openTabs.containsKey(title)) {
            tabPane.getTabs().remove(openTabs.get(title));
            openTabs.remove(title);
        }

        // 根据标题重新打开对应的界面
        switch (title) {
            case "商品管理":
                handleInventory();
                break;
            case "pos/结账":
                handleCheckout();
                break;
            case "交易记录":
                handleTransactions();
                break;
            case "会员管理":
                handleMembers();
                break;
            case "数据统计":
                handleStatistics();
                break;
            case "促销管理":
                handlePromotions();
                break;
            case "交班管理":
                handleShift();
                break;
            case "系统设置":
                handleSettings();
                break;
            case "用户管理":
                handleUserManagement();
                break;
            default:
                updateStatus("无法刷新: " + title);
                return;
        }

        updateStatus("已刷新: " + title);
    }
    
        /**
    
             * 关闭所有标签页（除了欢迎页）
    
             */
    
            private void closeAllTabs() {
    
                // 保留第一个标签页（欢迎页）
    
                while (tabPane.getTabs().size() > 1) {
    
                    Tab tab = tabPane.getTabs().get(tabPane.getTabs().size() - 1);
    
                    openTabs.remove(tab.getText());
    
                    tabPane.getTabs().remove(tab);
    
                }
    
            }
    
        
    
            /**
    
        
    
                 * 创建加载覆盖层
    
        
    
                 */
    
        
    
                private void createLoadingOverlay() {
    
        
    
                    loadingOverlay = new StackPane();
    
        
    
                    loadingOverlay.getStyleClass().add("loading-overlay");
    
        
    
            
    
        
    
                    ProgressIndicator progressIndicator = new ProgressIndicator();
    
        
    
                    progressIndicator.getStyleClass().add("loading-progress");
    
        
    
            
    
        
    
                    Label loadingLabel = new Label(com.cashier.i18n.I18nManager.getInstance().get("data.loading"));
    
        
    
                    loadingLabel.getStyleClass().add("loading-label");
    
        
    
            
    
        
    
                    VBox vbox = new VBox(10, progressIndicator, loadingLabel);
    
        
    
                    vbox.setAlignment(javafx.geometry.Pos.CENTER);
    
        
    
                    vbox.getStyleClass().add("loading-card");
    
        
    
            
    
        
    
                    loadingOverlay.getChildren().add(vbox);
    
        
    
                    loadingOverlay.setVisible(false);
    
        
    
                    loadingOverlay.setMouseTransparent(true);
    
        
    
                }
    
        
    
            
    
        
    
                /**
    
        
    
                 * 显示加载动画
    
        
    
                 */
    
        
    
                private void showLoading() {
    
        
    
                    loadingOverlay.setVisible(true);
    
        
    
                    loadingOverlay.setMouseTransparent(false);
    
        
    
                }
    
        
    
            
    
        
    
                /**
    
        
    
            
    
        
    
                     * 隐藏加载动画
    
        
    
            
    
        
    
                     */
    
        
    
            
    
        
    
                    private void hideLoading() {
    
        
    
            
    
        
    
                        loadingOverlay.setVisible(false);
    
        
    
            
    
        
    
                        loadingOverlay.setMouseTransparent(true);
    
        
    
            
    
        
    
                    }
    
        
    
            
    
        
    
                
    
        
    
            
    
        
    
                    /**
    
        
    
            
    
        
    
                     * 显示错误信息
    
        
    
            
    
        
    
                     * @param message 错误消息
    
        
    
            
    
        
    
                     */
    
        
    
            
    
        
    
                    private void showError(String message) {
                        com.cashier.util.StatusBarManager.updateError(message);
    
        
    
            
    
        
    
                        Alert alert = new Alert(Alert.AlertType.ERROR);
    
        
    
            
    
        
    
                        alert.setTitle(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Label.ERROR));
    
        
    
            
    
        
    
                        alert.setHeaderText(null);
    
        
    
            
    
        
    
                        alert.setContentText(message);
    
        
    
            
    
        
    
                        alert.showAndWait();
    
        
    
            
    
        
    
                    }
    
        
    
            /**
     * 异步加载内容
     * @param title 标题
     * @param icon 图标
     * @param message 消息
     */
    private void showPlaceholderAsync(String title, String icon, String message) {
        showLoading();

        // 使用 JavaFX PauseTransition 替代 Thread.sleep
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        pause.setOnFinished(event -> {
            showPlaceholder(title, icon, message);
            hideLoading();
        });
        pause.play();
    }

    /**
     * 清理资源，防止内存泄漏
     */
    public void cleanup() {
        // 停止时间更新动画
        if (timeTimeline != null) {
            timeTimeline.stop();
            timeTimeline = null;
        }
    }
}
