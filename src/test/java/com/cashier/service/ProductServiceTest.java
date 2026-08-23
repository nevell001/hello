package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.exception.BusinessException;
import com.cashier.model.Product;
import com.cashier.model.PageResult;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商品服务测试：CRUD、分页、批量导入与删除异常。
 */
@DisplayName("商品服务测试")
class ProductServiceTest extends DatabaseTestBase {

    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    private final ProductService productService = new ProductService(productDAO);

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    private Product newProduct(String name, double price) {
        Product product = new Product();
        product.productCode = "PS" + name.hashCode();
        product.name = name;
        product.price = BigDecimal.valueOf(price);
        product.quantity = 10;
        product.category = "测试";
        product.barcode = "PSB" + name.hashCode();
        product.unit = "个";
        product.cost = BigDecimal.valueOf(price * 0.7);
        return product;
    }

    @Test
    @DisplayName("创建、查询、分页、更新与删除商品")
    void productCrudAndPaging() {
        Product created = productService.createProduct(newProduct("服务商品A", 20.0));
        assertTrue(created.id > 0);

        Product found = productService.getProductById(created.id);
        assertEquals("服务商品A", found.name);

        productService.createProduct(newProduct("服务商品B", 30.0));

        PageResult<Product> page = productService.getProductsByPage(1, 10);
        assertEquals(2, page.getData().size());

        found.name = "服务商品A改";
        Product updated = productService.updateProduct(found);
        assertEquals("服务商品A改", updated.name);

        productService.deleteProduct(created.id);
        assertThrows(BusinessException.class, () -> productService.deleteProduct(created.id));
        assertEquals(1, productService.getProductCount());
    }

    @Test
    @DisplayName("批量导入商品与空列表")
    void batchImportProducts() {
        assertEquals(0, productService.batchImportProducts(List.of()));
        assertEquals(0, productService.batchImportProducts(null));

        int imported = productService.batchImportProducts(List.of(
            newProduct("批量商品1", 10.0),
            newProduct("批量商品2", 12.0),
            newProduct("批量商品3", 15.0)));
        assertEquals(3, imported);
        assertEquals(3, productService.getProductCount());
        assertNotNull(productService.getProductById(
            productService.getProductsByPage(1, 10).getData().get(0).id));
    }
}
