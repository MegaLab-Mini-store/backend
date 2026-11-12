package kg.megalab.ministorebackend.repository;

import java.time.OffsetDateTime;
import kg.megalab.ministorebackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying(flushAutomatically = true)
    @Query("""
            update Product p
            set p.stockQuantity = p.stockQuantity - :quantity,
                p.updatedAt = :now
            where p.id = :id
            """)
    int decreaseStock(@Param("id") Long id,
                      @Param("quantity") int quantity,
                      @Param("now") OffsetDateTime now);
}
