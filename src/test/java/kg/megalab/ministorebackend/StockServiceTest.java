package kg.megalab.ministorebackend;

import kg.megalab.ministorebackend.dto.OperationType;
import kg.megalab.ministorebackend.exception.InsufficientStockException;
import kg.megalab.ministorebackend.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StockServiceTest {

    @Autowired StockService stock;
    @Autowired JdbcClient jdbc;
    @Autowired TransactionTemplate tx;

    long productId;

    @BeforeEach
    void setUp() {
        jdbc.sql("""
                DELETE FROM inventory_movements
                 WHERE product_id IN (SELECT id FROM products WHERE barcode = 'TEST-STOCK')
                """).update();
        jdbc.sql("DELETE FROM products WHERE barcode = 'TEST-STOCK'").update();
        productId = jdbc.sql("""
                INSERT INTO products (barcode, name, purchase_price, sale_price, stock_quantity, updated_at)
                VALUES ('TEST-STOCK', 'Тест', 100, 150, 5, now()) RETURNING id
                """).query(Long.class).single();
    }

    private int stockOf() {
        return jdbc.sql("SELECT stock_quantity FROM products WHERE id = :id")
                .param("id", productId).query(Integer.class).single();
    }

    private long movementsCount() {
        return jdbc.sql("SELECT COUNT(*) FROM inventory_movements WHERE product_id = :id")
                .param("id", productId).query(Long.class).single();
    }

    @Test
    void delivery_increases_and_sale_decreases() {
        tx.executeWithoutResult(s -> stock.apply(productId, 10, OperationType.DELIVERY, 1L, "Поставка"));
        assertEquals(15, stockOf());
        tx.executeWithoutResult(s -> stock.apply(productId, -4, OperationType.SALE, 1L, "Чек"));
        assertEquals(11, stockOf());
    }

    @Test
    void sale_more_than_stock_is_rejected_and_stock_unchanged() {
        assertThrows(InsufficientStockException.class,
                () -> tx.executeWithoutResult(s ->
                        stock.apply(productId, -6, OperationType.SALE, 2L, "Чек")));
        assertEquals(5, stockOf());
        assertEquals(0, movementsCount());
    }

    @Test
    void movement_stores_reason_date_and_quantity() {
        tx.executeWithoutResult(s -> stock.apply(productId, -2, OperationType.SALE, 7L, "Чек №7"));
        var row = jdbc.sql("""
                SELECT operation_type, quantity_change, quantity_after, reference_id, reason, created_at
                  FROM inventory_movements WHERE product_id = :id
                """).param("id", productId).query().singleRow();
        assertEquals("SALE", row.get("operation_type"));
        assertEquals(-2, ((Number) row.get("quantity_change")).intValue());
        assertEquals(3, ((Number) row.get("quantity_after")).intValue());
        assertEquals(7L, ((Number) row.get("reference_id")).longValue());
        assertEquals("Чек №7", row.get("reason"));
        assertNotNull(row.get("created_at"));
    }

    @Test
    void failed_transaction_rolls_back_stock_change() {
        assertThrows(RuntimeException.class, () -> tx.executeWithoutResult(s -> {
            stock.apply(productId, -2, OperationType.SALE, 3L, "Чек");
            throw new RuntimeException("сбой после списания");
        }));
        assertEquals(5, stockOf());
        assertEquals(0, movementsCount());
    }

    @Test
    void concurrent_sales_never_go_below_zero() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            long saleId = 1000L + i;
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    tx.executeWithoutResult(s ->
                            stock.apply(productId, -1, OperationType.SALE, saleId, "Чек " + saleId));
                    ok.incrementAndGet();
                } catch (InsufficientStockException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(5, ok.get());
        assertEquals(5, rejected.get());
        assertEquals(0, stockOf());
        assertEquals(5, movementsCount());
    }
}