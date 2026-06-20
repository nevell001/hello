package com.cashier.controller;

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
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * POS模式控制器
 * 专门为收银员设计的简化界面，只包含收银台和交接班功能
 */
public class PosModeController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(PosModeController.class);

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
    private CartController cartController;
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

            // 根据角色设置头像颜色
            String avatarColor;
            if ("admin".equals(user.role)) {
                avatarColor = "#FFC107"; // 管理员 - 黄色
            } else if ("finance".equals(user.role)) {
                avatarColor = "#9C27B0"; // 财务 - 紫色
            } else {
                avatarColor = "#FFC107"; // 收银员 - 黄色
            }
            avatarCircle.setStyle("-fx-fill: " + avatarColor + "; -fx-stroke: #FFFFFF; -fx-stroke-width: 2;");
        }

        logger.info("POS模式登录用户: {} ({})", user.name, user.getRoleDisplayName());
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
        return switch (c) {
            case '白' -> "B";
            case '蔡', '陈', '程', '崔', '常' -> "C";
            case '戴', '邓', '丁', '董', '杜', '段' -> "D";
            case '范', '方', '冯', '傅' -> "F";
            case '高', '葛', '龚', '郭', '顾' -> "G";
            case '韩', '郝', '何', '贺', '侯', '胡', '黄' -> "H";
            case '贾', '姜', '江', '蒋', '金' -> "J";
            case '康', '孔' -> "K";
            case '赖', '雷', '黎', '李', '梁', '廖', '林', '刘', '龙', '罗', '卢', '陆', '吕' -> "L";
            case '马', '毛', '孟' -> "M";
            case '潘', '彭' -> "P";
            case '钱', '乔', '秦', '邱' -> "Q";
            case '任' -> "R";
            case '沈', '史', '石', '宋', '苏', '孙', '邵' -> "S";
            case '谭', '汤', '唐', '田' -> "T";
            case '万', '汪', '王', '魏', '文', '吴', '武' -> "W";
            case '夏', '萧', '谢', '熊', '徐', '许', '薛' -> "X";
            case '阎', '杨', '姚', '叶', '易', '尹', '于', '余', '袁' -> "Y";
            case '曾', '张', '赵', '郑', '钟', '周', '朱', '邹' -> "Z";
            default -> String.valueOf(c);
        };
    }

    /**
     * 初始化
     */
    @FXML
    private void initialize() {
        // 绑定状态栏
        statusLabel.textProperty().bind(StatusBarManager.statusProperty());

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

    /**
     * 加载收银台视图
     */
    private void loadCartView() {
        try {
            FXMLLoader loader = FXMLUtils.loadFXMLLoader("/com/cashier/view/CartView.fxml");
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

            StatusBarManager.updateStatus("收银台已加载");

            // 加载完成后，自动聚焦到搜索框
            Platform.runLater(() -> {
                if (cartController != null) {
                    cartController.focusSearchField();
                }
            });

        } catch (IOException e) {
            logger.error("加载收银台失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get("error.load_data") + ": " + e.getMessage());
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
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        timeLabel.setText(timeFormat.format(new Date()));
    }

    /**
     * 更新日期
     */
    private void updateDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateLabel.setText(dateFormat.format(new Date()));
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

            StatusBarManager.updateStatus("交接班操作完成");

        } catch (IOException e) {
            logger.error("加载交接班界面失败", e);
            showError(com.cashier.i18n.I18nManager.getInstance().get("error.load_data") + ": " + e.getMessage());
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
                alert.setTitle(I18nManager.getInstance().get("common.confirm"));
                alert.setHeaderText(com.cashier.i18n.I18nManager.getInstance().get("runtime.cart_not_empty"));
                alert.setContentText(com.cashier.i18n.I18nManager.getInstance().get("runtime.cart_exit_confirm"));
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(I18nManager.getInstance().get("label.error"));
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
