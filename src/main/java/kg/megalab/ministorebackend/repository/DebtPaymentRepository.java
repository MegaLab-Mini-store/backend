package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.entity.DebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {

    Optional<DebtPayment> findByClientUuid(String clientUuid);

    List<DebtPayment> findAllByDebtIdOrderByCreatedAtDescIdDesc(Long debtId);

    @Query(value = """
        SELECT 1 FROM pg_advisory_xact_lock(
            hashtextextended(:clientUuid, 0)
        )
        """, nativeQuery = true)
    Integer lockByClientUuid(@Param("clientUuid") String clientUuid);
}
