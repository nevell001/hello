package com.cashier.service;

import com.cashier.dao.DAOFactory;
import com.cashier.dao.MemberDAORefactored;
import com.cashier.dao.ProductDAORefactored;
import com.cashier.model.CartItem;
import com.cashier.model.Member;
import com.cashier.model.Product;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并发扣减安全测试：
 * 多个线程同时提交交易，验证乐观锁保证库存不超卖、会员余额不超扣。
 */
@DisplayName("并发扣减安全测试")
class ConcurrentDeductionTest extends DatabaseTestBase {

    private static final int THREADS = 2;

    private final ProductDAORefactored productDAO = DAOFactory.getInstance().getProductDAO();
    private final MemberDAORefactored memberDAO = DAOFactory.getInstance().getMemberDAO();

    @BeforeEach
    void setUp() throws Exception {
        if (!DatabaseTestBase.isInitialized()) {
            DatabaseTestBase.initTestDatabase();
        }
        clearTestData();
    }

    @Test
    @DisplayName("并发扣库存不超卖：库存 100，两单各买 60，仅一单成功")
    void concurrentInventoryDeductionNeverOversells() throws Exception {
        Product product = createProduct("并发商品", 10.0, 100);

        int success = runConcurrent(() -> {
            Product own = productDAO.findById(product.id);
            Map<String, Product> inventory = new ConcurrentHashMap<>();
            inventory.put(own.name, own);
            List<CartItem> cart = new ArrayList<>();
            cart.add(new CartItem(own, 60));
            return TransactionService.executeTransaction(
                cart, null, "现金", 600.0, 0.0, inventory).isSuccess();
        });

        assertEquals(1, success, "库存 100 时两单各买 60 应恰好一单成功");
        Product updated = productDAO.findById(product.id);
        assertEquals(40, updated.quantity, "成功一单后库存应为 40");
    }

    @Test
    @DisplayName("并发扣会员余额不超扣：余额 100，两单各用余额 60，仅一单成功")
    void concurrentBalanceDeductionNeverOverdraws() throws Exception {
        Member member = new Member();
        member.phone = "13900139000";
        member.name = "并发会员";
        member.balance = BigDecimal.valueOf(100.0);
        member.points = BigDecimal.ZERO;
        member.level = "普通";
        member.discount = BigDecimal.TEN;
        memberDAO.insert(member);

        Product product = createProduct("余额商品", 10.0, 1000);

        int success = runConcurrent(() -> {
            Member own = memberDAO.findByPhone(member.phone);
            Product ownProduct = productDAO.findById(product.id);
            Map<String, Product> inventory = new ConcurrentHashMap<>();
            inventory.put(ownProduct.name, ownProduct);
            List<CartItem> cart = new ArrayList<>();
            cart.add(new CartItem(ownProduct, 6)); // 6 * 10 = 60
            return TransactionService.executeTransaction(
                cart, own, "会员余额", 60.0, 0.0, inventory).isSuccess();
        });

        assertEquals(1, success, "余额 100 时两单各扣 60 应恰好一单成功");
        Member updated = memberDAO.findByPhone(member.phone);
        assertEquals(0, BigDecimal.valueOf(40.0).compareTo(updated.getBalance()),
            "成功一单后余额应为 40");
    }

    /**
     * 用两个线程同时执行任务，返回成功数量。
     */
    private int runConcurrent(CheckedCallable<Boolean> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch startGate = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return task.call();
                }));
            }
            startGate.countDown();

            int success = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) {
                    success++;
                }
            }
            return success;
        } finally {
            pool.shutdownNow();
        }
    }

    private Product createProduct(String name, double price, int quantity) throws Exception {
        Product product = new Product();
        product.productCode = "CP" + name.hashCode();
        product.name = name;
        product.price = BigDecimal.valueOf(price);
        product.quantity = quantity;
        product.category = "测试分类";
        product.barcode = "CTEST" + name.hashCode();
        product.unit = "个";
        product.minStock = 1;
        product.cost = BigDecimal.valueOf(price).multiply(new BigDecimal("0.7"));
        product.version = 0;

        assertTrue(productDAO.insert(product));
        Product created = productDAO.findByName(name);
        assertTrue(created != null && created.id > 0);
        return created;
    }

    @FunctionalInterface
    private interface CheckedCallable<T> {
        T call() throws Exception;
    }
}
