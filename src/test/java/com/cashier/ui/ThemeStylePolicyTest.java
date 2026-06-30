package com.cashier.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeStylePolicyTest {
    private static final List<String> THEME_COLOR_PROPERTIES = List.of(
        "-fx-background-color", "-fx-text-fill", "-fx-border-color"
    );

    @Test
    @DisplayName("FXML 和控制器不得用内联颜色覆盖主题")
    void viewsAndControllersDoNotOverrideThemeColorsInline() throws IOException {
        Path projectRoot = Path.of(System.getProperty("user.dir"));
        List<String> violations = new ArrayList<>();

        inspectFiles(projectRoot.resolve("src/main/resources/com/cashier/view"), ".fxml", violations);
        inspectFiles(projectRoot.resolve("src/main/java/com/cashier/controller"), ".java", violations);

        assertTrue(violations.isEmpty(),
            "请改用语义 styleClass，并在主题 CSS 中定义颜色：\n" + String.join("\n", violations));
    }

    @Test
    @DisplayName("LiSuan 菜单选中态使用主题主色")
    void lisuanSelectedNavigationUsesBrandPalette() throws IOException {
        String css = Files.readString(Path.of(
            "src/main/resources/css/lisuan-theme.css"
        ));

        assertTrue(css.contains(".sidebar .nav-button-active"));
        assertTrue(css.contains("-fx-background-color: #B85C1B;"));
        assertTrue(css.contains("-fx-text-fill: #FFFFFF;"));
        assertTrue(css.contains(".menu-button:showing"));
        assertTrue(css.contains(".menu-button:pressed"));
        assertTrue(css.contains("-fx-background-color: #FFEAD8;"));
    }

    @Test
    @DisplayName("LiSuan 主题应覆盖快捷键帮助窗口的紫色默认样式")
    void lisuanShortcutHelpUsesBrandPalette() throws IOException {
        String css = Files.readString(Path.of(
            "src/main/resources/css/lisuan-theme.css"
        ));

        assertTrue(css.contains(".shortcut-help-view .header-bar"));
        assertTrue(css.contains(".shortcut-help-view .shortcut-key"));
        assertTrue(css.contains("-fx-background-color: #B85C1B;"));
        assertTrue(css.contains("-fx-text-fill: #8F4314;"));
        assertTrue(css.contains("-fx-background-color: linear-gradient(to bottom, #FFF7EF, #FFEAD8);"));
    }

    private void inspectFiles(Path directory, String suffix, List<String> violations) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(path -> path.toString().endsWith(suffix)).forEach(path -> {
                try {
                    List<String> lines = Files.readAllLines(path);
                    boolean insideSetStyle = false;
                    for (int index = 0; index < lines.size(); index++) {
                        String line = lines.get(index);
                        if (line.contains("setStyle(")) {
                            insideSetStyle = true;
                        }
                        if ((line.contains("style=\"") || insideSetStyle)
                            && THEME_COLOR_PROPERTIES.stream().anyMatch(line::contains)) {
                            violations.add(projectRootRelative(path) + ":" + (index + 1));
                        }
                        if (insideSetStyle && line.contains(");")) {
                            insideSetStyle = false;
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("无法检查主题样式文件: " + path, e);
                }
            });
        }
    }

    private String projectRootRelative(Path path) {
        return Path.of(System.getProperty("user.dir")).relativize(path).toString();
    }
}
