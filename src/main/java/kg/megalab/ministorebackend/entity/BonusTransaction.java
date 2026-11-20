package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "bonus_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BonusTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private BonusOperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_type")
    private BonusBase baseType;

    @Column(name = "base_amount")
    private Long baseAmount;

    @Column(name = "percent")
    private Integer percent;

    @Enumerated(EnumType.STRING)
    @Column(name = "rounding")
    private BonusRounding rounding;

    @Column(name = "bonus_value")
    private Integer bonusValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static BonusTransaction accrual(Long clientId, Long saleId, Long amount,
                                           Long baseAmount, Integer percent) {
        BonusTransaction tx = new BonusTransaction();
        tx.clientId = clientId;
        tx.saleId = saleId;
        tx.amount = amount;
        tx.operationType = BonusOperationType.ACCRUAL;
        tx.baseType = BonusBase.PAID_AMOUNT;
        tx.baseAmount = baseAmount;
        tx.percent = percent;
        tx.rounding = BonusRounding.FLOOR;
        tx.bonusValue = 1;
        return tx;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }
}
