package com.cashier.printer;

import org.slf4j.Logger;
import com.cashier.util.LoggerFactoryUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 打印设备管理器
 * 负责管理所有打印设备的注册、启动、停止和打印任务调度
 */
public class PrinterManager {
    
    private static final Logger logger = LoggerFactoryUtil.getLogger(PrinterManager.class);
    private static final int MAX_PRINT_HISTORY_SIZE = 500;
    
    /**
     * 单例实例
     */
    private static volatile PrinterManager instance;
    
    /**
     * 所有注册的打印设备
     */
    private final Map<String, PrinterDevice> devices;
    
    /**
     * 默认打印机
     */
    private PrinterDevice defaultPrinter;
    
    /**
     * 任务队列
     */
    private final Queue<PrintTask> taskQueue;
    
    /**
     * 打印历史记录
     */
    private final List<PrintTask> printHistory;
    
    /**
     * 私有构造函数
     */
    private PrinterManager() {
        this.devices = new ConcurrentHashMap<>();
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.printHistory = Collections.synchronizedList(new ArrayList<>());
    }
    
    /**
     * 获取单例实例
     * @return PrinterManager 实例
     */
    public static PrinterManager getInstance() {
        if (instance == null) {
            synchronized (PrinterManager.class) {
                if (instance == null) {
                    instance = new PrinterManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册打印设备
     * @param device 打印设备
     */
    public void registerDevice(PrinterDevice device) {
        if (device == null) {
            logger.warn("尝试注册空设备");
            return;
        }
        
        String deviceId = device.getDeviceId();
        if (devices.containsKey(deviceId)) {
            logger.warn("设备已注册: {}", deviceId);
            return;
        }
        
        devices.put(deviceId, device);
        logger.info("注册打印设备: {} ({})", device.getDeviceName(), deviceId);
    }
    
    /**
     * 注销打印设备
     * @param deviceId 设备ID
     */
    public void unregisterDevice(String deviceId) {
        PrinterDevice device = devices.remove(deviceId);
        if (device != null) {
            if (defaultPrinter == device) {
                defaultPrinter = null;
            }
            device.dispose();
            logger.info("注销打印设备: {}", deviceId);
        }
    }
    
    /**
     * 获取设备
     * @param deviceId 设备ID
     * @return 打印设备
     */
    public PrinterDevice getDevice(String deviceId) {
        return devices.get(deviceId);
    }
    
    /**
     * 获取所有设备
     * @return 设备列表
     */
    public List<PrinterDevice> getAllDevices() {
        return new ArrayList<>(devices.values());
    }
    
    /**
     * 获取已连接的设备
     * @return 已连接的设备列表
     */
    public List<PrinterDevice> getConnectedDevices() {
        List<PrinterDevice> connected = new ArrayList<>();
        for (PrinterDevice device : devices.values()) {
            if (device.isConnected()) {
                connected.add(device);
            }
        }
        return connected;
    }
    
    /**
     * 设置默认打印机
     * @param deviceId 设备ID
     */
    public void setDefaultPrinter(String deviceId) {
        PrinterDevice device = getDevice(deviceId);
        if (device != null) {
            defaultPrinter = device;
            logger.info("设置默认打印机: {}", device.getDeviceName());
        } else {
            logger.warn("无法设置默认打印机: {}", deviceId);
        }
    }
    
    /**
     * 获取默认打印机
     * @return 默认的打印设备
     */
    public PrinterDevice getDefaultPrinter() {
        return defaultPrinter;
    }

    /**
     * 按名称查找打印设备（忽略大小写与首尾空白）
     * @param name 设备名称
     * @return 匹配的设备，未找到返回 null
     */
    public PrinterDevice findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String target = name.trim();
        for (PrinterDevice device : devices.values()) {
            String deviceName = device.getDeviceName();
            if (deviceName != null && deviceName.trim().equalsIgnoreCase(target)) {
                return device;
            }
        }
        return null;
    }

    /**
     * 按名称设置默认打印机
     * @param name 设备名称
     * @return 是否设置成功
     */
    public boolean setDefaultPrinterByName(String name) {
        PrinterDevice device = findByName(name);
        if (device == null) {
            logger.warn("未找到名称匹配的打印设备: {}", name);
            return false;
        }
        setDefaultPrinter(device.getDeviceId());
        return true;
    }

    /**
     * 按纸张规格标签（如 "58mm (热敏纸)" / "80mm (热敏纸)"）应用纸张宽度
     * @param paperSizeLabel 设置页纸张大小选项
     */
    public void applyPaperSize(String paperSizeLabel) {
        if (paperSizeLabel == null || paperSizeLabel.trim().isEmpty()) {
            return;
        }
        int widthMM = -1;
        if (paperSizeLabel.contains("58")) {
            widthMM = 58;
        } else if (paperSizeLabel.contains("80")) {
            widthMM = 80;
        }
        if (widthMM <= 0) {
            return;
        }
        for (PrinterDevice device : devices.values()) {
            if (device instanceof NetworkPrinterDevice) {
                ((NetworkPrinterDevice) device).setPaperWidth(widthMM);
            }
        }
        logger.info("已应用纸张宽度: {}mm", widthMM);
    }
    
    /**
     * 启动所有设备
     */
    public void startAllDevices() {
        for (PrinterDevice device : devices.values()) {
            if (device.getStatus() != PrinterDeviceStatus.DISPOSED) {
                device.start();
                logger.info("启动打印设备: {}", device.getDeviceName());
            }
        }
    }
    
    /**
     * 停止所有设备
     */
    public void stopAllDevices() {
        for (PrinterDevice device : devices.values()) {
            if (device.isConnected()) {
                device.stop();
                logger.info("停止打印设备: {}", device.getDeviceName());
            }
        }
    }
    
    /**
     * 打印文本
     * @param text 要打印的文本
     * @return 是否打印成功
     */
    public boolean printText(String text) {
        if (defaultPrinter != null && defaultPrinter.isConnected()) {
            return defaultPrinter.printText(text);
        }
        logger.warn("没有可用的默认打印机");
        return false;
    }
    
    /**
     * 打印任务
     * @param task 打印任务
     * @return 是否打印成功
     */
    public boolean print(PrintTask task) {
        if (task == null) {
            logger.warn("打印任务为空");
            return false;
        }
        
        task.markRunning();

        PrinterDevice printer = selectAvailablePrinter();
        if (printer == null || !printer.isConnected()) {
            String message = "没有可用的打印机";
            task.markFailed(message);
            addToHistory(task);
            logger.warn(message);
            return false;
        }
        
        logger.info("开始打印任务: {} - {}", task.getTaskName(), task.getTaskId());
        
        boolean success = printer.print(task);
        
        if (success) {
            String postProcessFailure = executePostPrintActions(printer, task);
            if (postProcessFailure != null) {
                task.markFailed(postProcessFailure);
                addToHistory(task);
                logger.error("打印任务后处理失败: {} - {}", task.getTaskId(), postProcessFailure);
                return false;
            }

            // 打印后处理
            task.markSuccess();
            addToHistory(task);
            
            logger.info("打印任务完成: {}", task.getTaskId());
        } else {
            task.markFailed("打印设备执行失败: " + printer.getDeviceName());
            addToHistory(task);
            logger.error("打印任务失败: {}", task.getTaskId());
        }
        
        return success;
    }
    
    /**
     * 添加打印任务到队列
     * @param task 打印任务
     */
    public void addPrintTask(PrintTask task) {
        if (task != null) {
            taskQueue.add(task);
            logger.info("添加打印任务到队列: {}", task.getTaskName());
        }
    }
    
    /**
     * 处理队列中的所有打印任务
     */
    public void processQueue() {
        while (true) {
            PrintTask task = taskQueue.poll();
            if (task != null) {
                print(task);
            } else {
                break;
            }
        }
    }
    
    /**
     * 获取打印历史
     * @return 打印历史记录
     */
    public List<PrintTask> getPrintHistory() {
        synchronized (printHistory) {
            return new ArrayList<>(printHistory);
        }
    }

    /**
     * 获取最近打印历史。
     * @param limit 最大返回数量
     * @return 最近打印历史记录
     */
    public List<PrintTask> getRecentPrintHistory(int limit) {
        if (limit < 1) {
            return List.of();
        }

        synchronized (printHistory) {
            int fromIndex = Math.max(0, printHistory.size() - limit);
            List<PrintTask> recent = new ArrayList<>(printHistory.subList(fromIndex, printHistory.size()));
            Collections.reverse(recent);
            return recent;
        }
    }
    
    /**
     * 清空打印历史
     */
    public void clearPrintHistory() {
        synchronized (printHistory) {
            printHistory.clear();
        }
        logger.info("清空打印历史");
    }
    
    /**
     * 打开钱箱
     * @return 是否成功
     */
    public boolean openCashDrawer() {
        PrinterDevice printer = selectAvailablePrinter();
        if (printer != null && printer.isConnected()) {
            return printer.openCashDrawer();
        }
        logger.warn("没有可用的默认打印机");
        return false;
    }
    
    /**
     * 检查所有打印机状态
     * @return 打印机状态列表
     */
    public List<PrinterStatus> checkAllStatus() {
        List<PrinterStatus> statuses = new ArrayList<>();
        for (PrinterDevice device : devices.values()) {
            statuses.add(device.checkStatus());
        }
        return statuses;
    }
    
    /**
     * 检查默认打印机状态
     * @return 打印机状态
     */
    public PrinterStatus checkDefaultStatus() {
        if (defaultPrinter != null) {
            return defaultPrinter.checkStatus();
        }
        return null;
    }
    
    /**
     * 销毁管理器，释放所有资源
     */
    public void dispose() {
        stopAllDevices();
        for (PrinterDevice device : devices.values()) {
            device.dispose();
        }
        devices.clear();
        taskQueue.clear();
        synchronized (printHistory) {
            printHistory.clear();
        }
        defaultPrinter = null;
        logger.info("打印设备管理器已销毁");
    }

    private PrinterDevice selectAvailablePrinter() {
        if (ensurePrinterReady(defaultPrinter)) {
            return defaultPrinter;
        }

        for (PrinterDevice device : devices.values()) {
            if (ensurePrinterReady(device)) {
                return device;
            }
        }

        return null;
    }

    private boolean ensurePrinterReady(PrinterDevice printer) {
        if (printer == null || printer.getStatus() == PrinterDeviceStatus.DISPOSED) {
            return false;
        }

        if (printer.isConnected()) {
            return true;
        }

        boolean started = printer.start();
        if (!started) {
            logger.warn("打印设备启动失败: {} ({})", printer.getDeviceName(), printer.getDeviceId());
        }
        return started && printer.isConnected();
    }

    private String executePostPrintActions(PrinterDevice printer, PrintTask task) {
        if (task.isOpenCashDrawer() && !printer.openCashDrawer()) {
            return "打开钱箱失败";
        }
        if (task.isCutPaper() && !printer.cutPaper()) {
            return "切纸失败";
        }
        return null;
    }

    private void addToHistory(PrintTask task) {
        synchronized (printHistory) {
            printHistory.add(task);
            while (printHistory.size() > MAX_PRINT_HISTORY_SIZE) {
                printHistory.remove(0);
            }
        }
    }
}
