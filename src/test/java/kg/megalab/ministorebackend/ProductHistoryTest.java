package kg.megalab.ministorebackend;

import kg.megalab.ministorebackend.dto.OperationType;
import kg.megalab.ministorebackend.dto.StockMovementsPage;
import kg.megalab.ministorebackend.service.StockQueryService;
import kg.megalab.ministorebackend.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProductHistoryTest {

    @Autowired StockService stock;
    @Autowired StockQueryService query;
    @Autowired JdbcClient jdbc;
    @Autowired TransactionTemplate tx;

    long productId;

    @BeforeEach
    void setUp() {
        productId = jdbc.sql("""
                INSERT INTO products (barcode, name, purchase_price, sale_price, stock_quantity, updated_at)
                VALUES (:b, 'История', 100, 150, 0, now()) RETURNING id
                """).param("b", "HIST-" + System.nanoTime()).query(Long.class).single();
        tx.executeWithoutResult(s -> stock.apply(productId, 10, OperationType.DELIVERY, 1L, "Приёмка №1"));
        tx.executeWithoutResult(s -> stock.apply(productId, -3, OperationType.SALE, 2L, "Продажа №2"));
        tx.executeWithoutResult(s -> stock.apply(productId, -1, OperationType.SALE, 3L, "Продажа №3"));
    }

    @Test
    void history_is_sorted_newest_first_with_all_fields() {
        StockMovementsPage page = query.movements(productId, null, 0, 50);

        assertEquals(3, page.total());
        assertEquals(6, page.currentStock());
        assertEquals(3L, page.items().get(0).referenceId());
        assertEquals(1L, page.items().get(2).referenceId());

        var first = page.items().get(0);
        assertEquals("SALE", first.operationType());
        assertEquals("Продажа", first.operationLabel());
        assertEquals(-1, first.quantityChange());
        assertEquals("Продажа №3", first.reason());
        assertNotNull(first.createdAt());
    }

    @Test
    void filter_returns_only_incoming_or_outgoing() {
        assertEquals(1, query.movements(productId, "IN", 0, 50).total());
        assertEquals(2, query.movements(productId, "OUT", 0, 50).total());
    }

    @Test
    void history_cannot_be_deleted_or_edited() {
        assertThrows(Exception.class,
                () -> jdbc.sql("DELETE FROM inventory_movements WHERE product_id = :id")
                        .param("id", productId).update());
        assertThrows(Exception.class,
                () -> jdbc.sql("UPDATE inventory_movements SET quantity_change = 999 WHERE product_id = :id")
                        .param("id", productId).update());
        assertEquals(3, query.movements(productId, null, 0, 50).total());
    }
}
