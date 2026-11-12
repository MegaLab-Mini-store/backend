package kg.megalab.ministorebackend.dto;

public record SaleItemResponse(
        Long productId,
        String productName,
        Long unitPrice,
        Integer quantity,
        Long subtotal,
        Integer availableStock
) {}
