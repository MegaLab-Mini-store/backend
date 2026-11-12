package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "barcode", unique = true)
    private String barcode;

    @Column(name = "name", nullable = false)
    private String name;

    @PositiveOrZero
    @Column(name = "purchase_price", nullable = false)
    private Long purchasePrice;

    @PositiveOrZero
    @Column(name = "sale_price", nullable = false)
    private Long salePrice;

    @Column(name = "stock_quantity", nullable = false)
    @Setter(AccessLevel.NONE)
    private Integer stockQuantity = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private OffsetDateTime updatedAt;

    public static Product create(String barcode, String name, Long purchasePrice, Long salePrice) {
        Product product = new Product();
        product.barcode = barcode;
        product.name = name;
        product.purchasePrice = purchasePrice;
        product.salePrice = salePrice;
        return product;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество для добавления должно быть больше 0");
        }
        this.stockQuantity += quantity;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество для списания должно быть больше 0");
        }
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("Недостаточно товара на складе");
        }
        this.stockQuantity -= quantity;
    }

    @PrePersist
    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}