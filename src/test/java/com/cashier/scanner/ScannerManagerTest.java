package com.cashier.scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScannerManagerTest {

    private final ScannerManager manager = ScannerManager.getInstance();

    @AfterEach
    void tearDown() {
        manager.dispose();
    }

    @Test
    @DisplayName("USB HID 扫码会清理回车换行并分发给焦点目标")
    void usbHidScanIsNormalizedAndDispatchedToFocusTarget() {
        USBHIDScannerDevice device = new USBHIDScannerDevice("S1", "扫码枪");
        RecordingFocusTarget target = new RecordingFocusTarget("barcode", true, true);
        List<ScanEvent> events = new ArrayList<>();

        manager.registerDevice(device);
        manager.getFocusManager().registerFocusTarget(target);
        manager.addGlobalListener(events::add);
        device.start();

        device.onScanDataReceived("  6901234567890\r\n");

        assertEquals(List.of("6901234567890"), target.scanInputs);
        assertEquals(List.of("6901234567890"), target.scanCompletes);
        assertEquals(1, events.size());
        assertEquals("6901234567890", events.get(0).getData());
        assertTrue(events.get(0).isSuccess());
    }

    @Test
    @DisplayName("空扫码数据会被忽略")
    void blankScanDataIsIgnored() {
        USBHIDScannerDevice device = new USBHIDScannerDevice("S1", "扫码枪");
        List<ScanEvent> events = new ArrayList<>();
        manager.registerDevice(device);
        manager.addGlobalListener(events::add);
        device.start();

        device.onScanDataReceived(" \r\n ");

        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("超长扫码数据会发送失败事件")
    void oversizedScanDataEmitsFailureEvent() {
        USBHIDScannerDevice device = new USBHIDScannerDevice("S1", "扫码枪");
        device.setConfiguration(java.util.Map.of("maxScanLength", "4"));
        List<ScanEvent> events = new ArrayList<>();
        manager.registerDevice(device);
        manager.addGlobalListener(events::add);
        device.start();

        device.onScanDataReceived("12345");

        assertEquals(1, events.size());
        assertFalse(events.get(0).isSuccess());
        assertEquals("扫码数据超过长度限制", events.get(0).getErrorMessage());
    }

    @Test
    @DisplayName("扫码数据校验：归一化 / 空数据 / 超长")
    void scanDataValidationRules() {
        ScanValidation ok = USBHIDScannerDevice.validateScanData("  6901234567890\r\n", 128);
        assertTrue(ok.isAccepted());
        assertEquals("6901234567890", ok.getNormalizedData());

        ScanValidation blank = USBHIDScannerDevice.validateScanData(" \r\n ", 128);
        assertFalse(blank.isAccepted());
        assertEquals(ScanValidation.ERROR_EMPTY, blank.getErrorCode());

        ScanValidation tooLong = USBHIDScannerDevice.validateScanData("12345", 4);
        assertFalse(tooLong.isAccepted());
        assertEquals(ScanValidation.ERROR_TOO_LONG, tooLong.getErrorCode());
        assertEquals("12345", tooLong.getNormalizedData());
    }

    @Test
    @DisplayName("默认扫码长度上限为 128")
    void defaultMaxScanLengthIs128() {
        USBHIDScannerDevice device = new USBHIDScannerDevice("S1", "扫码枪");
        assertEquals(128, device.getMaxScanLength());
        assertEquals(128, USBHIDScannerDevice.DEFAULT_MAX_SCAN_LENGTH);
    }

    @Test
    @DisplayName("全局监听器回调中增删监听器不会影响本次分发")
    void globalListenersAreSafeDuringDispatch() {
        USBHIDScannerDevice device = new USBHIDScannerDevice("S1", "扫码枪");
        AtomicInteger called = new AtomicInteger();
        ScanListener selfRemoving = new ScanListener() {
            @Override
            public void onScan(ScanEvent event) {
                called.incrementAndGet();
                manager.removeGlobalListener(this);
            }
        };

        manager.registerDevice(device);
        manager.addGlobalListener(selfRemoving);
        manager.addGlobalListener(event -> called.incrementAndGet());
        device.start();

        device.onScanDataReceived("ABC");

        assertEquals(2, called.get());
    }

    @Test
    @DisplayName("管理器可先设置活跃设备，再统一启动")
    void activeDeviceCanBeConfiguredBeforeStart() {
        USBHIDScannerDevice device = new USBHIDScannerDevice("S1", "扫码枪");
        manager.registerDevice(device);

        manager.setActiveDevice("S1");
        manager.startAllDevices();

        assertSame(device, manager.getActiveDevice());
        assertTrue(device.isConnected());
    }

    @Test
    @DisplayName("无扫码焦点目标时不会误派发到普通输入目标")
    void scanDispatchRequiresScanTarget() {
        FocusManager focusManager = new FocusManager();
        RecordingFocusTarget keyboardOnly = new RecordingFocusTarget("keyboard", true, false);
        focusManager.registerFocusTarget(keyboardOnly);

        boolean dispatched = focusManager.dispatchScan("690123");

        assertFalse(dispatched);
        assertNull(focusManager.getCurrentTarget());
        assertTrue(keyboardOnly.scanCompletes.isEmpty());
    }

    private static class RecordingFocusTarget implements FocusTarget {
        private final String name;
        private final boolean canReceiveFocus;
        private final boolean scanTarget;
        private final List<String> scanInputs = new ArrayList<>();
        private final List<String> scanCompletes = new ArrayList<>();

        RecordingFocusTarget(String name, boolean canReceiveFocus, boolean scanTarget) {
            this.name = name;
            this.canReceiveFocus = canReceiveFocus;
            this.scanTarget = scanTarget;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void gainFocus() {
        }

        @Override
        public void loseFocus() {
        }

        @Override
        public boolean canReceiveFocus() {
            return canReceiveFocus;
        }

        @Override
        public boolean isScanTarget() {
            return scanTarget;
        }

        @Override
        public void onKeyboardInput(String input) {
        }

        @Override
        public void onScanInput(String input) {
            scanInputs.add(input);
        }

        @Override
        public void onScanComplete(String input) {
            scanCompletes.add(input);
        }
    }
}
