package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.StockMovementResponse;
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

    @Transactional(readOnly = true)
    public List<StockMovementResponse> movements(Long productId, int limit) {
        currentStock(productId);

        return jdbc.sql("""
                SELECT id, operation_type, quantity_change, quantity_after,
                       reference_id, reason, created_at
                  FROM inventory_movements
                 WHERE product_id = :id
                 ORDER BY created_at DESC, id DESC
                 LIMIT :limit
                """)
                .param("id", productId)
                .param("limit", Math.min(Math.max(limit, 1), 200))
                .query((rs, n) -> new StockMovementResponse(
                        rs.getLong("id"),
                        rs.getString("operation_type"),
                        rs.getInt("quantity_change"),
                        rs.getObject("quantity_after", Integer.class),
                        rs.getObject("reference_id", Long.class),
                        rs.getString("reason"),
                        rs.getObject("created_at", OffsetDateTime.class)))
                .list();
    }
}