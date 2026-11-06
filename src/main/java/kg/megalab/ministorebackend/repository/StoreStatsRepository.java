package kg.megalab.ministorebackend.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class StoreStatsRepository {

    // нужны имена из db/migration после merge develop
    private static final String PRODUCTS = "products";
    private static final String CUSTOMERS = "customers";
    private static final String SALES = "sales";
    private static final String SALES_DATE_COL = "created_at";
    private static final String RECEIPTS = "stock_receipts";
    private static final String RECEIPTS_DATE_COL = "created_at";

    private final JdbcClient jdbc;
    public StoreStatsRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public long countProducts()  { return count("SELECT COUNT(*) FROM " + PRODUCTS); }
    public long countCustomers() { return count("SELECT COUNT(*) FROM " + CUSTOMERS); }

    public long countSales(Instant from, Instant to) {
        return between(SALES, SALES_DATE_COL, from, to);
    }
    public long countReceipts(Instant from, Instant to) {
        return between(RECEIPTS, RECEIPTS_DATE_COL, from, to);
    }

    private long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    private long between(String table, String col, Instant from, Instant to) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE " + col + " >= :f AND " + col + " < :t")
                .param("f", Timestamp.from(from))
                .param("t", Timestamp.from(to))
                .query(Long.class).single();
    }
}