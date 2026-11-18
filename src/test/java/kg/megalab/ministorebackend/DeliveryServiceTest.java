package kg.megalab.ministorebackend;

import kg.megalab.ministorebackend.auth.AuthService;
import kg.megalab.ministorebackend.dto.DeliveryItemRequest;
import kg.megalab.ministorebackend.dto.DeliveryRequest;
import kg.megalab.ministorebackend.dto.DeliveryResponse;
import kg.megalab.ministorebackend.exception.InvalidDeliveryException;
import kg.megalab.ministorebackend.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeliveryServiceTest {

    @Autowired DeliveryService deliveryService;
    @Autowired JdbcClient jdbc;
    @Autowired MockMvc mvc;
    @Autowired AuthService authService;
    @Autowired PasswordEncoder passwordEncoder;

    long userId;
    long firstProductId;
    long secondProductId;
    String token;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        userId = jdbc.sql("""
                INSERT INTO users (username, password_hash, role, created_at)
                VALUES (:username, :passwordHash, 'CASHIER', now()) RETURNING id
                """)
                .param("username", "delivery-test-" + suffix)
                .param("passwordHash", passwordEncoder.encode("test-password"))
                .query(Long.class).single();
        token = authService.login("delivery-test-" + suffix, "test-password")
                .orElseThrow().token();
        firstProductId = insertProduct("delivery-first-" + suffix, 4);
        secondProductId = insertProduct("delivery-second-" + suffix, 7);
    }

    private long insertProduct(String barcode, int stock) {
        return jdbc.sql("""
                INSERT INTO products (barcode, name, purchase_price, sale_price, stock_quantity, updated_at)
                VALUES (:barcode, 'Тестовый товар', 100, 150, :stock, now()) RETURNING id
                """)
                .param("barcode", barcode)
                .param("stock", stock)
                .query(Long.class).single();
    }

    private int stockOf(long productId) {
        return jdbc.sql("SELECT stock_quantity FROM products WHERE id = :id")
                .param("id", productId).query(Integer.class).single();
    }

    @Test
    void createsDeliveryWithItemsAndMovements() {
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-11-16T10:15:00+06:00");
        DeliveryRequest request = new DeliveryRequest(List.of(
                new DeliveryItemRequest(firstProductId, 3, 120L),
                new DeliveryItemRequest(secondProductId, 2, 200L)
        ), "Поставщик", occurredAt);

        DeliveryResponse result = deliveryService.create(request, userId);

        assertNotNull(result.id());
        assertEquals(userId, result.userId());
        assertEquals(760L, result.totalAmount());
        assertEquals(occurredAt.toInstant(), result.occurredAt().toInstant());
        assertNotNull(result.createdAt());
        assertEquals(7, stockOf(firstProductId));
        assertEquals(9, stockOf(secondProductId));
        assertEquals(7, result.items().get(0).stockQuantityAfter());
        assertEquals(9, result.items().get(1).stockQuantityAfter());

        DeliveryResponse stored = deliveryService.get(result.id());
        assertEquals(result.totalAmount(), stored.totalAmount());
        assertEquals(result.items(), stored.items());
        assertEquals(2L, jdbc.sql("SELECT count(*) FROM delivery_items WHERE delivery_id = :id")
                .param("id", result.id()).query(Long.class).single());
        assertEquals(2L, jdbc.sql("""
                SELECT count(*) FROM inventory_movements
                 WHERE operation_type = 'DELIVERY' AND reference_id = :id
                """).param("id", result.id()).query(Long.class).single());
    }

    @Test
    void failureAfterFirstItemRollsBackEverything() {
        jdbc.sql("UPDATE products SET stock_quantity = :stock WHERE id = :id")
                .param("stock", Integer.MAX_VALUE)
                .param("id", secondProductId)
                .update();
        DeliveryRequest request = new DeliveryRequest(List.of(
                new DeliveryItemRequest(firstProductId, 3, 120L),
                new DeliveryItemRequest(secondProductId, 1, 200L)
        ), null, null);

        assertThrows(RuntimeException.class, () -> deliveryService.create(request, userId));

        assertEquals(4, stockOf(firstProductId));
        assertEquals(Integer.MAX_VALUE, stockOf(secondProductId));
        assertEquals(0L, jdbc.sql("SELECT count(*) FROM deliveries WHERE user_id = :id")
                .param("id", userId).query(Long.class).single());
        assertEquals(0L, jdbc.sql("SELECT count(*) FROM inventory_movements WHERE product_id = :id")
                .param("id", firstProductId).query(Long.class).single());
    }

    @Test
    void duplicateProductIsRejectedBeforeSaving() {
        DeliveryRequest request = new DeliveryRequest(List.of(
                new DeliveryItemRequest(firstProductId, 1, 120L),
                new DeliveryItemRequest(firstProductId, 2, 120L)
        ), null, null);

        assertThrows(InvalidDeliveryException.class, () -> deliveryService.create(request, userId));
        assertEquals(4, stockOf(firstProductId));
        assertEquals(0L, jdbc.sql("SELECT count(*) FROM deliveries WHERE user_id = :id")
                .param("id", userId).query(Long.class).single());
    }

    @Test
    void httpEndpointRequiresLoginAndReturnsSavedDelivery() throws Exception {
        String body = """
                {"items":[{"productId":%d,"quantity":2,"purchasePrice":130}]}
                """.formatted(firstProductId);

        mvc.perform(post("/api/deliveries")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isUnauthorized());

        String location = mvc.perform(post("/api/deliveries")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.totalAmount").value(260))
                .andExpect(jsonPath("$.items[0].stockQuantityAfter").value(6))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].purchasePrice").value(130));
    }
}
