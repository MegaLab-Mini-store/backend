package kg.megalab.ministorebackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record DeliveryRequest(
        @NotEmpty(message = "Поставка не может быть пустой")
        List<@Valid DeliveryItemRequest> items,

        @Size(max = 255, message = "Информация о поставщике слишком длинная")
        String supplierInfo,

        OffsetDateTime occurredAt
) {}
