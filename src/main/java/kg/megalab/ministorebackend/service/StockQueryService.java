package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.OperationType;
import kg.megalab.ministorebackend.dto.StockMovementResponse;
import kg.megalab.ministorebackend.dto.StockMovementsPage;
import kg.megalab.ministorebackend.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class StockQueryService {

    private final JdbcClient jdbc;

    public StockQueryService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public int currentStock(Long productId) {
        return jdbc.sql("SELECT stock_quantity FROM products WHERE id = :id")
                .param("id", productId)
                .query(Integer.class)
                .optional()
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден (id " + productId + ")"));
    }

    /**
     * @param direction IN (приход), OUT (расход) или null (все)
     */
    @Transactional(readOnly = true)
    public StockMovementsPage movements(Long productId, String direction, int page, int size) {
        String name = jdbc.sql("SELECT name FROM products WHERE id = :id")
                .param("id", productId)
                .query(String.class)
                .optional()
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден (id " + productId + ")"));
        int stock = currentStock(productId);

        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);

        String filter = switch (direction == null ? "" : direction.toUpperCase()) {
            case "IN" -> " AND quantity_change > 0";
            case "OUT" -> " AND quantity_change < 0";
            case "" -> "";
            default -> throw new IllegalArgumentException("direction должен быть IN или OUT");
        };

        long total = jdbc.sql("SELECT COUNT(*) FROM inventory_movements WHERE product_id = :id" + filter)
                .param("id", productId)
                .query(Long.class).single();

        List<StockMovementResponse> items = jdbc.sql("""
                SELECT id, operation_type, quantity_change, quantity_after,
                       reference_id, reason, created_at
                  FROM inventory_movements
                 WHERE product_id = :id""" + filter + """

                 ORDER BY created_at DESC, id DESC
                 LIMIT :limit OFFSET :offset
                """)
                .param("id", productId)
                .param("limit", safeSize)
                .param("offset", (long) safePage * safeSize)
                .query((rs, n) -> new StockMovementResponse(
                        rs.getLong("id"),
                        rs.getString("operation_type"),
                        OperationType.label(rs.getString("operation_type")),
                        rs.getInt("quantity_change"),
                        rs.getObject("quantity_after", Integer.class),
                        rs.getObject("reference_id", Long.class),
                        rs.getString("reason"),
                        rs.getObject("created_at", OffsetDateTime.class)))
                .list();

        return new StockMovementsPage(productId, name, stock, total, safePage, safeSize, items);
    }
}