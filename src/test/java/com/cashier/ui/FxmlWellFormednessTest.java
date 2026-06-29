package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlWellFormednessTest {

    @Test
    @DisplayName("所有 FXML 文件必须是结构完整的 XML")
    void allFxmlFilesAreWellFormed() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        List<String> invalidFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Path.of("src/main/resources/com/cashier/view"))) {
            paths.filter(path -> path.toString().endsWith(".fxml")).forEach(path -> {
                try {
                    Document document = factory.newDocumentBuilder().parse(path.toFile());
                    document.getDocumentElement().normalize();
                } catch (Exception e) {
                    invalidFiles.add(path + ": " + e.getMessage());
                }
            });
        }

        assertTrue(invalidFiles.isEmpty(), "FXML 结构错误:\n" + String.join("\n", invalidFiles));
    }
}
