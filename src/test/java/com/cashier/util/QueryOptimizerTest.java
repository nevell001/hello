package com.cashier.util;

import com.cashier.util.DatabaseTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 查询优化工具测试：分批查询与 EXPLAIN 分析。
 */
@DisplayName("查询优化工具测试")
class QueryOptimizerTest extends DatabaseTestBase {

    @BeforeAll
    static void setup() throws SQLException {
        initTestDatabase();
    }

    @Test
    @DisplayName("batchQuery 按批次大小正确拆分调用")
    void batchQuerySplitsByIds() {
        List<Integer> ids = List.of(1, 2, 3, 4, 5);
        AtomicInteger calls = new AtomicInteger();

        List<String> results = QueryOptimizer.batchQuery(ids, batch -> {
            calls.incrementAndGet();
            return batch.stream().map(String::valueOf).toList();
        }, 2);

        assertEquals(3, calls.get(), "5 个 ID、批次 2 应调用 3 次");
        assertEquals(List.of("1", "2", "3", "4", "5"), results);
    }

    @Test
    @DisplayName("batchQuery 空列表不调用查询函数")
    void batchQueryEmptyList() {
        AtomicInteger calls = new AtomicInteger();
        List<String> results = QueryOptimizer.batchQuery(List.of(), batch -> {
            calls.incrementAndGet();
            return List.of("x");
        });

        assertEquals(0, calls.get());
        assertEquals(List.of(), results);
    }

    @Test
    @DisplayName("batchQuery 某批次失败时跳过并继续后续批次")
    void batchQuerySkipsFailedBatch() {
        List<Integer> ids = List.of(1, 2, 3, 4);
        List<String> results = QueryOptimizer.batchQuery(ids, batch -> {
            if (batch.contains(2)) {
                throw new SQLException("模拟批次失败");
            }
            return batch.stream().map(String::valueOf).toList();
        }, 1);

        assertEquals(List.of("1", "3", "4"), results);
    }

    @Test
    @DisplayName("analyzeQuery 仅接受 SELECT，且能产出 EXPLAIN 结果")
    void analyzeQueryAcceptsSelectOnly() throws SQLException {
        try (Connection conn = getTestConnection()) {
            assertThrows(IllegalArgumentException.class,
                () -> QueryOptimizer.analyzeQuery(conn, "DELETE FROM users"));

            QueryOptimizer.QueryPerformance performance =
                QueryOptimizer.analyzeQuery(conn, "SELECT id, name FROM users");

            assertNotNull(performance);
            assertNotNull(performance.explainResults);
            assertNotNull(performance.getOptimizationSuggestions());
        }
    }
}
