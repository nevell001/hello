package com.cashier.printer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrinterManagerTest {

    private final PrinterManager manager = PrinterManager.getInstance();

    @AfterEach
    void tearDown() {
        manager.dispose();
    }

    @Test
    @DisplayName("默认打印机可以先配置，打印时按需启动")
    void defaultPrinterStartsOnDemand() {
        FakePrinterDevice printer = new FakePrinterDevice("P1", "测试打印机");
        manager.registerDevice(printer);
        manager.setDefaultPrinter("P1");

        PrintTask task = PrintTask.createReceiptTask("hello", false, false);
        boolean success = manager.print(task);

        assertTrue(success);
        assertTrue(printer.started);
        assertEquals(PrintTaskStatus.SUCCESS, task.getStatus());
        assertNotNull(task.getStartedAt());
        assertNotNull(task.getFinishedAt());
        assertNull(task.getErrorMessage());
        assertEquals(List.of(task), manager.getPrintHistory());
    }

    @Test
    @DisplayName("没有可用打印机时任务标记失败并进入历史")
    void unavailablePrinterMarksTaskFailed() {
        PrintTask task = PrintTask.createReceiptTask("hello", false, false);

        boolean success = manager.print(task);

        assertFalse(success);
        assertEquals(PrintTaskStatus.FAILED, task.getStatus());
        assertEquals("没有可用的打印机", task.getErrorMessage());
        assertEquals(List.of(task), manager.getPrintHistory());
    }

    @Test
    @DisplayName("打印后切纸失败会让整个任务失败")
    void cutPaperFailureMarksTaskFailed() {
        FakePrinterDevice printer = new FakePrinterDevice("P1", "测试打印机");
        printer.cutPaperResult = false;
        manager.registerDevice(printer);
        manager.setDefaultPrinter("P1");

        PrintTask task = new PrintTask("T1", "测试", PrintTaskType.TEST, "hello",
            1, false, false, true, false);
        boolean success = manager.print(task);

        assertFalse(success);
        assertEquals(PrintTaskStatus.FAILED, task.getStatus());
        assertEquals("切纸失败", task.getErrorMessage());
        assertEquals(List.of(task), manager.getPrintHistory());
    }

    @Test
    @DisplayName("状态查询返回所有注册设备，不只返回已连接设备")
    void checkAllStatusIncludesDisconnectedDevices() {
        FakePrinterDevice printer = new FakePrinterDevice("P1", "测试打印机");
        manager.registerDevice(printer);

        List<PrinterStatus> statuses = manager.checkAllStatus();

        assertEquals(1, statuses.size());
        assertEquals(PrinterDeviceStatus.DISCONNECTED, statuses.get(0).getStatus());
    }

    private static class FakePrinterDevice implements PrinterDevice {
        private final String deviceId;
        private final String deviceName;
        private boolean connected;
        private boolean started;
        private boolean printResult = true;
        private boolean openCashDrawerResult = true;
        private boolean cutPaperResult = true;
        private PrinterDeviceStatus status = PrinterDeviceStatus.DISCONNECTED;
        private Map<String, String> configuration = new HashMap<>();

        FakePrinterDevice(String deviceId, String deviceName) {
            this.deviceId = deviceId;
            this.deviceName = deviceName;
        }

        @Override
        public String getDeviceId() {
            return deviceId;
        }

        @Override
        public String getDeviceName() {
            return deviceName;
        }

        @Override
        public PrinterDeviceType getDeviceType() {
            return PrinterDeviceType.NETWORK;
        }

        @Override
        public boolean initialize() {
            status = PrinterDeviceStatus.READY;
            return true;
        }

        @Override
        public boolean start() {
            started = true;
            connected = true;
            status = PrinterDeviceStatus.CONNECTED;
            return true;
        }

        @Override
        public boolean stop() {
            connected = false;
            status = PrinterDeviceStatus.DISCONNECTED;
            return true;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public PrinterDeviceStatus getStatus() {
            return status;
        }

        @Override
        public Map<String, String> getConfiguration() {
            return new HashMap<>(configuration);
        }

        @Override
        public void setConfiguration(Map<String, String> config) {
            configuration = new HashMap<>(config);
        }

        @Override
        public boolean printText(String text) {
            return printResult;
        }

        @Override
        public boolean print(PrintTask task) {
            return printResult;
        }

        @Override
        public boolean openCashDrawer() {
            return openCashDrawerResult;
        }

        @Override
        public boolean cutPaper() {
            return cutPaperResult;
        }

        @Override
        public PrinterStatus checkStatus() {
            return new PrinterStatus(deviceId, connected ? PrinterDeviceStatus.CONNECTED : PrinterDeviceStatus.DISCONNECTED);
        }

        @Override
        public void dispose() {
            stop();
            status = PrinterDeviceStatus.DISPOSED;
        }
    }
}
