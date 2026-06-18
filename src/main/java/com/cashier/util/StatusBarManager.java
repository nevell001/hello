package com.cashier.util;

import com.cashier.i18n.I18nManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Map;

/**
 * 状态栏管理器
 * 提供全局的状态栏更新功能
 */
public class StatusBarManager {

    private static final Map<String, String> LEGACY_STATUS_KEYS = Map.ofEntries(
        Map.entry("就绪", "status.ready"),
        Map.entry("数据已保存", "status_message.data_saved"),
        Map.entry("已刷新", "status_message.refreshed"),
        Map.entry("用户管理", "nav.user_management"),
        Map.entry("数据备份", "menu.data.backup"),
        Map.entry("数据恢复", "menu.data.restore"),
        Map.entry("导出数据", "status_message.export_data"),
        Map.entry("已切换到浅色主题", "status_message.theme_light"),
        Map.entry("已切换到深色主题", "status_message.theme_dark"),
        Map.entry("已切换到 LiSuan 主题", "status_message.theme_lisuan"),
        Map.entry("关于", "menu.help.about"),
        Map.entry("商品管理", "nav.inventory"),
        Map.entry("交易记录", "nav.transactions"),
        Map.entry("会员管理", "nav.members"),
        Map.entry("供应商管理", "nav.supplier"),
        Map.entry("采购订单", "nav.purchase_order"),
        Map.entry("采购审批", "nav.purchase_approval"),
        Map.entry("采购入库", "nav.purchase_inbound"),
        Map.entry("库存盘点", "nav.inventory_check"),
        Map.entry("数据统计", "nav.statistics"),
        Map.entry("库存预警", "status_message.inventory_alert"),
        Map.entry("采购报表", "nav.purchase_report"),
        Map.entry("库存报表", "nav.inventory_report"),
        Map.entry("利润分析", "nav.profit_report"),
        Map.entry("退货报表", "nav.return_report"),
        Map.entry("促销管理", "nav.promotions"),
        Map.entry("交接班", "nav.shift"),
        Map.entry("系统设置", "nav.settings"),
        Map.entry("退货订单", "nav.return_order"),
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
        Map.entry("导出成功", "success.export"),
        Map.entry("已刷新待审批订单", "status_message.approval_orders_refreshed"),
        Map.entry("采购订单更新成功", "status_message.purchase_order_updated"),
        Map.entry("采购订单创建成功", "status_message.purchase_order_created"),
        Map.entry("采购订单删除成功", "status_message.purchase_order_deleted"),
        Map.entry("供应商添加成功", "status_message.supplier_created")
    );

    private static final StringProperty statusProperty =
        new SimpleStringProperty(I18nManager.getInstance().get("status.ready"));

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

    /**
     * 获取当前状态
     * @return 当前状态文本
     */
    public static String getStatus() {
        return statusProperty.get();
    }

    /**
     * 更新状态栏
     * @param status 状态文本
     */
    public static void updateStatus(String status) {
        String localizedStatus = localizeStatus(status);
        Platform.runLater(() -> statusProperty.set(localizedStatus));
    }

    /**
     * 清除状态栏（恢复默认状态）
     */
    public static void clearStatus() {
        updateStatus(I18nManager.getInstance().get("status.ready"));
    }

    private static String localizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }

        String key = LEGACY_STATUS_KEYS.get(status);
        if (key != null) {
            return I18nManager.getInstance().get(key);
        }

        String localized = localizePrefixedStatus(status, "无法刷新: ", "status_message.refresh_failed");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "已刷新: ", "status_message.refreshed_item");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "商品删除成功: ", "status_message.product_deleted");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "商品添加成功: ", "status_message.product_created");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "商品更新成功: ", "status_message.product_updated");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "供应商添加成功: ", "status_message.supplier_created_named");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "供应商更新成功: ", "status_message.supplier_updated");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "供应商删除成功: ", "status_message.supplier_deleted");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "入库成功: ", "status_message.inbound_success");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "盘点完成: ", "status_message.inventory_check_completed");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "订单已提交审批: ", "status_message.order_submitted");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "会员添加成功: ", "status_message.member_created");
        if (localized != null) return localized;
        localized = localizePrefixedStatus(status, "会员更新成功: ", "status_message.member_updated");
        if (localized != null) return localized;

        if (status.startsWith("快速入库成功: ")) {
            return I18nManager.getInstance().get("status_message.quick_restock",
                status.substring("快速入库成功: ".length()));
        }
        if (status.startsWith("订单通过: ")) {
            return I18nManager.getInstance().get("status_message.order_approved",
                status.substring("订单通过: ".length()));
        }
        if (status.startsWith("订单拒绝: ")) {
            return I18nManager.getInstance().get("status_message.order_rejected",
                status.substring("订单拒绝: ".length()));
        }

        return status;
    }

    private static String localizePrefixedStatus(String status, String prefix, String key) {
        if (!status.startsWith(prefix)) {
            return null;
        }
        return I18nManager.getInstance().get(key, status.substring(prefix.length()));
    }
}
