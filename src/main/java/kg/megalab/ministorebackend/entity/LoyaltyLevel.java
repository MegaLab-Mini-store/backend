package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loyalty_levels")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoyaltyLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @PositiveOrZero
    @Column(name = "cashback_percentage", nullable = false)
    private Integer cashbackPercentage;


    public static LoyaltyLevel create(String name, Integer cashbackPercentage) {
        LoyaltyLevel loyaltyLevel = new LoyaltyLevel();
        loyaltyLevel.name = name;
        loyaltyLevel.cashbackPercentage = cashbackPercentage;
        return loyaltyLevel;
    }
}
