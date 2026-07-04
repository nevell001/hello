package com.cashier.util;

import com.cashier.i18n.I18nKeys;

import com.cashier.i18n.I18nManager;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;

import java.util.Map;

/**
 * 状态栏管理器
 * 提供全局的状态栏更新功能
 */
public class StatusBarManager {

    public enum StatusLevel {
        NORMAL,
        SUCCESS,
        WARNING,
        ERROR
    }

    private static final Map<String, String> LEGACY_STATUS_KEYS = Map.ofEntries(
        Map.entry("就绪", I18nKeys.Status.READY),
        Map.entry("数据已保存", "status_message.data_saved"),
        Map.entry("已刷新", "status_message.refreshed"),
        Map.entry("用户管理", I18nKeys.Nav.USER_MANAGEMENT),
        Map.entry("数据备份", "menu.data.backup"),
        Map.entry("数据恢复", "menu.data.restore"),
        Map.entry("导出数据", "status_message.export_data"),
        Map.entry("已切换到浅色主题", "status_message.theme_light"),
        Map.entry("已切换到深色主题", "status_message.theme_dark"),
        Map.entry("已切换到 LiSuan 主题", "status_message.theme_lisuan"),
        Map.entry("关于", I18nKeys.Menu.Help.ABOUT),
        Map.entry("商品管理", I18nKeys.Nav.INVENTORY),
        Map.entry("交易记录", I18nKeys.Nav.TRANSACTIONS),
        Map.entry("会员管理", I18nKeys.Nav.MEMBERS),
        Map.entry("供应商管理", I18nKeys.Nav.SUPPLIER),
        Map.entry("采购订单", I18nKeys.Nav.PURCHASE_ORDER),
        Map.entry("采购审批", I18nKeys.Nav.PURCHASE_APPROVAL),
        Map.entry("采购入库", I18nKeys.Nav.PURCHASE_INBOUND),
        Map.entry("库存盘点", I18nKeys.Nav.INVENTORY_CHECK),
        Map.entry("数据统计", I18nKeys.Nav.STATISTICS),
        Map.entry("库存预警", "status_message.inventory_alert"),
        Map.entry("采购报表", I18nKeys.Nav.PURCHASE_REPORT),
        Map.entry("库存报表", I18nKeys.Nav.INVENTORY_REPORT),
        Map.entry("利润分析", I18nKeys.Nav.PROFIT_REPORT),
        Map.entry("退货报表", "nav.return_report"),
        Map.entry("促销管理", I18nKeys.Nav.PROMOTIONS),
        Map.entry("交接班", I18nKeys.Nav.SHIFT),
        Map.entry("系统设置", I18nKeys.Nav.SETTINGS),
        Map.entry("退货订单", I18nKeys.Nav.RETURN_ORDER),
        Map.entry("退货审批", "nav.return_approval"),
        Map.entry("无需刷新", "status_message.no_refresh_needed"),
        Map.entry("盘点单更新成功", "status_message.inventory_check_updated"),
        Map.entry("盘点单创建成功", "status_message.inventory_check_created"),
        Map.entry("盘点单删除成功", "status_message.inventory_check_deleted"),
        Map.entry("商品列表已刷新", "status_message.product_list_refreshed"),
        Map.entry("商品添加成功", "status_message.product_created_plain"),
        Map.entry("商品更新成功", "status_message.product_updated_plain"),
        Map.entry("已刷新可入库订单", "status_message.inbound_orders_refreshed"),
        Map.entry("收银台已加载", "status_message.pos_loaded"),
        Map.entry("交接班操作完成", "status_message.shift_completed"),
        Map.entry("导出成功", I18nKeys.Success.EXPORT),
        Map.entry("已刷新待审批订单", "status_message.approval_orders_refreshed"),
        Map.entry("采购订单更新成功", "status_message.purchase_order_updated"),
        Map.entry("采购订单创建成功", "status_message.purchase_order_created"),
        Map.entry("采购订单删除成功", "status_message.purchase_order_deleted"),
        Map.entry("供应商添加成功", "status_message.supplier_created")
    );
    private static final Map<String, String> PREFIXED_STATUS_KEYS = Map.ofEntries(
        Map.entry("无法刷新: ", "status_message.refresh_failed"),
        Map.entry("已刷新: ", "status_message.refreshed_item"),
        Map.entry("商品删除成功: ", "status_message.product_deleted"),
        Map.entry("商品添加成功: ", "status_message.product_created"),
        Map.entry("商品更新成功: ", "status_message.product_updated"),
        Map.entry("供应商添加成功: ", "status_message.supplier_created_named"),
        Map.entry("供应商更新成功: ", "status_message.supplier_updated"),
        Map.entry("供应商删除成功: ", "status_message.supplier_deleted"),
        Map.entry("入库成功: ", "status_message.inbound_success"),
        Map.entry("盘点完成: ", "status_message.inventory_check_completed"),
        Map.entry("订单已提交审批: ", "status_message.order_submitted"),
        Map.entry("会员添加成功: ", "status_message.member_created"),
        Map.entry("会员更新成功: ", "status_message.member_updated"),
        Map.entry("快速入库成功: ", "status_message.quick_restock"),
        Map.entry("订单通过: ", "status_message.order_approved"),
        Map.entry("订单拒绝: ", "status_message.order_rejected")
    );

