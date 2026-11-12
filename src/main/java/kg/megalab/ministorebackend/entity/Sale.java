package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_uuid", unique = true, length = 64)
    private String clientUuid;

    @Column(name = "replaces_client_uuid", length = 64)
    private String replacesClientUuid;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "bonus_spent", nullable = false)
    private Long bonusSpent = 0L;

    @Column(name = "paid_amount", nullable = false)
    private Long paidAmount = 0L;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL)
    private List<SaleItem> items = new ArrayList<>();
}
