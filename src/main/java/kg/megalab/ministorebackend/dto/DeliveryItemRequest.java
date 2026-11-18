package kg.megalab.ministorebackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DeliveryItemRequest(
        @NotNull(message = "ID товара обязателен")
        @Positive(message = "ID товара должен быть положительным")
        Long productId,

        @NotNull(message = "Количество обязательно")
        @Min(value = 1, message = "Количество должно быть не меньше 1")
        Integer quantity,

        @NotNull(message = "Закупочная цена обязательна")
        @Min(value = 0, message = "Закупочная цена не может быть отрицательной")
        Long purchasePrice
) {}
