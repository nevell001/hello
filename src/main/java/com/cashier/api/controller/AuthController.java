package com.cashier.api.controller;

import com.cashier.api.ApiServer;
import com.cashier.dao.LoginAttemptDAO;
import com.cashier.dao.UserDAO;
import com.cashier.model.User;
import com.cashier.util.PasswordUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.Map;

/**
 * 认证接口
 */
public class AuthController {
    private static final Logger logger = LoggerFactoryUtil.getLogger(AuthController.class);
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 5;

    /**
     * 登录
     * POST /api/auth/login
     */
    public static void login(Context ctx) {
        try {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            if (request == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "请求体不能为空"));
                return;
            }
            
            if (request.username == null || request.password == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(Map.of("success", false, "message", "用户名和密码不能为空"));
                return;
            }

            // 检查账户锁定（与桌面端登录保持一致策略，持久化到数据库）
            request.username = request.username.trim();
            if (LoginAttemptDAO.isLocked(request.username)) {
                long remainingSeconds = LoginAttemptDAO.getRemainingLockoutSeconds(request.username);
                ctx.status(HttpStatus.TOO_MANY_REQUESTS)
                   .json(Map.of("success", false, "message", "账户已锁定，请 " + remainingSeconds + " 秒后重试"));
                return;
            }
            
            User user = UserDAO.findByUsername(request.username);

            if (user == null || !PasswordUtil.verifyPassword(request.password, user.password)) {
                int attempts = LoginAttemptDAO.recordFailedAttempt(request.username, MAX_LOGIN_ATTEMPTS, LOCKOUT_DURATION_MINUTES * 60 * 1000);
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "用户名或密码错误，剩余尝试次数: " + (MAX_LOGIN_ATTEMPTS - attempts)));
                return;
            }

            if (!user.active) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "用户已被禁用"));
                return;
            }

            // 登录成功，重置失败次数
            LoginAttemptDAO.resetAttempts(request.username);
            
            // 生成 Token
            String token = ApiServer.getInstance().generateToken(user);
            
            // 更新最后登录时间
            UserDAO.updateLastLoginTime(user.id);
            
            user.password = null;
            
            logger.info("用户登录: {}", user.username);
            ctx.json(Map.of(
                "success", true,
                "token", token,
                "user", user,
                "message", "登录成功"
            ));
        } catch (Exception e) {
            logger.error("登录失败", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "登录失败: " + e.getMessage()));
        }
    }
    
    /**
     * 刷新 Token
     * POST /api/auth/refresh
     */
    public static void refresh(Context ctx) {
        String token = ctx.header("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(Map.of("success", false, "message", "缺少认证 Token"));
            return;
        }
        
        token = token.substring(7);
        
        try {
            User user = ApiServer.getInstance().validateToken(token);
            if (user == null) {
                ctx.status(HttpStatus.UNAUTHORIZED)
                   .json(Map.of("success", false, "message", "Token 无效或已过期"));
                return;
            }
            
            // 吊销旧 Token，避免刷新后旧 token 仍有效（泄露的旧 token 失效）
            ApiServer.getInstance().invalidateToken(token);

            String newToken = ApiServer.getInstance().generateToken(user);
            ctx.json(Map.of("success", true, "token", newToken));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("success", false, "message", "Token 刷新失败"));
        }
    }
    
    /**
     * 注销
     * POST /api/auth/logout
     */
    public static void logout(Context ctx) {
        String token = ctx.header("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            ApiServer.getInstance().invalidateToken(token);
        }
        ctx.json(Map.of("success", true, "message", "已注销"));
    }
    
    /**
     * 获取当前用户信息
     * GET /api/auth/me
     */
    public static void getCurrentUser(Context ctx) {
        User user = ctx.attribute("currentUser");
        if (user == null) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(Map.of("success", false, "message", "未登录"));
            return;
        }
        
        user.password = null;
        ctx.json(Map.of("success", true, "user", user));
    }
    
    /**
     * 登录请求
     */
    public static class LoginRequest {
        public String username;
        public String password;
    }
}
