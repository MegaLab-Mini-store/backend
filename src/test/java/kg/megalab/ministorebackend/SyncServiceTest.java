package kg.megalab.ministorebackend;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import kg.megalab.ministorebackend.dto.SyncOperationRequest;
import kg.megalab.ministorebackend.dto.SyncOperationResponse;
import kg.megalab.ministorebackend.dto.SyncPushRequest;
import kg.megalab.ministorebackend.dto.SyncStatus;
import kg.megalab.ministorebackend.exception.InvalidSyncRequestException;
import kg.megalab.ministorebackend.service.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SyncServiceTest {

    @Autowired SyncService syncService;
    @Autowired JdbcClient jdbc;

    Long userId;
    Long breadId;

    @BeforeEach
    void setUp() {
        jdbc.sql("""
                DELETE FROM inventory_movements
                 WHERE product_id IN (SELECT id FROM products WHERE barcode LIKE 'TEST-US03-%')
                """).update();
        jdbc.sql("""
                DELETE FROM sale_items
                 WHERE product_id IN (SELECT id FROM products WHERE barcode LIKE 'TEST-US03-%')
                """).update();
        jdbc.sql("DELETE FROM sales WHERE client_uuid LIKE 'test-us03-%'").update();
        jdbc.sql("DELETE FROM sync_operations WHERE client_uuid LIKE 'test-us03-%'").update();
        jdbc.sql("DELETE FROM products WHERE barcode LIKE 'TEST-US03-%'").update();

        userId = jdbc.sql("""
                INSERT INTO users (username, password_hash, role, created_at)
                VALUES ('test-us03-cashier', 'hash', 'OWNER', now())
                ON CONFLICT (username) DO UPDATE SET username = EXCLUDED.username
                RETURNING id
                """).query(Long.class).single();
        breadId = jdbc.sql("""
                INSERT INTO products (barcode, name, purchase_price, sale_price, stock_quantity, updated_at)
                VALUES ('TEST-US03-BREAD', 'Хлеб', 1000, 5000, 10, now()) RETURNING id
                """).query(Long.class).single();
    }

    // Критерий 2: новая операция сохраняется с номером, ответ applied
    @Test
    void newSaleIsApplied() {
        SyncOperationResponse result = pushOne(sale("test-us03-0001", 2, 4500L));

        assertEquals(SyncStatus.APPLIED, result.status());
        assertEquals(9000L, result.totalAmount());
        assertNotNull(result.serverId());
        assertEquals(8, stock());
        assertEquals("ACCEPTED", journalStatus("test-us03-0001"));
    }

    // Критерий 4: повтор не создаёт вторую продажу и не списывает товар второй раз
    @Test
    void repeatIsDuplicate() {
        SyncOperationResponse first = pushOne(sale("test-us03-0002", 2, 4500L));
        SyncOperationResponse second = pushOne(sale("test-us03-0002", 2, 4500L));

        assertEquals(SyncStatus.DUPLICATE, second.status());
        assertEquals(first.serverId(), second.serverId());
        assertEquals(9000L, second.totalAmount());
        assertEquals(1, countSales("test-us03-0002"));
        assertEquals(8, stock());
    }

    // Тот же номер с другими данными: отказ, первая продажа не меняется
    @Test
    void sameUuidWithOtherDataIsRejected() {
        pushOne(sale("test-us03-0003", 2, 4500L));
        SyncOperationResponse second = pushOne(sale("test-us03-0003", 3, 4500L));

        assertEquals(SyncStatus.REJECTED, second.status());
        assertEquals("IDEMPOTENCY_PAYLOAD_MISMATCH", second.errorCode());
        assertEquals(1, countSales("test-us03-0003"));
        assertEquals(8, stock());
    }

    // Отклонённая операция записана в журнал, повтор снова получает отказ
    @Test
    void rejectedSaleStaysRejected() {
        SyncOperationRequest unknownProduct = new SyncOperationRequest("sale", "test-us03-0004",
                Map.of("payment_method", "cash",
                        "items", List.of(Map.of("product_id", 999999, "quantity", 1, "unit_price", 1000))));

        SyncOperationResponse first = pushOne(unknownProduct);
        SyncOperationResponse second = pushOne(unknownProduct);

        assertEquals("PRODUCT_NOT_FOUND", first.errorCode());
        assertEquals(SyncStatus.REJECTED, second.status());
        assertEquals("PRODUCT_NOT_FOUND", second.errorCode());
        assertEquals("REJECTED", journalStatus("test-us03-0004"));
        assertEquals(0, countSales("test-us03-0004"));
    }

    // Сбой одной операции не отменяет остальные, порядок ответов как в запросе
    @Test
    void badOperationDoesNotCancelOthers() {
        List<SyncOperationResponse> results = syncService.push(userId, new SyncPushRequest(List.of(
                sale("test-us03-0005", 1, 5000L),
                sale("test-us03-0006", 0, 5000L),
                sale("test-us03-0007", 1, 5000L)))).results();

        assertEquals(SyncStatus.APPLIED, results.get(0).status());
        assertEquals(SyncStatus.REJECTED, results.get(1).status());
        assertEquals("VALIDATION_ERROR", results.get(1).errorCode());
        assertEquals(SyncStatus.APPLIED, results.get(2).status());
        assertEquals(8, stock());
    }

    // Тип, который пока не поддерживается: отказ без записи в журнал
    @Test
    void unsupportedOperationIsNotSaved() {
        SyncOperationResponse result = pushOne(new SyncOperationRequest("delivery", "test-us03-0008",
                Map.of("items", List.of())));

        assertEquals("OPERATION_NOT_SUPPORTED", result.errorCode());
        assertNull(journalStatus("test-us03-0008"));
    }

    // Сломан сам запрос: ошибка на всю пачку
    @Test
    void emptyBatchIsInvalid() {
        assertThrows(InvalidSyncRequestException.class,
                () -> syncService.push(userId, new SyncPushRequest(List.of())));
    }

    private SyncOperationRequest sale(String clientUuid, int quantity, long unitPrice) {
        return new SyncOperationRequest("sale", clientUuid, Map.of(
                "payment_method", "cash",
                "items", List.of(Map.of("product_id", breadId, "quantity", quantity, "unit_price", unitPrice))));
    }

    private SyncOperationResponse pushOne(SyncOperationRequest operation) {
        return syncService.push(userId, new SyncPushRequest(List.of(operation))).results().getFirst();
    }

    private int stock() {
        return jdbc.sql("SELECT stock_quantity FROM products WHERE id = :id")
                .param("id", breadId).query(Integer.class).single();
    }

    private long countSales(String clientUuid) {
        return jdbc.sql("SELECT count(*) FROM sales WHERE client_uuid = :uuid")
                .param("uuid", clientUuid).query(Long.class).single();
    }

    private String journalStatus(String clientUuid) {
        return jdbc.sql("SELECT status FROM sync_operations WHERE client_uuid = :uuid")
                .param("uuid", clientUuid).query(String.class).optional().orElse(null);
    }
}
