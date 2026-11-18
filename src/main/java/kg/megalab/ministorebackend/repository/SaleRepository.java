package kg.megalab.ministorebackend.repository;

import java.util.List;
import java.util.Optional;
import kg.megalab.ministorebackend.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByClientUuid(String clientUuid);

    Page<Sale> findAllByClientIdOrderByOccurredAtDescCreatedAtDesc(Long clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Sale> findAllWithItemsByIdIn(List<Long> ids);
}
