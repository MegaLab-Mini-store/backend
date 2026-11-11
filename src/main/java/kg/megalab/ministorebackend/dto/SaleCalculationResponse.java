package kg.megalab.ministorebackend.dto;

import java.util.List;

public record SaleCalculationResponse(
        List<SaleItemResponse> items,
        Long totalAmount
) {}
