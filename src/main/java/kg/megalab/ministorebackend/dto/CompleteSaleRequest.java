package kg.megalab.ministorebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import kg.megalab.ministorebackend.entity.PaymentMethod;

public record CompleteSaleRequest(
        @JsonProperty("client_uuid") @NotNull @Size(min = 8, max = 64) String clientUuid,
        @JsonProperty("payment_method") @NotNull PaymentMethod paymentMethod,
        @JsonProperty("occurred_at") OffsetDateTime occurredAt,
        @JsonProperty("replaces_client_uuid") @Size(min = 8, max = 64) String replacesClientUuid,
        @NotEmpty List<@Valid CompleteSaleItemRequest> items
) {}
