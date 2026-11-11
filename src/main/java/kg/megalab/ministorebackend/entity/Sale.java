package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "client_uuid", unique = true)
    private String clientUuid;

    @Column(name = "replaces_client_uuid")
    private String replacesClientUuid;

    @Column(name = "total_amount", nullable = false)
    @Builder.Default
    private Long totalAmount = 0L;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "bonus_spent", nullable = false)
    @Builder.Default
    private Long bonusSpent = 0L;

    @Column(name = "paid_amount", nullable = false)
    @Builder.Default
    private Long paidAmount = 0L;

    @Column(name = "earned_bonuses", nullable = false)
    @Builder.Default
    private Long earnedBonuses = 0L;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "completed";

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;

    @Column(name = "original_occurred_at")
    private OffsetDateTime originalOccurredAt;

    @Column(name = "clock_skew_detected", nullable = false)
    @Builder.Default
    private Boolean clockSkewDetected = false;

    @Column(name = "late_sync", nullable = false)
    @Builder.Default
    private Boolean lateSync = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SaleItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.occurredAt == null) {
            this.occurredAt = this.createdAt;
        }
    }
}
