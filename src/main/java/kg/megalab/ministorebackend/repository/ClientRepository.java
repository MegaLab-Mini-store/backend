package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.entity.Client;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    @EntityGraph(attributePaths = "loyaltyLevel")
    Optional<Client> findByClientCodeAndIsActiveTrue(String clientCode);

    boolean existsByClientCode(String clientCode);

    @EntityGraph(attributePaths = "loyaltyLevel")
    Optional<Client> findWithLevelById(Long id);

    @EntityGraph(attributePaths = "loyaltyLevel")
    List<Client> findAllByIsActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "loyaltyLevel")
    @Query("""
        SELECT c FROM Client c
        WHERE c.isActive = true
          AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.clientCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR (c.phone IS NOT NULL AND LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%'))))
        ORDER BY c.createdAt DESC
        """)
    List<Client> searchActiveClients(@Param("search") String search);

    boolean existsByIdAndIsActiveTrue(Long id);
}
