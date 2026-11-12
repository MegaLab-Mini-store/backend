package kg.megalab.ministorebackend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import kg.megalab.ministorebackend.TestcontainersConfiguration;
import kg.megalab.ministorebackend.dto.SaleItemRequest;
import kg.megalab.ministorebackend.dto.SaleRequest;
import kg.megalab.ministorebackend.dto.SaleResult;
import kg.megalab.ministorebackend.entity.PaymentMethod;
import kg.megalab.ministorebackend.exception.SaleRejectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SaleServiceTest {

    @Autowired
    private SaleService saleService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long userId;
    private Long breadId;
    private Long waterId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM inventory_movements");
        jdbc.update("DELETE FROM sale_items");
        jdbc.update("DELETE FROM sales");
        jdbc.update("DELETE FROM products");
        jdbc.update("DELETE FROM refresh_tokens");
        jdbc.update("DELETE FROM users");

        userId = jdbc.queryForObject("""
                INSERT INTO users (username, password_hash, role, created_at)
                VALUES ('cashier', 'hash', 'OWNER', now()) RETURNING id
                """, Long.class);
        breadId = createProduct("Хлеб", 10);
        waterId = createProduct("Вода", 1);
    }

    // Критерий 1: создаётся запись продажи, сумма верная
    @Test
    void createsSaleWithItemsAndTotal() {
        SaleResult result = saleService.completeSale(userId, request("uuid-0001",
                new SaleItemRequest(breadId, 2, 5000L),
                new SaleItemRequest(waterId, 1, 3000L)));

        assertEquals(13000L, result.totalAmount());
        assertEquals(0L, result.earnedBonuses());
        assertEquals(1, count("sales"));
        assertEquals(2, count("sale_items"));
        assertEquals("COMPLETED", jdbc.queryForObject(
                "SELECT status FROM sales WHERE id = ?", String.class, result.saleId()));
    }

    // Критерий 2: остаток уменьшается, может уйти в минус, движение записано
    @Test
    void decreasesStockAndWritesMovement() {
        SaleResult result = saleService.completeSale(userId, request("uuid-0002",
                new SaleItemRequest(breadId, 2, 5000L),
                new SaleItemRequest(waterId, 3, 3000L)));

        assertEquals(8, stock(breadId));
        assertEquals(-2, stock(waterId));
        assertEquals(-2, jdbc.queryForObject("""
                SELECT quantity_change FROM inventory_movements
                WHERE product_id = ? AND operation_type = 'SALE' AND reference_id = ?
                """, Integer.class, breadId, result.saleId()));
    }

    // Критерий 3: тот же client_uuid второй раз не создаёт вторую продажу
    @Test
    void sameClientUuidDoesNotCreateSecondSale() {
        SaleRequest request = request("uuid-0003", new SaleItemRequest(breadId, 1, 5000L));
        saleService.completeSale(userId, request);

        assertThrows(DataIntegrityViolationException.class,
                () -> saleService.completeSale(userId, request));

        assertEquals(1, count("sales"));
        assertEquals(9, stock(breadId));
    }

    // Критерий 4: при ошибке ничего не сохраняется и не списывается
    @Test
    void unknownProductRejectsWholeSale() {
        SaleRejectedException error = assertThrows(SaleRejectedException.class,
                () -> saleService.completeSale(userId, request("uuid-0004",
                        new SaleItemRequest(breadId, 1, 5000L),
                        new SaleItemRequest(999999L, 1, 1000L))));

        assertEquals("PRODUCT_NOT_FOUND", error.getErrorCode());
        assertEquals(0, count("sales"));
        assertEquals(0, count("inventory_movements"));
        assertEquals(10, stock(breadId));
    }

    // Критерий 5: продажа сохраняется со временем с телефона (по нему отчёт за день)
    @Test
    void savesOccurredAtFromPhone() {
        OffsetDateTime occurredAt = OffsetDateTime.of(2026, 11, 11, 10, 30, 0, 0, ZoneOffset.ofHours(6));
        SaleRequest request = new SaleRequest("uuid-0005", PaymentMethod.CASH, occurredAt, null,
                List.of(new SaleItemRequest(breadId, 1, 5000L)));

        SaleResult result = saleService.completeSale(userId, request);

        OffsetDateTime saved = jdbc.queryForObject(
                "SELECT occurred_at FROM sales WHERE id = ?", OffsetDateTime.class, result.saleId());
        assertTrue(occurredAt.isEqual(saved));
    }

    // Долг без клиента нельзя
    @Test
    void debtWithoutClientIsRejected() {
        SaleRequest request = new SaleRequest("uuid-0006", PaymentMethod.DEBT, null, null,
                List.of(new SaleItemRequest(breadId, 1, 5000L)));

        SaleRejectedException error = assertThrows(SaleRejectedException.class,
                () -> saleService.completeSale(userId, request));

        assertEquals("DEBT_CLIENT_REQUIRED", error.getErrorCode());
        assertEquals(0, count("sales"));
    }

    private SaleRequest request(String clientUuid, SaleItemRequest... items) {
        return new SaleRequest(clientUuid, PaymentMethod.CASH, null, null, List.of(items));
    }

    private Long createProduct(String name, int stock) {
        return jdbc.queryForObject("""
                INSERT INTO products (name, purchase_price, sale_price, stock_quantity, updated_at)
                VALUES (?, 1000, 5000, ?, now()) RETURNING id
                """, Long.class, name, stock);
    }

    private int stock(Long productId) {
        return jdbc.queryForObject(
                "SELECT stock_quantity FROM products WHERE id = ?", Integer.class, productId);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
