package kg.megalab.ministorebackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import kg.megalab.ministorebackend.entity.PaymentMethod;

public record SaleRequest(
        @NotNull @Size(min = 8, max = 64) String clientUuid,
        @NotNull PaymentMethod paymentMethod,
        OffsetDateTime occurredAt,
        @Size(min = 8, max = 64) String replacesClientUuid,
        @NotEmpty List<@Valid SaleItemRequest> items
) {}
