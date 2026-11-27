package kg.megalab.ministorebackend.dto;

import java.util.List;

public record StockMovementsPage(
        Long productId,
        String productName,
        int currentStock,
        long total,
        int page,
        int size,
        List<StockMovementResponse> items) {}