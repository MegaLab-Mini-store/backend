package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "client_id")
    private Long clientId;

    @Size(min = 8, max = 64)
    @Column(name = "client_uuid", length = 64, unique = true)
    private String clientUuid;

    @Column(name = "replaces_client_uuid", length = 64)
    private String replacesClientUuid;

    @Column(name = "total_amount", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long totalAmount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @PositiveOrZero
    @Column(name = "bonus_spent", nullable = false)
    private Long bonusSpent = 0L;

    @PositiveOrZero
    @Column(name = "paid_amount", nullable = false)
    private Long paidAmount = 0L;

    @PositiveOrZero
    @Column(name = "earned_bonuses", nullable = false)
    private Long earnedBonuses = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SaleStatus status = SaleStatus.COMPLETED;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;

    @Column(name = "original_occurred_at")
    private OffsetDateTime originalOccurredAt;

    @Column(name = "clock_skew_detected", nullable = false)
    private Boolean clockSkewDetected = false;

    @Column(name = "late_sync", nullable = false)
    private Boolean lateSync = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.PERSIST, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<SaleItem> items = new ArrayList<>();

    public static Sale create(Long userId, PaymentMethod paymentMethod) {
        Sale sale = new Sale();
        sale.userId = userId;
        sale.paymentMethod = paymentMethod;
        return sale;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.occurredAt == null) {
            this.occurredAt = this.createdAt;
        }
    }

    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream().mapToLong(SaleItem::getSubtotal).sum();
    }
}
