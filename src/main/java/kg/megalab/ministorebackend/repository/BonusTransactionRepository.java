package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.entity.BonusTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BonusTransactionRepository extends JpaRepository<BonusTransaction, Long> {
}
