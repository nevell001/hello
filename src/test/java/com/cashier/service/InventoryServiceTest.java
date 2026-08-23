package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.InventoryStatistics;
import com.cashier.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {

    private Map<String, Product> mockInventory;

    @BeforeEach
    void setUp() {
        mockInventory = new HashMap<>();
        // 使用正确的构造器: Product(name, price, quantity, category, barcode, unit, description)
        Product p1 = new Product("商品A", BigDecimal.valueOf(10.0), 10, "分类", "123", "个", "desc");
        Product p2 = new Product("商品B", BigDecimal.valueOf(20.0), 2, "分类", "456", "个", "desc");
        // 设置 minStock
        p1.minStock = 5;
        p2.minStock = 5;
        
        mockInventory.put(p1.name, p1);
        mockInventory.put(p2.name, p2);
    }

    @Test
    void testSearchProducts() {
        List<Product> result = InventoryService.searchProducts("商品", mockInventory);
        assertEquals(2, result.size());

        result = InventoryService.searchProducts("A", mockInventory);
        assertEquals(1, result.size());
        assertEquals("商品A", result.get(0).name);

        result = InventoryService.searchProducts("nonexistent", mockInventory);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLowStockProducts() {
        List<Product> lowStock = InventoryService.getLowStockProducts(mockInventory);
        assertEquals(1, lowStock.size());
        assertEquals("商品B", lowStock.get(0).name);
    }

    @Test
    void testGetInventoryStatistics() {
        InventoryStatistics stats = InventoryService.getInventoryStatistics(mockInventory);
        assertEquals(2, stats.getTotalProducts());
        assertEquals(12, stats.getTotalQuantity());
        // (10*10 + 2*20) = 100 + 40 = 140.0
        // Product constructor calculates cost as: price * 0.7
        // Product p1 = new Product("商品A", BigDecimal.valueOf(10.0), 10...); cost = 10.0 * 0.7 = 7.0
        // Product p2 = new Product("商品B", BigDecimal.valueOf(20.0), 2...);  cost = 20.0 * 0.7 = 14.0
        // totalValue = (7.0 * 10) + (14.0 * 2) = 70.0 + 28.0 = 98.0
        assertEquals(0, BigDecimal.valueOf(98.0).compareTo(stats.getTotalValue()));
        assertEquals(1, stats.getLowStockCount());
    }
@Test
void testCheckStockAvailable() throws Exception {
    ProductDAORefactored mockDAO = mock(ProductDAORefactored.class);
    InventoryService.setProductDAO(mockDAO);

    Product p = new Product("商品A", BigDecimal.valueOf(10.0), 20, "分类", "123", "个", "desc");
    when(mockDAO.findById(1)).thenReturn(p);

    assertTrue(InventoryService.checkStockAvailable(1, 5));
    assertFalse(InventoryService.checkStockAvailable(1, 25));

    // Reset to original
    InventoryService.setProductDAO(DAOFactory.getInstance().getProductDAO());
}

}
