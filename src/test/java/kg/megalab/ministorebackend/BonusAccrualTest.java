package kg.megalab.ministorebackend;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import kg.megalab.ministorebackend.dto.CompleteSaleItemRequest;
import kg.megalab.ministorebackend.dto.CompleteSaleRequest;
import kg.megalab.ministorebackend.dto.CompleteSaleResult;
import kg.megalab.ministorebackend.dto.SyncOperationRequest;
import kg.megalab.ministorebackend.dto.SyncOperationResponse;
import kg.megalab.ministorebackend.dto.SyncPushRequest;
import kg.megalab.ministorebackend.dto.SyncStatus;
import kg.megalab.ministorebackend.entity.PaymentMethod;
import kg.megalab.ministorebackend.exception.SaleRejectedException;
import kg.megalab.ministorebackend.service.SaleService;
import kg.megalab.ministorebackend.service.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BonusAccrualTest {

    @Autowired SaleService saleService;
    @Autowired SyncService syncService;
    @Autowired JdbcClient jdbc;

    Long userId;
    Long breadId;

    @BeforeEach
    void setUp() {
        jdbc.sql("""
                DELETE FROM bonus_transactions
                 WHERE client_id IN (SELECT id FROM clients WHERE client_code LIKE 'TEST-US27-%')
                """).update();
        jdbc.sql("""
                DELETE FROM inventory_movements
                 WHERE product_id IN (SELECT id FROM products WHERE barcode LIKE 'TEST-US27-%')
                """).update();
        jdbc.sql("""
                DELETE FROM sale_items
                 WHERE product_id IN (SELECT id FROM products WHERE barcode LIKE 'TEST-US27-%')
                """).update();
        jdbc.sql("DELETE FROM sales WHERE client_uuid LIKE 'test-us27-%'").update();
        jdbc.sql("DELETE FROM sync_operations WHERE client_uuid LIKE 'test-us27-%'").update();
        jdbc.sql("DELETE FROM clients WHERE client_code LIKE 'TEST-US27-%'").update();
        jdbc.sql("DELETE FROM loyalty_levels WHERE name LIKE 'test-us27-%'").update();
        jdbc.sql("DELETE FROM products WHERE barcode LIKE 'TEST-US27-%'").update();

        userId = jdbc.sql("""
                INSERT INTO users (username, password_hash, role, created_at)
                VALUES ('test-us27-cashier', 'hash', 'OWNER', now())
                ON CONFLICT (username) DO UPDATE SET username = EXCLUDED.username
                RETURNING id
                """).query(Long.class).single();
        breadId = jdbc.sql("""
                INSERT INTO products (barcode, name, purchase_price, sale_price, stock_quantity, updated_at)
                VALUES ('TEST-US27-BREAD', 'Хлеб', 1000, 5000, 100, now()) RETURNING id
                """).query(Long.class).single();
    }

    // Критерии 1, 2, 5: продажа с клиентом 2% от 700 сом -> 14 сом бонусов, баланс и история
    @Test
    void accruesBonusByClientLevel() {
        Long clientId = createClient("TEST-US27-ANNA", 200, 500L);

        CompleteSaleResult result = saleService.completeSale(userId,
                saleWithClient("test-us27-0001", "TEST-US27-ANNA", 7, 10000L));

        assertEquals(1400L, result.earnedBonuses());
        assertEquals(1900L, bonusBalance(clientId));
        assertEquals(clientId, jdbc.sql("SELECT client_id FROM sales WHERE id = :id")
                .param("id", result.saleId()).query(Long.class).single());
        assertEquals(1400L, jdbc.sql("SELECT earned_bonuses FROM sales WHERE id = :id")
                .param("id", result.saleId()).query(Long.class).single());

        Map<String, Object> tx = jdbc.sql("""
                SELECT client_id, amount, operation_type, base_type, base_amount, percent, rounding
                  FROM bonus_transactions WHERE sale_id = :sale
                """).param("sale", result.saleId()).query().singleRow();
        assertEquals(clientId, ((Number) tx.get("client_id")).longValue());
        assertEquals(1400L, ((Number) tx.get("amount")).longValue());
        assertEquals("ACCRUAL", tx.get("operation_type"));
        assertEquals("PAID_AMOUNT", tx.get("base_type"));
        assertEquals(70000L, ((Number) tx.get("base_amount")).longValue());
        assertEquals(200, ((Number) tx.get("percent")).intValue());
        assertEquals("FLOOR", tx.get("rounding"));
    }

    // Критерий 2: округление вниз. 1% от 123,45 сом = 1,2345 сом -> 123 тыйына
    @Test
    void roundsBonusDown() {
        Long clientId = createClient("TEST-US27-BORIS", 100, 0L);

        CompleteSaleResult result = saleService.completeSale(userId,
                saleWithClient("test-us27-0002", "TEST-US27-BORIS", 1, 12345L));

        assertEquals(123L, result.earnedBonuses());
        assertEquals(123L, bonusBalance(clientId));
    }

    // Критерий 3: уровень 0% -> бонуса нет, баланс не меняется, в истории пусто
    @Test
    void zeroLevelGivesNoBonus() {
        Long clientId = createClient("TEST-US27-ZERO", 0, 500L);

        CompleteSaleResult result = saleService.completeSale(userId,
                saleWithClient("test-us27-0003", "TEST-US27-ZERO", 7, 10000L));

        assertEquals(0L, result.earnedBonuses());
        assertEquals(500L, bonusBalance(clientId));
        assertEquals(0, countBonusTransactions(clientId));
        assertEquals(clientId, jdbc.sql("SELECT client_id FROM sales WHERE id = :id")
                .param("id", result.saleId()).query(Long.class).single());
    }

    // Критерий 1: продажа без клиента -> без бонуса и без привязки
    @Test
    void saleWithoutClientGivesNoBonus() {
        CompleteSaleResult result = saleService.completeSale(userId,
                saleWithClient("test-us27-0004", null, 7, 10000L));

        assertEquals(0L, result.earnedBonuses());
        assertTrue(jdbc.sql("SELECT client_id IS NULL FROM sales WHERE id = :id")
                .param("id", result.saleId()).query(Boolean.class).single());
        assertEquals(0L, jdbc.sql("SELECT count(*) FROM bonus_transactions WHERE sale_id = :id")
                .param("id", result.saleId()).query(Long.class).single());
    }

    // Неизвестный клиент -> продажа отклонена, ничего не записано и не списано
    @Test
    void unknownClientRejectsSale() {
        SaleRejectedException error = assertThrows(SaleRejectedException.class,
                () -> saleService.completeSale(userId,
                        saleWithClient("test-us27-0005", "TEST-US27-NOBODY", 1, 5000L)));

        assertEquals("CLIENT_NOT_FOUND", error.getErrorCode());
        assertEquals(0L, jdbc.sql("SELECT count(*) FROM sales WHERE client_uuid = 'test-us27-0005'")
                .query(Long.class).single());
        assertEquals(100, jdbc.sql("SELECT stock_quantity FROM products WHERE id = :id")
                .param("id", breadId).query(Integer.class).single());
    }

    // Отключённый клиент -> тоже CLIENT_NOT_FOUND
    @Test
    void inactiveClientRejectsSale() {
        Long clientId = createClient("TEST-US27-OFF", 200, 0L);
        jdbc.sql("UPDATE clients SET is_active = false WHERE id = :id").param("id", clientId).update();

        SaleRejectedException error = assertThrows(SaleRejectedException.class,
                () -> saleService.completeSale(userId,
                        saleWithClient("test-us27-0006", "TEST-US27-OFF", 1, 5000L)));

        assertEquals("CLIENT_NOT_FOUND", error.getErrorCode());
        assertEquals(0L, bonusBalance(clientId));
    }

    // Критерий 4: повтор через синхронизацию -> duplicate, бонус начислен один раз
    @Test
    void repeatedSyncDoesNotAccrueTwice() {
        Long clientId = createClient("TEST-US27-REPEAT", 200, 0L);
        SyncOperationRequest operation = new SyncOperationRequest("sale", "test-us27-0007", Map.of(
                "payment_method", "cash",
                "client_code", "TEST-US27-REPEAT",
                "items", List.of(Map.of("product_id", breadId, "quantity", 7, "unit_price", 10000))));

        SyncOperationResponse first = pushOne(operation);
        SyncOperationResponse second = pushOne(operation);

        assertEquals(SyncStatus.APPLIED, first.status());
        assertEquals(1400L, first.earnedBonuses());
        assertEquals(SyncStatus.DUPLICATE, second.status());
        assertEquals(1400L, second.earnedBonuses());
        assertEquals(1400L, bonusBalance(clientId));
        assertEquals(1, countBonusTransactions(clientId));
    }

    // Критерий 4: база сама не даёт второе начисление за ту же продажу
    @Test
    void databaseForbidsSecondAccrualForSameSale() {
        Long clientId = createClient("TEST-US27-DB", 200, 0L);
        CompleteSaleResult result = saleService.completeSale(userId,
                saleWithClient("test-us27-0008", "TEST-US27-DB", 7, 10000L));

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.sql("""
                INSERT INTO bonus_transactions (client_id, sale_id, amount, operation_type, created_at)
                VALUES (:client, :sale, 1400, 'ACCRUAL', now())
                """).param("client", clientId).param("sale", result.saleId()).update());

        assertEquals(1, countBonusTransactions(clientId));
    }

    private CompleteSaleRequest saleWithClient(String clientUuid, String clientCode, int quantity, long unitPrice) {
        return new CompleteSaleRequest(clientUuid, PaymentMethod.CASH, null, null, clientCode, null, // US-27: null = debt_reminder_date
                List.of(new CompleteSaleItemRequest(breadId, quantity, unitPrice)));
    }

    private SyncOperationResponse pushOne(SyncOperationRequest operation) {
        return syncService.push(userId, new SyncPushRequest(List.of(operation))).results().getFirst();
    }

    private Long createClient(String clientCode, int cashbackPercentage, long bonusBalance) {
        Long levelId = jdbc.sql("""
                INSERT INTO loyalty_levels (name, cashback_percentage)
                VALUES (:name, :percent) RETURNING id
                """).param("name", "test-us27-" + clientCode).param("percent", cashbackPercentage)
                .query(Long.class).single();
        return jdbc.sql("""
                INSERT INTO clients (client_code, name, loyalty_level_id, bonus_balance, is_active, created_at, updated_at)
                VALUES (:code, 'Тест', :level, :balance, true, now(), now()) RETURNING id
                """).param("code", clientCode).param("level", levelId).param("balance", bonusBalance)
                .query(Long.class).single();
    }

    private long bonusBalance(Long clientId) {
        return jdbc.sql("SELECT bonus_balance FROM clients WHERE id = :id")
                .param("id", clientId).query(Long.class).single();
    }

    private long countBonusTransactions(Long clientId) {
        return jdbc.sql("SELECT count(*) FROM bonus_transactions WHERE client_id = :id")
                .param("id", clientId).query(Long.class).single();
    }
}
