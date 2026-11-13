package kg.megalab.ministorebackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaleCheckoutRequest(
        @NotEmpty(message = "Чек не может быть пустым")
        @Valid
        List<SaleItemRequest> items,
        String clientCode
) {}
