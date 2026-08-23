package com.cashier.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 导出工具测试：验证 Excel/PDF 导出文件可正常生成并可被读取。
 */
@DisplayName("导出工具测试")
class ExportUtilTest {

    private static final Path EXPORT_ROOT = Path.of("exports", "test");

    @AfterAll
    static void cleanup() throws IOException {
        if (Files.exists(EXPORT_ROOT)) {
            try (var walk = Files.walk(EXPORT_ROOT)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    @DisplayName("Excel 导出文件可被 POI 读回且内容完整")
    void excelExportCanBeReadBack() throws IOException {
        String filePath = ExportUtil.export(
            "销售报表", List.of("日期", "金额"),
            List.<String[]>of(new String[]{"2026-08-23", "100.50"}, new String[]{"2026-08-22", "88.00"}),
            ExportUtil.ExportFormat.EXCEL, "test");

        assertNotNull(filePath, "Excel 导出不应失败");
        assertTrue(filePath.endsWith(".xlsx"));
        assertTrue(Files.exists(Path.of(filePath)));

        try (Workbook workbook = WorkbookFactory.create(new File(filePath))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);
            assertEquals("销售报表", workbook.getSheetName(0));
            assertEquals("销售报表", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("日期", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("金额", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("2026-08-23", sheet.getRow(2).getCell(0).getStringCellValue());
            assertEquals("88.00", sheet.getRow(3).getCell(1).getStringCellValue());
        }
    }

    @Test
    @DisplayName("PDF 导出文件可被 PDFBox 读回且包含内容")
    void pdfExportCanBeReadBack() throws IOException {
        String filePath = ExportUtil.export(
            "库存报表", List.of("商品", "数量"),
            List.<String[]>of(new String[]{"可乐", "120"}, new String[]{"雪碧", "80"}),
            ExportUtil.ExportFormat.PDF, "test");

        assertNotNull(filePath, "PDF 导出不应失败");
        assertTrue(filePath.endsWith(".pdf"));
        assertTrue(Files.exists(Path.of(filePath)));

        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            assertTrue(document.getNumberOfPages() >= 1, "PDF 应至少包含一页");
            assertEquals(1, document.getNumberOfPages(), "小数据量导出应为一页");
        }
    }

    @Test
    @DisplayName("非法子目录被拒绝且不生成文件")
    void illegalSubDirIsRejected() {
        String filePath = ExportUtil.export(
            "测试", List.of("列"), List.<String[]>of(new String[]{"值"}),
            ExportUtil.ExportFormat.EXCEL, "../etc");

        assertNull(filePath, "路径穿越的子目录应被拒绝并返回 null");
    }
}
