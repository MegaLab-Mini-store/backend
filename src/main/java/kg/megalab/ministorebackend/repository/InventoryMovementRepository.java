package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
}
