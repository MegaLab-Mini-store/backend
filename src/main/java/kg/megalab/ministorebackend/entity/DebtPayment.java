package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "debt_payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DebtPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debt_id", nullable = false)
    private Debt debt;

    @Column(name = "client_uuid", unique = true, length = 64)
    private String clientUuid;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static DebtPayment create(
            Debt debt,
            String clientUuid,
            long amount,
            PaymentMethod paymentMethod,
            OffsetDateTime occurredAt
    ) {
        DebtPayment payment = new DebtPayment();
        payment.debt = debt;
        payment.clientUuid = clientUuid;
        payment.amount = amount;
        payment.paymentMethod = paymentMethod;
        payment.occurredAt = occurredAt;

        return payment;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}