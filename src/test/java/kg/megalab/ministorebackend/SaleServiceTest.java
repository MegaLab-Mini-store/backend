package kg.megalab.ministorebackend;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import kg.megalab.ministorebackend.dto.CompleteSaleItemRequest;
import kg.megalab.ministorebackend.dto.CompleteSaleRequest;
import kg.megalab.ministorebackend.dto.CompleteSaleResult;
import kg.megalab.ministorebackend.entity.PaymentMethod;
import kg.megalab.ministorebackend.exception.SaleRejectedException;
import kg.megalab.ministorebackend.service.SaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SaleServiceTest {

    @Autowired SaleService saleService;
    @Autowired JdbcClient jdbc;

    Long userId;
    Long breadId;
    Long waterId;

    @BeforeEach
    void setUp() {
        jdbc.sql("""
                DELETE FROM inventory_movements
                 WHERE product_id IN (SELECT id FROM products WHERE barcode LIKE 'TEST-US15-%')
                """).update();
        jdbc.sql("""
                DELETE FROM sale_items
                 WHERE product_id IN (SELECT id FROM products WHERE barcode LIKE 'TEST-US15-%')
                """).update();
        jdbc.sql("DELETE FROM sales WHERE client_uuid LIKE 'test-us15-%'").update();
        jdbc.sql("DELETE FROM products WHERE barcode LIKE 'TEST-US15-%'").update();

        userId = jdbc.sql("""
                INSERT INTO users (username, password_hash, role, created_at)
                VALUES ('test-us15-cashier', 'hash', 'OWNER', now())
                ON CONFLICT (username) DO UPDATE SET username = EXCLUDED.username
                RETURNING id
                """).query(Long.class).single();
        breadId = createProduct("TEST-US15-BREAD", "Хлеб", 10);
        waterId = createProduct("TEST-US15-WATER", "Вода", 1);
    }

    // Критерий 1: продажа создаётся, сумма по цене с телефона
    @Test
    void createsSaleWithPriceFromPhone() {
        CompleteSaleResult result = saleService.completeSale(userId, request("test-us15-0001",
                item(breadId, 2, 4500L),
                item(waterId, 1, 3000L)));

        assertEquals(12000L, result.totalAmount());
        assertEquals(0L, result.earnedBonuses());
        assertEquals(1, countSales("test-us15-0001"));
        assertEquals(4500L, jdbc.sql("""
                SELECT unit_price FROM sale_items WHERE sale_id = :sale AND product_id = :product
                """).param("sale", result.saleId()).param("product", breadId).query(Long.class).single());
        assertEquals("COMPLETED", jdbc.sql("SELECT status FROM sales WHERE id = :id")
                .param("id", result.saleId()).query(String.class).single());
    }

    // Критерий 2: остаток уменьшается, может уйти в минус, движение записано
    @Test
    void decreasesStockAndAllowsNegative() {
        CompleteSaleResult result = saleService.completeSale(userId, request("test-us15-0002",
                item(breadId, 2, 5000L),
                item(waterId, 3, 3000L)));

        assertEquals(8, stock(breadId));
        assertEquals(-2, stock(waterId));
        assertEquals(-2, jdbc.sql("""
                SELECT quantity_change FROM inventory_movements
                 WHERE product_id = :product AND operation_type = 'SALE' AND reference_id = :sale
                """).param("product", breadId).param("sale", result.saleId()).query(Integer.class).single());
    }

    // Один товар двумя строками: одно списание на сумму
    @Test
    void sameProductInTwoLinesIsWrittenOffOnce() {
        saleService.completeSale(userId, request("test-us15-0003",
                item(breadId, 1, 5000L),
                item(breadId, 2, 5000L)));

        assertEquals(7, stock(breadId));
        assertEquals(1, countMovements(breadId));
    }

    // Критерий 3: тот же client_uuid второй раз не создаёт продажу
    @Test
    void sameClientUuidDoesNotCreateSecondSale() {
        CompleteSaleRequest request = request("test-us15-0004", item(breadId, 1, 5000L));
        saleService.completeSale(userId, request);

        assertThrows(DataIntegrityViolationException.class,
                () -> saleService.completeSale(userId, request));

        assertEquals(1, countSales("test-us15-0004"));
        assertEquals(9, stock(breadId));
    }

    // Критерий 4: при ошибке ничего не сохраняется и не списывается
    @Test
    void unknownProductRejectsWholeSale() {
        SaleRejectedException error = assertThrows(SaleRejectedException.class,
                () -> saleService.completeSale(userId, request("test-us15-0005",
                        item(breadId, 1, 5000L),
                        item(999999L, 1, 1000L))));

        assertEquals("PRODUCT_NOT_FOUND", error.getErrorCode());
        assertEquals(0, countSales("test-us15-0005"));
        assertEquals(0, countMovements(breadId));
        assertEquals(10, stock(breadId));
    }

    // Критерий 5: время продажи с телефона сохраняется (по нему продажа попадает в день)
    @Test
    void savesOccurredAtFromPhone() {
        OffsetDateTime occurredAt = OffsetDateTime.of(2026, 11, 11, 10, 30, 0, 0, ZoneOffset.ofHours(6));
        CompleteSaleResult result = saleService.completeSale(userId, new CompleteSaleRequest(
                "test-us15-0006", PaymentMethod.CASH, occurredAt, null,
                List.of(item(breadId, 1, 5000L))));

        OffsetDateTime saved = jdbc.sql("SELECT occurred_at FROM sales WHERE id = :id")
                .param("id", result.saleId()).query(OffsetDateTime.class).single();
        assertTrue(occurredAt.isEqual(saved));
    }

    // Долг без клиента нельзя
    @Test
    void debtWithoutClientIsRejected() {
        SaleRejectedException error = assertThrows(SaleRejectedException.class,
                () -> saleService.completeSale(userId, new CompleteSaleRequest(
                        "test-us15-0007", PaymentMethod.DEBT, null, null,
                        List.of(item(breadId, 1, 5000L)))));

        assertEquals("DEBT_CLIENT_REQUIRED", error.getErrorCode());
        assertEquals(0, countSales("test-us15-0007"));
    }

    private CompleteSaleRequest request(String clientUuid, CompleteSaleItemRequest... items) {
        return new CompleteSaleRequest(clientUuid, PaymentMethod.CASH, null, null, List.of(items));
    }

    private CompleteSaleItemRequest item(Long productId, int quantity, long unitPrice) {
        return new CompleteSaleItemRequest(productId, quantity, unitPrice);
    }

    private Long createProduct(String barcode, String name, int stock) {
        return jdbc.sql("""
                INSERT INTO products (barcode, name, purchase_price, sale_price, stock_quantity, updated_at)
                VALUES (:barcode, :name, 1000, 5000, :stock, now()) RETURNING id
                """).param("barcode", barcode).param("name", name).param("stock", stock)
                .query(Long.class).single();
    }

    private int stock(Long productId) {
        return jdbc.sql("SELECT stock_quantity FROM products WHERE id = :id")
                .param("id", productId).query(Integer.class).single();
    }

    private long countSales(String clientUuid) {
        return jdbc.sql("SELECT count(*) FROM sales WHERE client_uuid = :uuid")
                .param("uuid", clientUuid).query(Long.class).single();
    }

    private long countMovements(Long productId) {
        return jdbc.sql("SELECT count(*) FROM inventory_movements WHERE product_id = :id")
                .param("id", productId).query(Long.class).single();
    }
}
