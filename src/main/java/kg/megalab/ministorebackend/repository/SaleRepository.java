package kg.megalab.ministorebackend.repository;

import java.util.Optional;
import kg.megalab.ministorebackend.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    Optional<Sale> findByClientUuid(String clientUuid);
}
