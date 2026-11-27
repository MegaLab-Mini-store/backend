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

    String runPrefix;
    Long userId;
    Long breadId;

    @BeforeEach
    void setUp() {
        runPrefix = "US03-" + System.nanoTime();

        userId = jdbc.sql("""
                INSERT INTO users (username, password_hash, role, created_at)
                VALUES (:username, 'hash', 'OWNER', now())
                RETURNING id
                """).param("username", "cashier-" + runPrefix).query(Long.class).single();

        breadId = jdbc.sql("""
                INSERT INTO products (barcode, name, purchase_price, sale_price, stock_quantity, updated_at)
                VALUES (:barcode, 'Хлеб', 1000, 5000, 10, now()) RETURNING id
                """).param("barcode", "BARCODE-" + runPrefix).query(Long.class).single();
    }

    private String uuid(String base) {
        return base + "-" + runPrefix;
    }

    @Test
    void newSaleIsApplied() {
        String id = uuid("test-us03-0001");
        SyncOperationResponse result = pushOne(sale(id, 2, 4500L));

        assertEquals(SyncStatus.APPLIED, result.status());
        assertEquals(9000L, result.totalAmount());
        assertNotNull(result.serverId());
        assertEquals(8, stock());
        assertEquals("ACCEPTED", journalStatus(id));
    }

    @Test
    void repeatIsDuplicate() {
        String id = uuid("test-us03-0002");
        SyncOperationResponse first = pushOne(sale(id, 2, 4500L));
        SyncOperationResponse second = pushOne(sale(id, 2, 4500L));

        assertEquals(SyncStatus.DUPLICATE, second.status());
        assertEquals(first.serverId(), second.serverId());
        assertEquals(9000L, second.totalAmount());
        assertEquals(1, countSales(id));
        assertEquals(8, stock());
    }

    @Test
    void sameUuidWithOtherDataIsRejected() {
        String id = uuid("test-us03-0003");
        pushOne(sale(id, 2, 4500L));
        SyncOperationResponse second = pushOne(sale(id, 3, 4500L));

        assertEquals(SyncStatus.REJECTED, second.status());
        assertEquals("IDEMPOTENCY_PAYLOAD_MISMATCH", second.errorCode());
        assertEquals(1, countSales(id));
        assertEquals(8, stock());
    }

    @Test
    void rejectedSaleStaysRejected() {
        String id = uuid("test-us03-0004");
        SyncOperationRequest unknownProduct = new SyncOperationRequest("sale", id,
                Map.of("payment_method", "cash",
                        "items", List.of(Map.of("product_id", 999999, "quantity", 1, "unit_price", 1000))));

        SyncOperationResponse first = pushOne(unknownProduct);
        SyncOperationResponse second = pushOne(unknownProduct);

        assertEquals("PRODUCT_NOT_FOUND", first.errorCode());
        assertEquals(SyncStatus.REJECTED, second.status());
        assertEquals("PRODUCT_NOT_FOUND", second.errorCode());
        assertEquals("REJECTED", journalStatus(id));
        assertEquals(0, countSales(id));
    }

    @Test
    void badOperationDoesNotCancelOthers() {
        String id1 = uuid("test-us03-0005");
        String id2 = uuid("test-us03-0006");
        String id3 = uuid("test-us03-0007");
        List<SyncOperationResponse> results = syncService.push(userId, new SyncPushRequest(List.of(
                sale(id1, 1, 5000L),
                sale(id2, 0, 5000L),
                sale(id3, 1, 5000L)))).results();

        assertEquals(SyncStatus.APPLIED, results.get(0).status());
        assertEquals(SyncStatus.REJECTED, results.get(1).status());
        assertEquals("VALIDATION_ERROR", results.get(1).errorCode());
        assertEquals(SyncStatus.APPLIED, results.get(2).status());
        assertEquals(8, stock());
    }

    @Test
    void unsupportedOperationIsNotSaved() {
        String id = uuid("test-us03-0008");
        SyncOperationResponse result = pushOne(new SyncOperationRequest("delivery", id,
                Map.of("items", List.of())));

        assertEquals("OPERATION_NOT_SUPPORTED", result.errorCode());
        assertNull(journalStatus(id));
    }

    @Test
    void emptyBatchIsInvalid() {
        assertThrows(InvalidSyncRequestException.class,
                () -> syncService.push(userId, new SyncPushRequest(List.of())));
    }

    @Test
    void syncSaleSavesCardPaymentMethod() {
        String id = uuid("test-us03-0017");
        SyncOperationRequest operation = new SyncOperationRequest(
                "sale",
                id,
                Map.of(
                        "payment_method", "card",
                        "items", List.of(Map.of(
                                "product_id", breadId,
                                "quantity", 1,
                                "unit_price", 5000L
                        ))
                )
        );

        SyncOperationResponse result = pushOne(operation);

        assertEquals(SyncStatus.APPLIED, result.status());

        String paymentMethod = jdbc.sql(
                        "SELECT payment_method FROM sales WHERE client_uuid = :uuid"
                )
                .param("uuid", id)
                .query(String.class)
                .single();

        assertEquals("CARD", paymentMethod);
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