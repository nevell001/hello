package com.cashier.i18n;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class I18nBundleCompletenessTest {
    private static final String BASE_PATH = "com/cashier/i18n/";

    @Test
    void retainedLanguageBundlesHaveIdenticalKeys() throws IOException {
        Set<String> englishKeys = loadKeys("messages_en.properties");

        assertEquals(englishKeys, loadKeys("messages_zh_CN.properties"));
        assertEquals(englishKeys, loadKeys("messages_zh_TW.properties"));
    }

    @Test
    void onlyThreeLocalesAreSupported() {
        assertEquals(
            Set.of(Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE, Locale.ENGLISH),
            Set.copyOf(I18nManager.AVAILABLE_LOCALES)
        );
    }

    private Set<String> loadKeys(String fileName) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(BASE_PATH + fileName)) {
            if (input == null) {
                throw new IOException("Missing language bundle: " + fileName);
            }
            properties.load(input);
        }
        return properties.stringPropertyNames();
    }
}
