package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sale_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Positive
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @PositiveOrZero
    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @PositiveOrZero
    @Column(name = "subtotal", nullable = false)
    private Long subtotal;

    public static SaleItem of(Product product, int quantity) {
        SaleItem item = new SaleItem();
        item.product = product;
        item.quantity = quantity;
        item.unitPrice = product.getSalePrice();
        item.subtotal = item.unitPrice * quantity;
        return item;
    }

    void setSale(Sale sale) {
        this.sale = sale;
    }
}
