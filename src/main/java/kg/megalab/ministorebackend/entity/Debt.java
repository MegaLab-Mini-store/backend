package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import kg.megalab.ministorebackend.exception.DebtOverpaymentException;
import kg.megalab.ministorebackend.exception.InvalidDebtException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "debts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "sale_id", unique = true)
    private Long saleId;

    @Column(name = "initial_amount", nullable = false)
    private Long initialAmount;

    @Column(name = "remaining_amount", nullable = false)
    private Long remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DebtStatus status;

    @Setter
    @Column(name = "reminder_date")
    private LocalDate reminderDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static Debt create(Client client, Long saleId, long initialAmount, LocalDate reminderDate) {
        Objects.requireNonNull(client, "client must not be null");
        if (initialAmount <= 0L) {
            throw new InvalidDebtException("Сумма долга должна быть больше нуля");
        }

        Debt debt = new Debt();
        debt.client = client;
        debt.saleId = saleId;
        debt.initialAmount = initialAmount;
        debt.remainingAmount = initialAmount;
        debt.status = DebtStatus.OPEN;
        debt.reminderDate = reminderDate;
        return debt;
    }

    public void applyPayment(long paymentAmount) {
        if (this.status != DebtStatus.OPEN) {
            throw new InvalidDebtException("Нельзя погасить закрытый или аннулированный долг");
        }
        if (paymentAmount <= 0L) {
            throw new InvalidDebtException("Сумма погашения должна быть больше нуля");
        }
        if (paymentAmount > this.remainingAmount) {
            throw new DebtOverpaymentException(this.remainingAmount, paymentAmount);
        }

        this.remainingAmount -= paymentAmount;
        if (this.remainingAmount == 0L) {
            this.status = DebtStatus.CLOSED;
        }
    }

    public void voidDebt() {
        if (this.status == DebtStatus.VOIDED) {
            return;
        }
        this.remainingAmount = 0L;
        this.status = DebtStatus.VOIDED;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
