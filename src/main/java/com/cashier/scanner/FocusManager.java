package com.cashier.scanner;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 焦点管理器
 * 负责智能管理输入焦点，处理键盘输入和扫描输入的冲突
 */
public class FocusManager {
    
    private static final Logger logger = LoggerFactoryUtil.getLogger(FocusManager.class);
    
    /**
     * 注册的焦点目标
     */
    private final List<FocusTarget> focusTargets;
    
    /**
     * 当前活跃的焦点目标
     */
    private FocusTarget currentTarget;
    
    /**
     * 上次输入时间
     */
    private long lastInputTime;
    
    /**
     * 判定为扫描输入的时间间隔（毫秒）
     */
    private static final long SCAN_INPUT_INTERVAL = 50;
    
    /**
     * 判定为键盘输入的时间间隔（毫秒）
     */
    private static final long KEYBOARD_INPUT_INTERVAL = 500;
    
    public FocusManager() {
        this.focusTargets = new CopyOnWriteArrayList<>();
    }
    
    /**
     * 注册焦点目标
     * @param target 焦点目标
     */
    public void registerFocusTarget(FocusTarget target) {
        if (target != null && !focusTargets.contains(target)) {
            focusTargets.add(target);
            logger.debug("注册焦点目标: {}", target.getName());
        }
    }
    
    /**
     * 注销焦点目标
     * @param target 焦点目标
     */
    public void unregisterFocusTarget(FocusTarget target) {
        if (target != null) {
            focusTargets.remove(target);
            if (currentTarget == target) {
                currentTarget = null;
            }
            logger.debug("注销焦点目标: {}", target.getName());
        }
    }
    
    /**
     * 请求焦点
     * @param target 焦点目标
     */
    public void requestFocus(FocusTarget target) {
        if (target == null || !target.canReceiveFocus()) {
            logger.debug("焦点目标不可用: {}", target != null ? target.getName() : "null");
            return;
        }

        if (currentTarget != null && currentTarget != target) {
            safeLoseFocus(currentTarget);
        }
        currentTarget = target;
        safeGainFocus(currentTarget);
        logger.debug("焦点切换到: {}", target.getName());
    }
    
    /**
     * 获取当前焦点目标
     * @return 焦点目标
     */
    public FocusTarget getCurrentTarget() {
        return currentTarget;
    }
    
    /**
     * 处理输入事件
     * @param input 输入内容
     * @param isEnter 是否是回车键
     * @return 输入类型
     */
    public InputType handleInput(String input, boolean isEnter) {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastInputTime;
        
        if ((currentTarget == null || !currentTarget.canReceiveFocus()) && !focusTargets.isEmpty()) {
            // 自动选择第一个可用的焦点目标
            currentTarget = findFirstAvailableTarget(false);
            if (currentTarget != null) {
                safeGainFocus(currentTarget);
            }
        }
        
        if (currentTarget == null) {
            return InputType.UNKNOWN;
        }
        
        // 根据时间间隔判断输入类型
        if (timeDiff <= SCAN_INPUT_INTERVAL) {
            // 快速连续输入，判定为扫描输入
            safeScanInput(currentTarget, input);
            if (isEnter) {
                safeScanComplete(currentTarget, input);
            }
            lastInputTime = currentTime;
            return InputType.SCAN;
        } else if (timeDiff <= KEYBOARD_INPUT_INTERVAL) {
            // 正常速度输入，判定为键盘输入
            safeKeyboardInput(currentTarget, input);
            lastInputTime = currentTime;
            return InputType.KEYBOARD;
        } else {
            // 超过间隔，可能是新的扫描开始
            safeKeyboardInput(currentTarget, input);
            lastInputTime = currentTime;
            return InputType.KEYBOARD; // 暂时判定为键盘，等待下一个字符
        }
    }

    public boolean dispatchScan(String input) {
        String normalizedInput = USBHIDScannerDevice.normalizeScanData(input);
        if (normalizedInput == null) {
            return false;
        }

        FocusTarget target = currentTarget;
        if (target == null || !target.canReceiveFocus() || !target.isScanTarget()) {
            target = findFirstAvailableTarget(true);
            if (target != null) {
                requestFocus(target);
            }
        }

        if (target == null) {
            logger.warn("没有可接收扫码的焦点目标: {}", normalizedInput);
            return false;
        }

        safeScanInput(target, normalizedInput);
        safeScanComplete(target, normalizedInput);
        lastInputTime = System.currentTimeMillis();
        return true;
    }
    
    /**
     * 自动选择扫描焦点目标
     * 当检测到扫描输入时，自动将焦点切换到最适合的目标
     */
    public void autoSelectScanTarget() {
        // 查找优先级最高的扫描目标
        for (FocusTarget target : focusTargets) {
            if (target.isScanTarget() && target.canReceiveFocus()) {
                requestFocus(target);
                return;
            }
        }
    }

    private FocusTarget findFirstAvailableTarget(boolean scanOnly) {
        for (FocusTarget target : focusTargets) {
            if (target.canReceiveFocus() && (!scanOnly || target.isScanTarget())) {
                return target;
            }
        }
        return null;
    }

    private void safeGainFocus(FocusTarget target) {
        try {
            target.gainFocus();
        } catch (Exception e) {
            logger.error("焦点目标获取焦点失败: {}", target.getName(), e);
        }
    }

    private void safeLoseFocus(FocusTarget target) {
        try {
            target.loseFocus();
        } catch (Exception e) {
            logger.error("焦点目标失去焦点失败: {}", target.getName(), e);
        }
    }

    private void safeKeyboardInput(FocusTarget target, String input) {
        try {
            target.onKeyboardInput(input);
        } catch (Exception e) {
            logger.error("键盘输入处理失败: {}", target.getName(), e);
        }
    }

    private void safeScanInput(FocusTarget target, String input) {
        try {
            target.onScanInput(input);
        } catch (Exception e) {
            logger.error("扫码输入处理失败: {}", target.getName(), e);
        }
    }

    private void safeScanComplete(FocusTarget target, String input) {
        try {
            target.onScanComplete(input);
        } catch (Exception e) {
            logger.error("扫码完成处理失败: {}", target.getName(), e);
        }
    }
    
    /**
     * 输入类型枚举
     */
    public enum InputType {
        /**
         * 键盘输入
         */
        KEYBOARD,
        
        /**
         * 扫描输入
         */
        SCAN,
        
        /**
         * 未知类型
         */
        UNKNOWN
    }
}
