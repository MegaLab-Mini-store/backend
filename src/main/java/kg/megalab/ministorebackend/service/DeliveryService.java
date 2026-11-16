package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.DeliveryItemRequest;
import kg.megalab.ministorebackend.dto.DeliveryItemResponse;
import kg.megalab.ministorebackend.dto.DeliveryRequest;
import kg.megalab.ministorebackend.dto.DeliveryResponse;
import kg.megalab.ministorebackend.dto.OperationType;
import kg.megalab.ministorebackend.exception.InvalidDeliveryException;
import kg.megalab.ministorebackend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private record Item(Long productId, String productName, int quantity, long purchasePrice) {}

    private final JdbcClient jdbc;
    private final StockService stockService;
    private final Clock clock;

    @Transactional
    public DeliveryResponse create(DeliveryRequest request, Long userId) {
        List<Item> items = new ArrayList<>();
        Set<Long> productIds = new HashSet<>();
        long totalAmount = 0;

        for (DeliveryItemRequest requested : request.items()) {
            if (!productIds.add(requested.productId())) {
                throw new InvalidDeliveryException("Товар с ID " + requested.productId() + " повторяется в поставке");
            }
            String productName = jdbc.sql("SELECT name FROM products WHERE id = :id AND is_active = true")
                    .param("id", requested.productId())
                    .query(String.class)
                    .optional()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Товар не найден или неактивен (id " + requested.productId() + ")"));
            try {
                totalAmount = Math.addExact(totalAmount,
                        Math.multiplyExact(requested.purchasePrice(), requested.quantity().longValue()));
            } catch (ArithmeticException ex) {
                throw new InvalidDeliveryException("Сумма поставки слишком большая");
            }
            items.add(new Item(requested.productId(), productName,
                    requested.quantity(), requested.purchasePrice()));
        }

        OffsetDateTime createdAt = OffsetDateTime.now(clock);
        OffsetDateTime occurredAt = request.occurredAt() == null ? createdAt : request.occurredAt();
        String supplierInfo = request.supplierInfo() == null ? null : request.supplierInfo().trim();
        Long deliveryId = jdbc.sql("""
                INSERT INTO deliveries (user_id, supplier_info, total_amount, occurred_at, created_at)
                VALUES (:userId, :supplierInfo, :totalAmount, :occurredAt, :createdAt)
                RETURNING id
                """)
                .param("userId", userId)
                .param("supplierInfo", supplierInfo)
                .param("totalAmount", totalAmount)
                .param("occurredAt", occurredAt)
                .param("createdAt", createdAt)
                .query(Long.class)
                .single();

        List<DeliveryItemResponse> responses = new ArrayList<>();
        for (Item item : items) {
            jdbc.sql("""
                    INSERT INTO delivery_items (delivery_id, product_id, quantity, purchase_price)
                    VALUES (:deliveryId, :productId, :quantity, :purchasePrice)
                    """)
                    .param("deliveryId", deliveryId)
                    .param("productId", item.productId())
                    .param("quantity", item.quantity())
                    .param("purchasePrice", item.purchasePrice())
                    .update();

            int stockAfter = stockService.apply(item.productId(), item.quantity(),
                    OperationType.DELIVERY, deliveryId, "Поставка №" + deliveryId);
            responses.add(new DeliveryItemResponse(item.productId(), item.productName(),
                    item.quantity(), item.purchasePrice(), stockAfter));
        }

        return new DeliveryResponse(deliveryId, userId, supplierInfo, totalAmount,
                occurredAt, createdAt, List.copyOf(responses));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse get(Long id) {
        DeliveryResponse header = jdbc.sql("""
                SELECT id, user_id, supplier_info, total_amount, occurred_at, created_at
                  FROM deliveries WHERE id = :id
                """)
                .param("id", id)
                .query((rs, row) -> new DeliveryResponse(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getString("supplier_info"),
                        rs.getLong("total_amount"),
                        rs.getObject("occurred_at", OffsetDateTime.class),
                        rs.getObject("created_at", OffsetDateTime.class),
                        List.of()))
                .optional()
                .orElseThrow(() -> new ResourceNotFoundException("Поставка не найдена (id " + id + ")"));

        List<DeliveryItemResponse> items = jdbc.sql("""
                SELECT di.product_id, p.name, di.quantity, di.purchase_price, im.quantity_after
                  FROM delivery_items di
                  JOIN products p ON p.id = di.product_id
                  JOIN inventory_movements im ON im.product_id = di.product_id
                     AND im.operation_type = 'DELIVERY' AND im.reference_id = di.delivery_id
                 WHERE di.delivery_id = :id
                 ORDER BY di.id
                """)
                .param("id", id)
                .query((rs, row) -> new DeliveryItemResponse(
                        rs.getLong("product_id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getLong("purchase_price"),
                        rs.getInt("quantity_after")))
                .list();

        return new DeliveryResponse(header.id(), header.userId(), header.supplierInfo(),
                header.totalAmount(), header.occurredAt(), header.createdAt(), items);
    }
}
