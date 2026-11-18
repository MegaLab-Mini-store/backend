package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.OperationType;
import kg.megalab.ministorebackend.exception.InsufficientStockException;
import kg.megalab.ministorebackend.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class StockService {

    private record ProductInfo(String name, int stock) {}

    private final JdbcClient jdbc;
    private final Clock clock;

    public StockService(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /**
     * delta > 0: приход, delta < 0: расход.
     * Только внутри транзакции приёмки или продажи (MANDATORY).
     * @return остаток после операции
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int apply(Long productId, int delta, OperationType type,
                     Long referenceId, String reason) {
        if (delta == 0) {
            throw new IllegalArgumentException("Количество не может быть нулевым");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);

        Optional<Integer> after = jdbc.sql("""
                UPDATE products
                   SET stock_quantity = stock_quantity + :delta,
                       updated_at = :now
                 WHERE id = :id
                   AND stock_quantity + :delta >= 0
             RETURNING stock_quantity
                """)
                .param("delta", delta)
                .param("now", now)
                .param("id", productId)
                .query(Integer.class)
                .optional();

        if (after.isEmpty()) {
            ProductInfo info = jdbc.sql("SELECT name, stock_quantity FROM products WHERE id = :id")
                    .param("id", productId)
                    .query((rs, n) -> new ProductInfo(rs.getString("name"), rs.getInt("stock_quantity")))
                    .optional()
                    .orElseThrow(() -> new ResourceNotFoundException("Товар не найден (id " + productId + ")"));
            throw new InsufficientStockException(info.name(), -delta, info.stock());
        }

        jdbc.sql("""
                INSERT INTO inventory_movements
                    (product_id, operation_type, quantity_change, quantity_after,
                     reference_id, reason, created_at)
                VALUES (:p, :t, :d, :a, :ref, :reason, :now)
                """)
                .param("p", productId)
                .param("t", type.name())
                .param("d", delta)
                .param("a", after.get())
                .param("ref", referenceId)
                .param("reason", reason)
                .param("now", now)
                .update();

        return after.get();
    }
}