package com.cashier.controller;

import com.cashier.model.User;

/**
 * 收银视图宿主接口
 *
 * <p>让传统 {@code CartController} 与触屏版 {@code TouchCartController} 实现统一契约，
 * 使 {@code PosModeController} 能以统一类型持有当前收银视图，无需感知具体实现。
 * 这样切换 cashier 角色的收银视图时，PosModeController 的退出确认、聚焦、用户注入等
 * 逻辑无需改动。</p>
 */
public interface CartViewHost {

    /** 购物车是否为空（退出登录前用于提示确认） */
    boolean isCartEmpty();

    /** 聚焦到搜索框 */
    void focusSearchField();

    /** 注入当前登录用户（用于结账审计、交接班等） */
    void setCurrentUser(User user);
}
