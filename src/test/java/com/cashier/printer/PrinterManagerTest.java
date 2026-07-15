package com.cashier.printer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrinterManagerTest {

    private PrinterManager printerManager;

    @BeforeEach
    void setUp() {
        // Reset singleton for testing
        try {
            java.lang.reflect.Field instanceField = PrinterManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        printerManager = PrinterManager.getInstance();
    }

    @Test
    void testRegisterAndGetDevice() {
        PrinterDevice mockDevice = mock(PrinterDevice.class);
        when(mockDevice.getDeviceId()).thenReturn("test-printer");
        
        printerManager.registerDevice(mockDevice);
        assertEquals(mockDevice, printerManager.getDevice("test-printer"));
        assertEquals(1, printerManager.getAllDevices().size());
        
        printerManager.unregisterDevice("test-printer");
        assertNull(printerManager.getDevice("test-printer"));
    }

    @Test
    void testSetDefaultPrinter() {
        PrinterDevice mockDevice = mock(PrinterDevice.class);
        when(mockDevice.getDeviceId()).thenReturn("test-printer");
        printerManager.registerDevice(mockDevice);
        
        printerManager.setDefaultPrinter("test-printer");
        assertEquals(mockDevice, printerManager.getDefaultPrinter());
    }

    @Test
    void testPrintTaskFlow() {
        PrinterDevice mockDevice = mock(PrinterDevice.class);
        when(mockDevice.getDeviceId()).thenReturn("test-printer");
        when(mockDevice.isConnected()).thenReturn(true);
        when(mockDevice.print(any())).thenReturn(true);
        when(mockDevice.cutPaper()).thenReturn(true);
        
        printerManager.registerDevice(mockDevice);
        printerManager.setDefaultPrinter("test-printer");
        
        PrintTask task = mock(PrintTask.class);
        when(task.getTaskId()).thenReturn("task1");
        when(task.isCutPaper()).thenReturn(true);
        
        boolean success = printerManager.print(task);
        
        assertTrue(success);
        verify(task).markRunning();
        verify(task).markSuccess();
        assertEquals(1, printerManager.getPrintHistory().size());
    }

    @Test
    void testQueueProcessing() {
        PrinterDevice mockDevice = mock(PrinterDevice.class);
        when(mockDevice.getDeviceId()).thenReturn("test-printer");
        when(mockDevice.isConnected()).thenReturn(true);
        when(mockDevice.print(any())).thenReturn(true);
        
        printerManager.registerDevice(mockDevice);
        printerManager.setDefaultPrinter("test-printer");
        
        PrintTask task = mock(PrintTask.class);
        when(task.getTaskId()).thenReturn("task1");
        
        printerManager.addPrintTask(task);
        printerManager.processQueue();
        
        assertEquals(1, printerManager.getPrintHistory().size());
    }

    @Test
    void testPrintHistoryIsBoundedAndRecentFirst() {
        PrinterDevice mockDevice = mock(PrinterDevice.class);
        when(mockDevice.getDeviceId()).thenReturn("test-printer");
        when(mockDevice.isConnected()).thenReturn(true);
        when(mockDevice.print(any())).thenReturn(true);

        printerManager.registerDevice(mockDevice);
        printerManager.setDefaultPrinter("test-printer");

        for (int i = 0; i < 505; i++) {
            PrintTask task = mock(PrintTask.class);
            when(task.getTaskId()).thenReturn("task-" + i);
            printerManager.print(task);
        }

        assertEquals(500, printerManager.getPrintHistory().size());
        List<PrintTask> recent = printerManager.getRecentPrintHistory(2);
        assertEquals("task-504", recent.get(0).getTaskId());
        assertEquals("task-503", recent.get(1).getTaskId());
        assertTrue(printerManager.getRecentPrintHistory(0).isEmpty());
    }
}
