package com.cashier.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Shared date/time format definitions used by persistence, reports and printing.
 */
public final class DateTimeFormats {
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String COMPACT_DATE_PATTERN = "yyyyMMdd";
    public static final String COMPACT_DATE_TIME_PATTERN = "yyyyMMddHHmmss";
    public static final String COMPACT_DATE_TIME_MILLIS_PATTERN = "yyyyMMddHHmmssSSS";
    public static final String STANDARD_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String TIME_HOUR_MINUTE_PATTERN = "HH:mm";
    public static final String STANDARD_DATE_TIME_MINUTE_PATTERN = "yyyy-MM-dd HH:mm";
    public static final String FULL_DATE_PATTERN = "yyyy-MM-dd EEEE";
    public static final String MONTH_PATTERN = "yyyy-MM";
    public static final String BACKUP_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss";
    public static final String SHIFT_TIME_PATTERN = "MM-dd HH:mm";
    public static final String BIRTHDAY_PATTERN = "MM-dd";
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(DATE_PATTERN);
    public static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.ofPattern(COMPACT_DATE_PATTERN);
    public static final DateTimeFormatter BIRTHDAY = DateTimeFormatter.ofPattern(BIRTHDAY_PATTERN);
    public static final DateTimeFormatter COMPACT_DATE_TIME = DateTimeFormatter.ofPattern(COMPACT_DATE_TIME_PATTERN);
    public static final DateTimeFormatter COMPACT_DATE_TIME_MILLIS = DateTimeFormatter.ofPattern(COMPACT_DATE_TIME_MILLIS_PATTERN);
    public static final DateTimeFormatter STANDARD_DATE_TIME = DateTimeFormatter.ofPattern(STANDARD_DATE_TIME_PATTERN);
    public static final DateTimeFormatter STANDARD_DATE_TIME_MINUTE = DateTimeFormatter.ofPattern(STANDARD_DATE_TIME_MINUTE_PATTERN);
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern(TIME_PATTERN);
    public static final DateTimeFormatter TIME_HOUR_MINUTE = DateTimeFormatter.ofPattern(TIME_HOUR_MINUTE_PATTERN);
    public static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern(FULL_DATE_PATTERN);
    public static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern(MONTH_PATTERN);
    public static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern(BACKUP_TIMESTAMP_PATTERN);
    public static final DateTimeFormatter SHIFT_TIME = DateTimeFormatter.ofPattern(SHIFT_TIME_PATTERN);

    private DateTimeFormats() {
    }

    // Utility helpers using java.time for safer usage
    public static String formatStandard(LocalDateTime dt) {
        // L-6: null 安全检查
        if (dt == null) {
            return "";
        }
        return dt.format(STANDARD_DATE_TIME);
    }

    public static String formatDate(LocalDate date) {
        // L-6: null 安全检查
        if (date == null) {
            return "";
        }
        return date.format(DATE);
    }

    public static LocalDateTime parseStandard(String text) {
        return LocalDateTime.parse(text, STANDARD_DATE_TIME);
    }
}
