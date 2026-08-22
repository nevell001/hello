package com.cashier.dao;

import com.cashier.model.Promotion;
import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromotionDAOTest extends DatabaseTestBase {

    private final PromotionDAORefactored promotionDAO = DAOFactory.getInstance().getPromotionDAO();

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @AfterEach
    void cleanup() throws SQLException {
        clearTestData();
    }

    private Promotion createTestPromotion(String name, String type, double threshold, double discount) {
        Promotion p = new Promotion();
        p.promotionCode = "TEST_" + System.nanoTime();
        p.name = name;
        p.type = type;
        p.threshold = BigDecimal.valueOf(threshold);
        p.discount = BigDecimal.valueOf(discount);
        p.description = "测试促销";
        p.enabled = true;
        p.startDate = LocalDateTime.now().minusDays(1);
        p.endDate = LocalDateTime.now().plusDays(1);
        p.usageCount = 0;
        p.maxUsage = -1;
        return p;
    }

    @Test
    void testInsert() throws SQLException {
        Promotion p = createTestPromotion("满100减20", "满减", 100, 20);

        boolean result = promotionDAO.insert(p);
        assertTrue(result);
        assertTrue(p.id > 0);
    }

    @Test
    void testFindById() throws SQLException {
        Promotion p = createTestPromotion("满200减30", "满减", 200, 30);
        promotionDAO.insert(p);

        Promotion found = promotionDAO.findById(p.id);
        assertNotNull(found);
        assertEquals("满200减30", found.name);
        assertEquals("满减", found.type);
        assertEquals(0, BigDecimal.valueOf(200).compareTo(found.threshold));
        assertEquals(0, BigDecimal.valueOf(30).compareTo(found.discount));
    }

    @Test
    void testFindByIdNotFound() throws SQLException {
        Promotion found = promotionDAO.findById(99999);
        assertNull(found);
    }

    @Test
    void testFindAll() throws SQLException {
        promotionDAO.insert(createTestPromotion("促销A", "满减", 100, 10));
        promotionDAO.insert(createTestPromotion("促销B", "打折", 200, 0.9));

        List<Promotion> all = promotionDAO.findAll();
        assertTrue(all.size() >= 2);
    }

    @Test
    void testFindRecentLimitsResults() throws SQLException {
        promotionDAO.insert(createTestPromotion("促销A", "满减", 100, 10));
        promotionDAO.insert(createTestPromotion("促销B", "打折", 200, 0.9));

        List<Promotion> recent = promotionDAO.findRecent(1);

        assertEquals(1, recent.size());
    }

    @Test
    void testFindEnabled() throws SQLException {
        Promotion enabled = createTestPromotion("启用促销", "满减", 100, 10);
        enabled.enabled = true;
        promotionDAO.insert(enabled);

        Promotion disabled = createTestPromotion("禁用促销", "满减", 100, 10);
        disabled.enabled = false;
        promotionDAO.insert(disabled);

        List<Promotion> enabledList = promotionDAO.findEnabled();
        assertTrue(enabledList.stream().allMatch(p -> p.enabled));
    }

    @Test
    void testFindActive() throws SQLException {
        Promotion active = createTestPromotion("进行中促销", "满减", 100, 10);
        active.startDate = LocalDateTime.now().minusDays(1);
        active.endDate = LocalDateTime.now().plusDays(1);
        promotionDAO.insert(active);

        Promotion expired = createTestPromotion("已过期促销", "满减", 100, 10);
        expired.startDate = LocalDateTime.now().minusDays(2);
        expired.endDate = LocalDateTime.now().minusDays(1);
        promotionDAO.insert(expired);

        List<Promotion> activeList = promotionDAO.findActive();
        assertTrue(activeList.size() >= 1);
        assertTrue(activeList.stream().anyMatch(p -> "进行中促销".equals(p.name)));
    }

    @Test
    void testUpdate() throws SQLException {
        Promotion p = createTestPromotion("原名称", "满减", 100, 10);
        promotionDAO.insert(p);

        p.name = "新名称";
        p.threshold = BigDecimal.valueOf(200);
        boolean updated = promotionDAO.update(p);
        assertTrue(updated);

        Promotion found = promotionDAO.findById(p.id);
        assertEquals("新名称", found.name);
        assertEquals(0, BigDecimal.valueOf(200).compareTo(found.threshold));
    }

    @Test
    void testDelete() throws SQLException {
        Promotion p = createTestPromotion("待删除促销", "满减", 100, 10);
        promotionDAO.insert(p);

        boolean deleted = promotionDAO.delete(p.id);
        assertTrue(deleted);

        assertNull(promotionDAO.findById(p.id));
    }

    @Test
    void testIncrementUsage() throws SQLException {
        Promotion p = createTestPromotion("使用次数测试", "满减", 100, 10);
        promotionDAO.insert(p);

        promotionDAO.incrementUsage(p.id);

        Promotion found = promotionDAO.findById(p.id);
        assertEquals(1, found.usageCount);
    }

    @Test
    void testBatchInsert() throws SQLException {
        List<Promotion> promotions = List.of(
            createTestPromotion("批量1", "满减", 100, 10),
            createTestPromotion("批量2", "打折", 200, 0.8),
            createTestPromotion("批量3", "优惠券", 0, 50)
        );

        promotionDAO.batchInsert(promotions);

        List<Promotion> all = promotionDAO.findAll();
        assertTrue(all.size() >= 3);
    }

    @Test
    void testPromotionCalculateDiscount() {
        Promotion p = createTestPromotion("满100减20", "满减", 100, 20);
        p.enabled = true;
        p.startDate = LocalDateTime.now().minusDays(1);
        p.endDate = LocalDateTime.now().plusDays(1);

        // 未达到门槛
        BigDecimal discount1 = p.calculateDiscount(BigDecimal.valueOf(50));
        assertEquals(0, BigDecimal.ZERO.compareTo(discount1));

        // 达到门槛
        BigDecimal discount2 = p.calculateDiscount(BigDecimal.valueOf(100));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(discount2));
    }

    @Test
    void testPromotionMaxUsage() {
        Promotion p = createTestPromotion("限量促销", "满减", 100, 10);
        p.enabled = true;
        p.usageCount = 5;
        p.maxUsage = 5;
        p.startDate = LocalDateTime.now().minusDays(1);
        p.endDate = LocalDateTime.now().plusDays(1);

        BigDecimal discount = p.calculateDiscount(BigDecimal.valueOf(200));
        assertEquals(0, BigDecimal.ZERO.compareTo(discount));
    }
}
