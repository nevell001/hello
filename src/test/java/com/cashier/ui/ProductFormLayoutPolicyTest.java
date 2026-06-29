package com.cashier.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductFormLayoutPolicyTest {

    @Test
    void productFormControlsReserveEnoughHeightForCjkText() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/css/styles.css"));

        assertTrue(css.contains(".product-edit-view .form-input"));
        assertTrue(css.contains("-fx-min-height: 38px;"));
        assertTrue(css.contains(".font-size-large.product-edit-view .form-input"));
        assertTrue(css.contains(".font-size-extra-large.product-edit-view .form-input"));
    }
}
