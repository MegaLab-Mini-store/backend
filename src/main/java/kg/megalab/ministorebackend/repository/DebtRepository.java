package kg.megalab.ministorebackend.repository;

import kg.megalab.ministorebackend.entity.Debt;
import kg.megalab.ministorebackend.entity.DebtStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    interface ClientDebtTotalProjection {
        Long getClientId();
        Long getTotalDebt();
    }

    @EntityGraph(attributePaths = "client")
    Optional<Debt> findWithClientById(Long id);

    @EntityGraph(attributePaths = "client")
    @Query("""
            SELECT d FROM Debt d
            WHERE (:status IS NULL OR d.status = :status)
              AND (:clientId IS NULL OR d.client.id = :clientId)
            ORDER BY d.createdAt DESC, d.id DESC
            """)
    Page<Debt> findAllFiltered(
            @Param("status") DebtStatus status,
            @Param("clientId") Long clientId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "client")
    Page<Debt> findAllByClientIdOrderByCreatedAtDescIdDesc(Long clientId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(d.remainingAmount), 0)
            FROM Debt d
            WHERE d.client.id = :clientId
              AND d.status = kg.megalab.ministorebackend.entity.DebtStatus.OPEN
            """)
    long sumOpenRemainingAmountByClientId(@Param("clientId") Long clientId);

    @Query("""
            SELECT d.client.id AS clientId, COALESCE(SUM(d.remainingAmount), 0) AS totalDebt
            FROM Debt d
            WHERE d.client.id IN :clientIds
              AND d.status = kg.megalab.ministorebackend.entity.DebtStatus.OPEN
            GROUP BY d.client.id
            """)
    List<ClientDebtTotalProjection> sumOpenRemainingAmountByClientIds(@Param("clientIds") Collection<Long> clientIds);

    boolean existsBySaleId(Long saleId);
}
