package com.cashier.scanner;

/**
 * 扫码数据校验结果
 */
public final class ScanValidation {

    public static final String ERROR_EMPTY = "empty";
    public static final String ERROR_TOO_LONG = "too_long";

    private final boolean accepted;
    private final String normalizedData;
    private final String errorCode;

    private ScanValidation(boolean accepted, String normalizedData, String errorCode) {
        this.accepted = accepted;
        this.normalizedData = normalizedData;
        this.errorCode = errorCode;
    }

    public static ScanValidation accepted(String normalizedData) {
        return new ScanValidation(true, normalizedData, null);
    }

    public static ScanValidation rejected(String errorCode, String normalizedData) {
        return new ScanValidation(false, normalizedData, errorCode);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getNormalizedData() {
        return normalizedData;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
