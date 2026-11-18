package kg.megalab.ministorebackend.dto;

public record DeliveryItemResponse(
        Long productId,
        String productName,
        int quantity,
        long purchasePrice,
        int stockQuantityAfter
) {}
