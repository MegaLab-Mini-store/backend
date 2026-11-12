package kg.megalab.ministorebackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SaleItemRequest(
        @NotNull(message = "ID товара обязателен")
        Long productId,

        @NotNull(message = "Количество обязательно")
        @Min(value = 1, message = "Количество товара должно быть не меньше 1")
        Integer quantity
) {}
