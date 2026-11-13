package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.SaleCalculationResponse;
import kg.megalab.ministorebackend.dto.SaleCheckoutRequest;
import kg.megalab.ministorebackend.dto.SaleItemRequest;
import kg.megalab.ministorebackend.dto.SaleItemResponse;
import kg.megalab.ministorebackend.entity.Product;
import kg.megalab.ministorebackend.exception.InsufficientStockException;
import kg.megalab.ministorebackend.exception.ResourceNotFoundException;
import kg.megalab.ministorebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleCheckoutService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public SaleItemResponse scanProductForSale(String barcode) {
        Product product = productRepository.findByBarcodeAndIsActiveTrue(barcode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Товар со штрихкодом '" + barcode + "' не найден или неактивен"
                ));

        if (product.getStockQuantity() < 1) {
            throw new InsufficientStockException(product.getName(), 1, product.getStockQuantity());
        }

        return new SaleItemResponse(
                product.getId(),
                product.getName(),
                product.getSalePrice(),
                1,
                product.getSalePrice(),
                product.getStockQuantity()
        );
    }

    @Transactional(readOnly = true)
    public SaleCalculationResponse calculateAndValidateReceipt(SaleCheckoutRequest request) {
        Map<Long, Integer> mergedQuantities = new LinkedHashMap<>();
        for (SaleItemRequest item : request.items()) {
            mergedQuantities.merge(item.productId(), item.quantity(), Integer::sum);
        }

        Map<Long, Product> productsMap = productRepository.findAllById(mergedQuantities.keySet())
                .stream()
                .filter(Product::getIsActive)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<SaleItemResponse> calculatedItems = new ArrayList<>();
        long totalAmount = 0L;

        for (Map.Entry<Long, Integer> entry : mergedQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer requestedQty = entry.getValue();

            Product product = productsMap.get(productId);
            if (product == null) {
                throw new ResourceNotFoundException("Товар с ID " + productId + " не найден или неактивен");
            }

            if (product.getStockQuantity() < requestedQty) {
                throw new InsufficientStockException(
                        product.getName(),
                        requestedQty,
                        product.getStockQuantity()
                );
            }

            long unitPrice = product.getSalePrice();
            long subtotal = unitPrice * requestedQty;
            totalAmount += subtotal;

            calculatedItems.add(new SaleItemResponse(
                    product.getId(),
                    product.getName(),
                    unitPrice,
                    requestedQty,
                    subtotal,
                    product.getStockQuantity()
            ));
        }

        return new SaleCalculationResponse(calculatedItems, totalAmount);
    }
}
