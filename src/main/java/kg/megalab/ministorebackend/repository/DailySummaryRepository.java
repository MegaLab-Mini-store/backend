package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.dto.OperationType;
import kg.megalab.ministorebackend.dto.ProductDayMovement;
import kg.megalab.ministorebackend.dto.StockMovementResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class DailySummaryRepository {

    private final JdbcClient jdbc;

    public DailySummaryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Только успешные продажи (COMPLETED). Аннулированные VOIDED не считаются. */
    public long salesTotal(OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(total_amount), 0) FROM sales
                 WHERE status = 'COMPLETED'
                   AND COALESCE(occurred_at, created_at) >= :from
                   AND COALESCE(occurred_at, created_at) < :to
                """)
                .param("from", from).param("to", to)
                .query(Long.class).single();
    }

    public long salesCount(OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM sales
                 WHERE status = 'COMPLETED'
                   AND COALESCE(occurred_at, created_at) >= :from
                   AND COALESCE(occurred_at, created_at) < :to
                """)
                .param("from", from).param("to", to)
                .query(Long.class).single();
    }

    public long purchasesTotal(OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(total_amount), 0) FROM deliveries
                 WHERE COALESCE(occurred_at, created_at) >= :from
                   AND COALESCE(occurred_at, created_at) < :to
                """)
                .param("from", from).param("to", to)
                .query(Long.class).single();
    }

    public long deliveriesCount(OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM deliveries
                 WHERE COALESCE(occurred_at, created_at) >= :from
                   AND COALESCE(occurred_at, created_at) < :to
                """)
                .param("from", from).param("to", to)
                .query(Long.class).single();
    }

    /** Движение по каждому товару за день. Журнал пишется только при успешной транзакции. */
    public List<ProductDayMovement> productMovements(OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT p.id, p.name,
                       COALESCE(SUM(CASE WHEN m.quantity_change > 0 THEN m.quantity_change END), 0) AS incoming,
                       COALESCE(-SUM(CASE WHEN m.quantity_change < 0 THEN m.quantity_change END), 0) AS outgoing
                  FROM inventory_movements m
                  JOIN products p ON p.id = m.product_id
                 WHERE m.created_at >= :from AND m.created_at < :to
                 GROUP BY p.id, p.name
                 ORDER BY p.name
                """)
                .param("from", from).param("to", to)
                .query((rs, n) -> {
                    int in = rs.getInt("incoming");
                    int out = rs.getInt("outgoing");
                    return new ProductDayMovement(rs.getLong("id"), rs.getString("name"), in, out, in - out);
                })
                .list();
    }

    public Optional<String> productName(Long productId) {
        return jdbc.sql("SELECT name FROM products WHERE id = :id")
                .param("id", productId)
                .query(String.class)
                .optional();
    }

    public List<StockMovementResponse> movementsOfProduct(Long productId, OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT id, operation_type, quantity_change, quantity_after,
                       reference_id, reason, created_at
                  FROM inventory_movements
                 WHERE product_id = :id AND created_at >= :from AND created_at < :to
                 ORDER BY created_at, id
                """)
                .param("id", productId).param("from", from).param("to", to)
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
    }
}