package kg.megalab.ministorebackend.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public class CashSummaryRepository {

    public record MethodTotals(long paid, long count) {}

    private final JdbcClient jdbc;

    public CashSummaryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Суммы и количество чеков по способу оплаты (CASH или CARD), только COMPLETED. */
    public MethodTotals byMethod(String method, OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(paid_amount), 0) AS paid, COUNT(*) AS cnt
                  FROM sales
                 WHERE status = 'COMPLETED'
                   AND payment_method = :method
                   AND COALESCE(occurred_at, created_at) >= :from
                   AND COALESCE(occurred_at, created_at) < :to
                """)
                .param("method", method)
                .param("from", from)
                .param("to", to)
                .query((rs, n) -> new MethodTotals(rs.getLong("paid"), rs.getLong("cnt")))
                .single();
    }

    /** Сколько продано в долг (не поступило в кассу). */
    public long debtTotal(OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(total_amount - bonus_spent - paid_amount), 0)
                  FROM sales
                 WHERE status = 'COMPLETED'
                   AND payment_method = 'DEBT'
                   AND COALESCE(occurred_at, created_at) >= :from
                   AND COALESCE(occurred_at, created_at) < :to
                """)
                .param("from", from)
                .param("to", to)
                .query(Long.class).single();
    }

    public long bonusSpentTotal(OffsetDateTime from, OffsetDateTime to) {
        return jdbc.sql("""
                SELECT COALESCE(SUM(bonus_spent), 0)
                  FROM sales
                 WHERE status = 'COMPLETED'
                   AND COALESCE(occurred_at, created_at) >= :from
                   AND COALESCE(occurred_at, created_at) < :to
                """)
                .param("from", from)
                .param("to", to)
                .query(Long.class).single();
    }
}