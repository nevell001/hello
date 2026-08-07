package com.cashier.api.sync;

import com.cashier.api.ApiServer;
import com.cashier.model.User;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsMessageContext;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

/**
 * WebSocket 同步处理器
 * 处理终端连接、断开、消息
 */
public class SyncWebSocketHandler {
    private static final Logger logger = LoggerFactoryUtil.getLogger(SyncWebSocketHandler.class);
    
    /**
     * 处理连接
     */
    public static void onConnect(WsConnectContext ctx) {
        try {
            // Token 放在请求头中，避免出现在 URL、代理日志和浏览器历史记录里。
            String authorization = ctx.header("Authorization");
            String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : null;
            String terminalName = ctx.queryParam("terminal");
            
            if (terminalName == null || terminalName.isEmpty()) {
                terminalName = "终端" + System.currentTimeMillis() % 1000;
            }
            
            // 验证 Token
            User user = null;
            if (token != null && !token.isEmpty()) {
                user = ApiServer.getInstance().validateToken(token);
            }
            
            if (user == null) {
                logger.warn("WebSocket 连接认证失败");
                // 发送错误消息后关闭连接，避免未认证连接被保留
                ctx.send("{\"type\":\"ERROR\",\"data\":{\"message\":\"认证失败\"}}");
                ctx.session.close(4001, "认证失败");
                return;
            }
            
            // 注册终端
            SyncManager.getInstance().registerTerminal(ctx, user, terminalName);
            
            logger.info("WebSocket 连接成功: {} - {}", user.username, terminalName);
            
        } catch (Exception e) {
            logger.error("WebSocket 连接处理失败", e);
        }
    }
    
    /**
     * 处理断开
     */
    public static void onClose(WsCloseContext ctx) {
        try {
            SyncManager.getInstance().unregisterTerminal(ctx);
            logger.info("WebSocket 连接关闭");
        } catch (Exception e) {
            logger.error("WebSocket 断开处理失败", e);
        }
    }
    
    /**
     * 处理消息
     */
    public static void onMessage(WsMessageContext ctx) {
        try {
            String message = ctx.message();
            SyncManager.getInstance().handleMessage(ctx, message);
        } catch (Exception e) {
            logger.error("WebSocket 消息处理失败", e);
        }
    }
    
    /**
     * 处理错误（可选）
     */
    public static void onError(io.javalin.websocket.WsErrorContext ctx) {
        logger.error("WebSocket 错误: {}", ctx.error());
        SyncManager.getInstance().unregisterTerminal(ctx);
    }
}