    private static final StringProperty statusProperty =
        new SimpleStringProperty(I18nManager.getInstance().get(I18nKeys.Status.READY));
    private static final ObjectProperty<StatusLevel> statusLevelProperty =
        new SimpleObjectProperty<>(StatusLevel.NORMAL);

    private StatusBarManager() {
        // 私有构造函数，防止实例化
    }

    /**
     * 获取状态属性，用于绑定到 UI
     * @return 状态属性
     */
    public static StringProperty statusProperty() {
        return statusProperty;
    }

    public static ObjectProperty<StatusLevel> statusLevelProperty() {
        return statusLevelProperty;
    }

    /**
     * 获取当前状态
     * @return 当前状态文本
     */
    public static String getStatus() {
        return statusProperty.get();
    }

    public static StatusLevel getStatusLevel() {
        return statusLevelProperty.get();
    }

    /**
     * 更新状态栏
     * @param status 状态文本
     */
    public static void updateStatus(String status) {
        updateStatus(status, inferStatusLevel(status));
    }

    public static void updateSuccess(String status) {
        updateStatus(status, StatusLevel.SUCCESS);
    }

    public static void updateWarning(String status) {
        updateStatus(status, StatusLevel.WARNING);
    }

    public static void updateError(String status) {
        updateStatus(status, StatusLevel.ERROR);
    }

    public static void updateStatus(String status, StatusLevel level) {
        String localizedStatus = localizeStatus(status);
        StatusLevel nextLevel = level != null ? level : StatusLevel.NORMAL;
        Platform.runLater(() -> {
            statusProperty.set(localizedStatus);
            statusLevelProperty.set(nextLevel);
        });
    }

    /**
     * 清除状态栏（恢复默认状态）
     */
    public static void clearStatus() {
        updateStatus(I18nManager.getInstance().get(I18nKeys.Status.READY), StatusLevel.NORMAL);
    }

    private static String localizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        String key = LEGACY_STATUS_KEYS.get(status);
        if (key != null) {
            return I18nManager.getInstance().get(key);
        }

        for (Map.Entry<String, String> entry : PREFIXED_STATUS_KEYS.entrySet()) {
            String localized = localizePrefixedStatus(status, entry.getKey(), entry.getValue());
            if (localized != null) {
                return localized;
            }
        }

        return status;
    }

    private static String localizePrefixedStatus(String status, String prefix, String key) {
        if (!status.startsWith(prefix)) {
            return null;
        }
        return I18nManager.getInstance().get(key, status.substring(prefix.length()));
    }

    private static StatusLevel inferStatusLevel(String status) {
        if (status == null || status.isBlank()) {
            return StatusLevel.NORMAL;
        }

        String normalized = status.toLowerCase();
        if (containsAny(normalized,
                "失败", "错误", "无法", "异常", "不足", "未找到", "拒绝", "无权限",
                "failed", "failure", "error", "invalid", "unavailable", "not found", "denied")) {
            return StatusLevel.ERROR;
        }

        if (containsAny(normalized,
                "警告", "请选择", "需要", "重复", "冲突", "待处理",
                "warning", "select", "required", "duplicate", "conflict", "pending")) {
            return StatusLevel.WARNING;
        }

        if (containsAny(normalized,
                "成功", "完成", "已保存", "已刷新", "已加载",
                "success", "completed", "saved", "refreshed", "loaded")) {
            return StatusLevel.SUCCESS;
        }

        return StatusLevel.NORMAL;
    }

    private static boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
