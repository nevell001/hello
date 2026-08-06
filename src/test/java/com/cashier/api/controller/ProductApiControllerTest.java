package com.cashier.api.controller;

import com.cashier.api.support.TestContext;
import com.cashier.dao.DAOFactory;
import com.cashier.model.Product;
import com.cashier.util.DatabaseTestBase;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductApiControllerTest extends DatabaseTestBase {

    private Product insertProduct(String name) throws Exception {
        Product product = new Product();
        product.productCode = "P-" + name;
        product.name = name;
        product.price = BigDecimal.valueOf(9.9);
        product.quantity = 20;
        product.category = "测试分类";
        product.unit = "个";
        assertTrue(DAOFactory.getInstance().getProductDAO().insert(product));
        return product;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> response(TestContext ctx) {
        return (Map<String, Object>) ctx.json;
    }

    @Test
    @DisplayName("商品列表：无条件返回全部")
    void listProductsWithoutFilters() throws Exception {
        insertProduct("列表商品A");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products");
        ProductApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertNotNull(response(ctx).get("data"));
    }

    @Test
    @DisplayName("商品列表：关键词搜索")
    void listProductsByKeyword() throws Exception {
        insertProduct("搜索目标商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products")
            .withQueryParam("keyword", "搜索目标");
        ProductApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("商品列表：按分类过滤")
    void listProductsByCategory() throws Exception {
        insertProduct("分类商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products")
            .withQueryParam("category", "测试分类");
        ProductApiController.list(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("商品详情：返回已保存商品")
    void getProductReturnsData() throws Exception {
        Product saved = insertProduct("详情商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products/1")
            .withPathParam("id", String.valueOf(saved.id));
        ProductApiController.get(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        Product data = (Product) response(ctx).get("data");
        assertEquals(saved.id, data.id);
    }

    @Test
    @DisplayName("商品详情：不存在返回 404")
    void getMissingProductReturns404() {
        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products/999999")
            .withPathParam("id", "999999");
        ProductApiController.get(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
        assertFalse((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("创建商品返回 201")
    void createProductReturnsCreated() {
        ProductApiController.ProductRequest request = new ProductApiController.ProductRequest();
        request.productCode = "P-NEW-001";
        request.name = "新建商品";
        request.price = BigDecimal.valueOf(5);

        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/products").withBody(request);
        ProductApiController.create(ctx.context);

        assertEquals(HttpStatus.CREATED, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("更新商品并持久化")
    void updateProductAppliesChanges() throws Exception {
        Product saved = insertProduct("待更新商品");
        ProductApiController.ProductRequest request = new ProductApiController.ProductRequest();
        request.name = "更新后的商品";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/products/1")
            .withPathParam("id", String.valueOf(saved.id))
            .withBody(request);
        ProductApiController.update(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertEquals("更新后的商品", DAOFactory.getInstance().getProductDAO().findById(saved.id).name);
    }

    @Test
    @DisplayName("更新不存在的商品返回 404")
    void updateMissingProductReturns404() {
        ProductApiController.ProductRequest request = new ProductApiController.ProductRequest();
        request.name = "x";

        TestContext ctx = new TestContext().withRequest(HandlerType.PUT, "/api/products/999999")
            .withPathParam("id", "999999")
            .withBody(request);
        ProductApiController.update(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("删除商品后记录不存在")
    void deleteProductRemovesIt() throws Exception {
        Product saved = insertProduct("待删除商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.DELETE, "/api/products/1")
            .withPathParam("id", String.valueOf(saved.id));
        ProductApiController.delete(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
        assertNull(DAOFactory.getInstance().getProductDAO().findById(saved.id));
    }

    @Test
    @DisplayName("删除不存在的商品返回 404")
    void deleteMissingProductReturns404() {
        TestContext ctx = new TestContext().withRequest(HandlerType.DELETE, "/api/products/999999")
            .withPathParam("id", "999999");
        ProductApiController.delete(ctx.context);

        assertEquals(HttpStatus.NOT_FOUND, ctx.status);
    }

    @Test
    @DisplayName("低库存列表返回成功")
    void lowStockReturnsSummary() throws Exception {
        insertProduct("低库存商品");

        TestContext ctx = new TestContext().withRequest(HandlerType.GET, "/api/products/low-stock");
        ProductApiController.lowStock(ctx.context);

        assertEquals(HttpStatus.OK, ctx.status);
        assertTrue((Boolean) response(ctx).get("success"));
    }

    @Test
    @DisplayName("空请求体创建商品返回 400")
    void createWithNullBodyReturns400() {
        TestContext ctx = new TestContext().withRequest(HandlerType.POST, "/api/products");
        ProductApiController.create(ctx.context);

        assertEquals(HttpStatus.BAD_REQUEST, ctx.status);
        assertFalse((Boolean) response(ctx).get("success"));
    }
}
