package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "client_code", nullable = false, unique = true)
    private String clientCode;

    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    @Column(name = "name")
    private String name;

    @Column(name = "phone")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loyalty_level_id", nullable = false)
    private LoyaltyLevel loyaltyLevel;

    @PositiveOrZero
    @Column(name = "bonus_balance", nullable = false)
    private Long bonusBalance = 0L;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime updatedAt;


    public static Client create(String clientCode, String name, String phone, LoyaltyLevel loyaltyLevel) {
        Client client = new Client();
        client.clientCode = clientCode;
        client.name = name;
        client.phone = phone;
        client.loyaltyLevel = loyaltyLevel;
        return client;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
