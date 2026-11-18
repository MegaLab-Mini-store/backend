package kg.megalab.ministorebackend.dto;

import kg.megalab.ministorebackend.entity.SaleItem;

public record ClientPurchaseItemResponse(
        Long id,
        Long productId,
        String productName,
        Integer quantity,
        Long unitPrice,
        Long subtotal
) {
    public static ClientPurchaseItemResponse from(SaleItem item) {
        return new ClientPurchaseItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }
}
