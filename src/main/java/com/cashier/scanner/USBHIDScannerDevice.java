package com.cashier.scanner;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * USB HID 扫描设备实现
 * 模拟键盘输入的扫描枪
 */
public class USBHIDScannerDevice implements ScannerDevice {
    
    private static final Logger logger = LoggerFactoryUtil.getLogger(USBHIDScannerDevice.class);
    private static final String MAX_SCAN_LENGTH_KEY = "maxScanLength";
    public static final int DEFAULT_MAX_SCAN_LENGTH = 128;
    
    private final String deviceId;
    private final String deviceName;
    private ScannerDeviceStatus status;
    private Map<String, String> configuration;
    private final List<ScanListener> listeners;
    private boolean connected;
    private int maxScanLength = DEFAULT_MAX_SCAN_LENGTH;
    
    public USBHIDScannerDevice(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.status = ScannerDeviceStatus.UNINITIALIZED;
        this.configuration = new HashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.connected = false;
        
        // 默认配置
        configuration.put("baudRate", "9600");
        configuration.put("dataBits", "8");
        configuration.put("stopBits", "1");
        configuration.put("parity", "none");
        configuration.put("autoEnter", "true");
        configuration.put(MAX_SCAN_LENGTH_KEY, String.valueOf(maxScanLength));
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
    public ScannerDeviceType getDeviceType() {
        return ScannerDeviceType.USB_HID;
    }
    
    @Override
    public boolean initialize() {
        logger.info("初始化 USB HID 扫描设备: {}", deviceName);
        status = ScannerDeviceStatus.INITIALIZING;
        
        try {
            // USB HID 设备模拟键盘输入，无需特殊初始化
            status = ScannerDeviceStatus.READY;
            connected = true;
            logger.info("USB HID 扫描设备初始化成功: {}", deviceName);
            return true;
        } catch (Exception e) {
            logger.error("USB HID 扫描设备初始化失败: {}", deviceName, e);
            status = ScannerDeviceStatus.ERROR;
            return false;
        }
    }
    
    @Override
    public boolean start() {
        logger.info("启动 USB HID 扫描设备: {}", deviceName);
        if (status == ScannerDeviceStatus.DISPOSED) {
            logger.warn("扫描设备已销毁，不能启动: {}", deviceName);
            return false;
        }

        if (status == ScannerDeviceStatus.UNINITIALIZED || status == ScannerDeviceStatus.DISCONNECTED) {
            if (!initialize()) {
                return false;
            }
        }

        status = ScannerDeviceStatus.STARTING;
        
        try {
            status = ScannerDeviceStatus.CONNECTED;
            connected = true;
            logger.info("USB HID 扫描设备已启动: {}", deviceName);
            return true;
        } catch (Exception e) {
            logger.error("USB HID 扫描设备启动失败: {}", deviceName, e);
            status = ScannerDeviceStatus.ERROR;
            return false;
        }
    }
    
    @Override
    public boolean stop() {
        logger.info("停止 USB HID 扫描设备: {}", deviceName);
        status = ScannerDeviceStatus.DISCONNECTED;
        connected = false;
        return true;
    }
    
    @Override
    public boolean isConnected() {
        return connected && status == ScannerDeviceStatus.CONNECTED;
    }
    
    @Override
    public ScannerDeviceStatus getStatus() {
        return status;
    }
    
    @Override
    public Map<String, String> getConfiguration() {
        return new HashMap<>(configuration);
    }
    
    @Override
    public void setConfiguration(Map<String, String> config) {
        if (config != null) {
            configuration.putAll(config);
            if (config.containsKey(MAX_SCAN_LENGTH_KEY)) {
                try {
                    int configuredMaxLength = Integer.parseInt(config.get(MAX_SCAN_LENGTH_KEY));
                    if (configuredMaxLength > 0) {
                        maxScanLength = configuredMaxLength;
                    } else {
                        logger.warn("无效的扫码长度限制: {}", config.get(MAX_SCAN_LENGTH_KEY));
                    }
                } catch (NumberFormatException e) {
                    logger.warn("无效的扫码长度限制: {}", config.get(MAX_SCAN_LENGTH_KEY));
                }
            }
        }
    }
    
    @Override
    public void addScanListener(ScanListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeScanListener(ScanListener listener) {
        listeners.remove(listener);
    }
    
    @Override
    public void dispose() {
        logger.info("销毁 USB HID 扫描设备: {}", deviceName);
        stop();
        listeners.clear();
        status = ScannerDeviceStatus.DISPOSED;
        connected = false;
    }
    
    /**
     * 模拟收到扫描数据
     * 此方法供外部调用，模拟扫描枪输入
     * @param data 扫描数据
     */
    public void onScanDataReceived(String data) {
        if (!isConnected()) {
            logger.warn("设备未连接，忽略扫描数据: {}", data);
            return;
        }

        ScanValidation validation = validateScanData(data, maxScanLength);
        if (!validation.isAccepted()) {
            if (ScanValidation.ERROR_EMPTY.equals(validation.getErrorCode())) {
                logger.warn("忽略空扫码数据: {}", data);
            } else if (ScanValidation.ERROR_TOO_LONG.equals(validation.getErrorCode())) {
                String normalizedData = validation.getNormalizedData();
                logger.warn("扫码数据过长，已拒绝: device={}, length={}, max={}",
                    deviceId, normalizedData.length(), maxScanLength);
                notifyListeners(new ScanEvent(normalizedData, deviceId, ScanDataType.BARCODE, false,
                    "扫码数据超过长度限制"));
            }
            return;
        }

        String normalizedData = validation.getNormalizedData();
        logger.debug("收到扫描数据: {}", normalizedData);
        status = ScannerDeviceStatus.SCANNING;
        
        try {
            ScanEvent event = new ScanEvent(normalizedData, deviceId, ScanDataType.BARCODE, true, null);
            notifyListeners(event);
        } finally {
            status = ScannerDeviceStatus.CONNECTED;
        }
    }
    
    /**
     * 通知所有监听器
     * @param event 扫描事件
     */
    private void notifyListeners(ScanEvent event) {
        for (ScanListener listener : listeners) {
            try {
                listener.onScan(event);
            } catch (Exception e) {
                logger.error("扫描监听器执行失败", e);
            }
        }
    }

    static String normalizeScanData(String data) {
        if (data == null) {
            return null;
        }

        String normalized = data
            .replace("\r", "")
            .replace("\n", "")
            .trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * 校验扫码数据：归一化、空数据、长度限制
     * @param rawData 原始扫码数据
     * @param maxScanLength 最大长度（≤0 表示不限制）
     * @return 校验结果
     */
    public static ScanValidation validateScanData(String rawData, int maxScanLength) {
        String normalized = normalizeScanData(rawData);
        if (normalized == null) {
            return ScanValidation.rejected(ScanValidation.ERROR_EMPTY, null);
        }
        if (maxScanLength > 0 && normalized.length() > maxScanLength) {
            return ScanValidation.rejected(ScanValidation.ERROR_TOO_LONG, normalized);
        }
        return ScanValidation.accepted(normalized);
    }

    /**
     * 当前设备的扫码长度上限
     * @return 最大长度
     */
    public int getMaxScanLength() {
        return maxScanLength;
    }
}
