package com.cashier.printer;

/**
 * 打印任务状态
 */
public enum PrintTaskStatus {
    /**
     * 等待打印
     */
    PENDING("等待中"),

    /**
     * 正在打印
     */
    RUNNING("打印中"),

    /**
     * 打印成功
     */
    SUCCESS("成功"),

    /**
     * 打印失败
     */
    FAILED("失败");

    private final String displayName;

    PrintTaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
