package kg.megalab.ministorebackend.repository;

import java.util.Optional;
import kg.megalab.ministorebackend.entity.SyncOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncOperationRepository extends JpaRepository<SyncOperation, Integer> {

    Optional<SyncOperation> findByClientUuid(String clientUuid);
}
