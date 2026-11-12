package kg.megalab.ministorebackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record SaleItemRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer quantity,
        @NotNull @PositiveOrZero Long unitPrice
) {}
