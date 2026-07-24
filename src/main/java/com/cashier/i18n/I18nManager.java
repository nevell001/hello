package com.cashier.i18n;

import com.cashier.constant.ResourceBundleNames;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.text.MessageFormat;

/**
 * 国际化管理器
 * 支持 ResourceBundle 多语言
 */
public class I18nManager {
    private static final Logger logger = LoggerFactoryUtil.getLogger(I18nManager.class);
    
    private static I18nManager instance;
    private Locale currentLocale;
    private ResourceBundle bundle;
    private final ConcurrentHashMap<String, ResourceBundle> bundles = new ConcurrentHashMap<>();
    private static final Map<String, String> FALLBACK_TEXTS = Map.of(
        "inventory.status.out_of_stock", "Out of Stock",
        I18nKeys.Inventory.Status.LOW_STOCK, "Low Stock",
        I18nKeys.Inventory.Status.NORMAL, "Normal"
    );
    
    // 支持的语言列表
    public static final Locale CHINESE_SIMPLIFIED = Locale.SIMPLIFIED_CHINESE;
    public static final Locale CHINESE_TRADITIONAL = Locale.TRADITIONAL_CHINESE;
    public static final Locale ENGLISH = Locale.ENGLISH;

    // 可用的语言列表
    public static final List<Locale> AVAILABLE_LOCALES = Arrays.asList(
        CHINESE_SIMPLIFIED, CHINESE_TRADITIONAL, ENGLISH
    );

    private I18nManager() {
        // 默认使用简体中文
        setLocaleInternal(CHINESE_SIMPLIFIED);
    }

    /**
     * 获取单例实例
     */
    public static I18nManager getInstance() {
        if (instance == null) {
            instance = new I18nManager();
            // 在实例完全构造后刷新货币格式
            try {
                com.cashier.util.CurrencyUtil.refresh();
            } catch (Exception e) {
                LoggerFactoryUtil.getLogger(I18nManager.class).warn("刷新货币格式失败", e);
            }
        }
        return instance;
    }

    /**
     * 设置当前语言
     */
    public void setLocale(Locale locale) {
        setLocaleInternal(locale);

        // 刷新货币格式
        try {
            com.cashier.util.CurrencyUtil.refresh();
        } catch (Exception e) {
            logger.warn("刷新货币格式失败", e);
        }
    }

    /**
     * 内部设置语言方法（不触发货币刷新）
     */
    private void setLocaleInternal(Locale locale) {
        this.currentLocale = locale;
        this.bundle = getBundle(locale);
        logger.info("语言已切换到: {} ({})", locale.getDisplayLanguage(), locale);
    }
    
    /**
     * 设置当前语言（字符串格式）
     */
    public void setLocale(String languageTag) {
        Locale locale = Locale.forLanguageTag(languageTag);

        // 通过语言标签直接匹配，避免 Locale 对象比较的问题
        String normalizedTag = locale.toLanguageTag();
        boolean supported = false;
        for (Locale supportedLocale : AVAILABLE_LOCALES) {
            if (supportedLocale.toLanguageTag().equals(normalizedTag)) {
                locale = supportedLocale; // 使用预定义的 Locale 常量
                supported = true;
                break;
            }
        }

        if (!supported) {
            logger.warn("语言标签 {} ({}) 不在可用列表中，使用默认简体中文", languageTag, normalizedTag);
            locale = CHINESE_SIMPLIFIED;
        }

        logger.info("设置语言: {} -> Locale: {} (language={}, country={})",
            languageTag, locale, locale.getLanguage(), locale.getCountry());
        setLocale(locale);
    }
    
    /**
     * 获取当前语言
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }
    
    /**
     * 获取当前语言标签
     */
    public String getCurrentLanguageTag() {
        return currentLocale.toLanguageTag();
    }
    
    /**
     * 获取 ResourceBundle
     */
    private ResourceBundle getBundle(Locale locale) {
        return bundles.computeIfAbsent(locale.toLanguageTag(), tag -> {
            try {
                return ResourceBundle.getBundle(ResourceBundleNames.I18N_MESSAGES, locale);
            } catch (MissingResourceException e) {
                logger.warn("找不到语言包: {}, 使用默认", locale);
                return ResourceBundle.getBundle(ResourceBundleNames.I18N_MESSAGES, CHINESE_SIMPLIFIED);
            }
        });
    }

    /**
     * 获取当前 ResourceBundle（用于 FXML 加载）
     */
    public ResourceBundle getResourceBundle() {
        return bundle;
    }
    
    /**
     * 获取翻译文本
     */
    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            String fallbackText = FALLBACK_TEXTS.get(key);
            if (fallbackText != null) {
                return fallbackText;
            }
            logger.warn("找不到翻译: {}", key);
            return key; // 返回 key 作为默认值
        }
    }
    
    /**
     * 获取翻译文本（带参数）
     */
    public String get(String key, Object... params) {
        String template = get(key);
        if (params == null || params.length == 0) {
            return template;
        }
        
        // 使用 MessageFormat 格式化
        return MessageFormat.format(template, params);
    }
    
    /**
     * 判断是否存在翻译
     */
    public boolean has(String key) {
        return bundle.containsKey(key);
    }
    
    /**
     * 获取所有可用的语言
     */
    public List<LocaleInfo> getAvailableLocales() {
        List<LocaleInfo> list = new ArrayList<>();
        
        for (Locale locale : AVAILABLE_LOCALES) {
            LocaleInfo info = new LocaleInfo();
            info.locale = locale;
            info.languageTag = locale.toLanguageTag();
            info.displayName = locale.getDisplayLanguage(locale);
            info.displayNameLocal = locale.getDisplayLanguage(currentLocale);
            info.current = locale.equals(currentLocale);
            list.add(info);
        }
        
        return list;
    }
    
    /**
     * 语言信息
     */
    public static class LocaleInfo {
        public Locale locale;
        public String languageTag;
        public String displayName;      // 该语言的本地名称
        public String displayNameLocal; // 当前语言下的名称
        public boolean current;         // 是否当前语言
        
        @Override
        public String toString() {
            return displayName + " (" + languageTag + ")";
        }
    }
}
