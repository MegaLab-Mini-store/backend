package kg.megalab.ministorebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CompleteSaleItemRequest(
        @JsonProperty("product_id") @NotNull Long productId,
        @NotNull @Positive Integer quantity,
        @JsonProperty("unit_price") @NotNull @PositiveOrZero Long unitPrice
) {}
