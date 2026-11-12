package kg.megalab.ministorebackend.service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kg.megalab.ministorebackend.dto.SaleItemRequest;
import kg.megalab.ministorebackend.dto.SaleRequest;
import kg.megalab.ministorebackend.dto.SaleResult;
import kg.megalab.ministorebackend.entity.InventoryMovement;
import kg.megalab.ministorebackend.entity.MovementType;
import kg.megalab.ministorebackend.entity.PaymentMethod;
import kg.megalab.ministorebackend.entity.Product;
import kg.megalab.ministorebackend.entity.Sale;
import kg.megalab.ministorebackend.entity.SaleItem;
import kg.megalab.ministorebackend.exception.SaleRejectedException;
import kg.megalab.ministorebackend.repository.InventoryMovementRepository;
import kg.megalab.ministorebackend.repository.ProductRepository;
import kg.megalab.ministorebackend.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;

    @Transactional
    public SaleResult completeSale(Long userId, SaleRequest request) {
        if (request.paymentMethod() == PaymentMethod.DEBT) {
            throw new SaleRejectedException("DEBT_CLIENT_REQUIRED",
                    "Продажа в долг возможна только с клиентом");
        }

        Map<Long, Product> products = loadProducts(request);
        OffsetDateTime now = OffsetDateTime.now();

        Sale sale = new Sale();
        sale.setUserId(userId);
        sale.setClientUuid(request.clientUuid());
        sale.setReplacesClientUuid(request.replacesClientUuid());
        sale.setPaymentMethod(request.paymentMethod());
        sale.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : now);

        long total = 0;
        for (SaleItemRequest itemRequest : request.items()) {
            long subtotal = Math.multiplyExact(itemRequest.unitPrice(), itemRequest.quantity());
            total = Math.addExact(total, subtotal);

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(products.get(itemRequest.productId()));
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            item.setSubtotal(subtotal);
            sale.getItems().add(item);
        }
        sale.setTotalAmount(total);
        sale.setPaidAmount(total);

        Sale saved = saleRepository.save(sale);

        for (SaleItemRequest itemRequest : request.items()) {
            productRepository.decreaseStock(itemRequest.productId(), itemRequest.quantity(), now);

            InventoryMovement movement = new InventoryMovement();
            movement.setProduct(products.get(itemRequest.productId()));
            movement.setOperationType(MovementType.SALE);
            movement.setQuantityChange(-itemRequest.quantity());
            movement.setReferenceId(saved.getId());
            movementRepository.save(movement);
        }

        return new SaleResult(saved.getId(), saved.getTotalAmount(), saved.getEarnedBonuses());
    }

    private Map<Long, Product> loadProducts(SaleRequest request) {
        Set<Long> ids = request.items().stream()
                .map(SaleItemRequest::productId)
                .collect(Collectors.toSet());

        Map<Long, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (Long id : ids) {
            if (!products.containsKey(id)) {
                throw new SaleRejectedException("PRODUCT_NOT_FOUND", "Товар не найден: id " + id);
            }
        }
        return products;
    }
}
