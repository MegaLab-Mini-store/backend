package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.entity.Client;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    @EntityGraph(attributePaths = "loyaltyLevel")
    Optional<Client> findByClientCodeAndIsActiveTrue(String clientCode);
}
