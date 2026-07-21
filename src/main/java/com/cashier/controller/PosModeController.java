package com.cashier.controller;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import com.cashier.CashierSystemFXApplication;
import com.cashier.model.User;
import com.cashier.util.FXMLUtils;
import com.cashier.util.StatusBarManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.io.IOException;
import java.util.Map;

/**
 * POS模式控制器
 * 专门为收银员设计的简化界面，只包含收银台和交接班功能
 */
public class PosModeController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PosModeController.class);
    private static final String AVATAR_CIRCLE_CLASS = "pos-avatar-circle";
    private static final String AVATAR_ADMIN_CLASS = "pos-avatar-admin";
    private static final String AVATAR_FINANCE_CLASS = "pos-avatar-finance";
    private static final String AVATAR_CASHIER_CLASS = "pos-avatar-cashier";
    private static final Map<Character, String> PINYIN_FIRST_LETTER = Map.ofEntries(
        Map.entry('白', "B"),
        Map.entry('蔡', "C"), Map.entry('陈', "C"), Map.entry('程', "C"), Map.entry('崔', "C"), Map.entry('常', "C"),
        Map.entry('戴', "D"), Map.entry('邓', "D"), Map.entry('丁', "D"), Map.entry('董', "D"), Map.entry('杜', "D"), Map.entry('段', "D"),
        Map.entry('范', "F"), Map.entry('方', "F"), Map.entry('冯', "F"), Map.entry('傅', "F"),
        Map.entry('高', "G"), Map.entry('葛', "G"), Map.entry('龚', "G"), Map.entry('郭', "G"), Map.entry('顾', "G"),
        Map.entry('韩', "H"), Map.entry('郝', "H"), Map.entry('何', "H"), Map.entry('贺', "H"), Map.entry('侯', "H"), Map.entry('胡', "H"), Map.entry('黄', "H"),
        Map.entry('贾', "J"), Map.entry('姜', "J"), Map.entry('江', "J"), Map.entry('蒋', "J"), Map.entry('金', "J"),
        Map.entry('康', "K"), Map.entry('孔', "K"),
        Map.entry('赖', "L"), Map.entry('雷', "L"), Map.entry('黎', "L"), Map.entry('李', "L"), Map.entry('梁', "L"), Map.entry('廖', "L"),
        Map.entry('林', "L"), Map.entry('刘', "L"), Map.entry('龙', "L"), Map.entry('罗', "L"), Map.entry('卢', "L"), Map.entry('陆', "L"), Map.entry('吕', "L"),
        Map.entry('马', "M"), Map.entry('毛', "M"), Map.entry('孟', "M"),
        Map.entry('潘', "P"), Map.entry('彭', "P"),
        Map.entry('钱', "Q"), Map.entry('乔', "Q"), Map.entry('秦', "Q"), Map.entry('邱', "Q"),
        Map.entry('任', "R"),
        Map.entry('沈', "S"), Map.entry('史', "S"), Map.entry('石', "S"), Map.entry('宋', "S"), Map.entry('苏', "S"), Map.entry('孙', "S"), Map.entry('邵', "S"),
        Map.entry('谭', "T"), Map.entry('汤', "T"), Map.entry('唐', "T"), Map.entry('田', "T"),
        Map.entry('万', "W"), Map.entry('汪', "W"), Map.entry('王', "W"), Map.entry('魏', "W"), Map.entry('文', "W"), Map.entry('吴', "W"), Map.entry('武', "W"),
        Map.entry('夏', "X"), Map.entry('萧', "X"), Map.entry('谢', "X"), Map.entry('熊', "X"), Map.entry('徐', "X"), Map.entry('许', "X"), Map.entry('薛', "X"),
        Map.entry('阎', "Y"), Map.entry('杨', "Y"), Map.entry('姚', "Y"), Map.entry('叶', "Y"), Map.entry('易', "Y"), Map.entry('尹', "Y"),
        Map.entry('于', "Y"), Map.entry('余', "Y"), Map.entry('袁', "Y"),
        Map.entry('曾', "Z"), Map.entry('张', "Z"), Map.entry('赵', "Z"), Map.entry('郑', "Z"), Map.entry('钟', "Z"), Map.entry('周', "Z"),
        Map.entry('朱', "Z"), Map.entry('邹', "Z")
    );

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    @FXML
    private Circle avatarCircle;

    @FXML
    private Label avatarText;

    @FXML
    private Button exitButton;

    @FXML
    private Button shiftButton;

    @FXML
    private VBox cartContainer;

    @FXML
    private Label dateLabel;

    @FXML
    private Label timeLabel;

    @FXML
    private Label statusLabel;

    private CashierSystemFXApplication application;
    private User currentUser;
    private CartViewHost cartController;
    private Timeline timeTimeline;

    /**
     * 设置应用程序实例
     */
    public void setApplication(CashierSystemFXApplication application) {
        this.application = application;
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        // 加载用户特定的语言偏好
        String userLanguage = com.cashier.service.DataService.loadLanguagePreference(user.username);
        com.cashier.i18n.I18nManager.getInstance().setLocale(userLanguage);

        // 更新用户信息显示
        userNameLabel.setText(user.name);
        userRoleLabel.setText(user.getRoleDisplayName());

        // 设置头像（显示用户名的首字母）
        if (user.name != null && !user.name.isEmpty()) {
            // 获取首字母
            String firstLetter = getFirstLetter(user.name);
            avatarText.setText(firstLetter);

            avatarCircle.getStyleClass().removeAll(AVATAR_ADMIN_CLASS, AVATAR_FINANCE_CLASS, AVATAR_CASHIER_CLASS);
            if (!avatarCircle.getStyleClass().contains(AVATAR_CIRCLE_CLASS)) {
                avatarCircle.getStyleClass().add(AVATAR_CIRCLE_CLASS);
            }
            avatarCircle.getStyleClass().add(resolveAvatarRoleClass(user.role));
        }

        logger.info("POS模式登录用户: {} ({})", user.name, user.getRoleDisplayName());
    }

    private String resolveAvatarRoleClass(String role) {
        if ("admin".equals(role)) {
            return AVATAR_ADMIN_CLASS;
        }
        if ("finance".equals(role)) {
            return AVATAR_FINANCE_CLASS;
        }
        return AVATAR_CASHIER_CLASS;
    }

    /**
     * 获取姓名的首字母
     * 对于英文：取第一个字母
     * 对于中文：取第一个字的拼音首字母
     */
    private String getFirstLetter(String name) {
        if (name == null || name.isEmpty()) {
            return "U";
        }

        char firstChar = name.charAt(0);
        
        // 如果是英文字母，直接返回
        if ((firstChar >= 'A' && firstChar <= 'Z') || (firstChar >= 'a' && firstChar <= 'z')) {
            return String.valueOf(firstChar).toUpperCase();
        }
        
        // 如果是中文，返回常用汉字的首字母映射
        // 这是一个简化版，实际应该使用完整的拼音库
        String pinyinFirstLetter = getPinyinFirstLetter(firstChar);
        return pinyinFirstLetter;
    }

    /**
     * 获取中文字符的拼音首字母
     * 这是一个简化的映射表，覆盖常见姓氏
     */
    private String getPinyinFirstLetter(char c) {
        return PINYIN_FIRST_LETTER.getOrDefault(c, String.valueOf(c));
    }

    /**
     * 初始化
     */
    @FXML
    private void initialize() {
        // 绑定状态栏
        statusLabel.textProperty().bind(StatusBarManager.statusProperty());
        StatusBarManager.statusLevelProperty().addListener((obs, oldLevel, newLevel) ->
            applyStatusLevelStyle(newLevel)
        );
        applyStatusLevelStyle(StatusBarManager.getStatusLevel());

        // 更新状态
        StatusBarManager.updateStatus("就绪");
        updateDate();

        // 启动时间更新
        startTimeUpdate();

        // 加载收银台
        loadCartView();

        // 设置快捷键
        setupShortcuts();

        logger.info("POS模式控制器初始化完成");
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
     * 加载收银台视图
     */
    private void loadCartView() {
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/TouchCartView.fxml");
            VBox cartView = loader.load();

            // 获取CartController并设置当前用户
            cartController = loader.getController();
            if (cartController != null && currentUser != null) {
                cartController.setCurrentUser(currentUser);
                logger.debug("已将当前用户传递给CartController");
            }

            // 添加到容器
            cartContainer.getChildren().setAll(cartView);
            VBox.setMargin(cartView, new Insets(0));

            StatusBarManager.updateSuccess("收银台已加载");

            // 加载完成后，自动聚焦到搜索框
            Platform.runLater(() -> {
                if (cartController != null) {
                    cartController.focusSearchField();
                }
            });

        } catch (IOException e) {
            logger.error("加载收银台失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    /**
     * 设置快捷键
     */
    private void setupShortcuts() {
        // 等待场景加载完成后设置快捷键
        Platform.runLater(() -> {
            if (avatarCircle.getScene() != null) {
                setupSceneShortcuts(avatarCircle.getScene());
            } else {
                // 如果场景还未加载，监听场景属性
                avatarCircle.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null) {
                        setupSceneShortcuts(newScene);
                    }
                });
            }
        });
    }

    /**
     * 为场景设置快捷键
     */
    private void setupSceneShortcuts(Scene scene) {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            // F6 - 交接班
            if (event.getCode() == KeyCode.F6) {
                handleShift();
                event.consume();
            }
            // F8 - 结账（传递给CartController处理）
            else if (event.getCode() == KeyCode.F8) {
                // 让事件继续传递到CartController
                event.consume();
            }
            // ESC - 退出确认
            else if (event.getCode() == KeyCode.ESCAPE) {
                handleExit();
                event.consume();
            }
        });
    }

    /**
     * 启动时间更新
     */
    private void startTimeUpdate() {
        timeTimeline = new Timeline(new KeyFrame(
            Duration.seconds(1),
            event -> updateTime()
        ));
        timeTimeline.setCycleCount(Timeline.INDEFINITE);
        timeTimeline.play();
    }

    /**
     * 更新时间
     */
    private void updateTime() {
        timeLabel.setText(java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
            .format(com.cashier.util.DateTimeFormats.TIME));
    }

    /**
     * 更新日期
     */
    private void updateDate() {
        dateLabel.setText(java.time.LocalDateTime.now(java.time.ZoneId.systemDefault())
            .format(com.cashier.util.DateTimeFormats.DATE));
    }

    /**
     * 处理交接班
     */
    @FXML
    public void handleShift() {
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/ShiftView.fxml");
            VBox root = loader.load();

            ShiftController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            Stage stage = new Stage();
            stage.setTitle(com.cashier.i18n.I18nManager.getInstance().get("runtime.shift_handover"));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            StatusBarManager.updateSuccess("交接班操作完成");

        } catch (IOException e) {
            logger.error("加载交接班界面失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get(I18nKeys.Error.LOAD_DATA) + ": " + e.getMessage());
        }
    }

    /**
     * 处理退出登录
     */
    @FXML
    public void handleExit() {
        if (cartController != null) {
            // 检查购物车是否为空
            boolean cartEmpty = cartController.isCartEmpty();
            if (!cartEmpty) {
                // 购物车不为空，提示确认
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle(I18nManager.getInstance().get(I18nKeys.Common.CONFIRM));
                alert.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get("runtime.cart_not_empty"));
                String message = com.cashier.i18n.I18nManager.getInstance().get("runtime.cart_exit_confirm");
                StatusBarManager.updateWarning(message);
                alert.setContentText(message);
                alert.showAndWait();
                // 无论选择什么都继续退出（因为收银员可能需要重新开始）
            }
        }

        // 停止时间更新
        if (timeTimeline != null) {
            timeTimeline.stop();
        }

        // 返回登录界面
        if (application != null) {
            application.logoutToLoginView();
        }
    }

    /**
     * 显示错误信息
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
