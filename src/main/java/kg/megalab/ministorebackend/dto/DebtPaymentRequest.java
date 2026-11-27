package kg.megalab.ministorebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record DebtPaymentRequest(

        @NotNull
        @Positive
        Long amount,

        @NotNull
        @Pattern(regexp = "cash|card")
        @JsonProperty("payment_method")
        String paymentMethod,

        @Size(min = 8, max = 64)
        @JsonProperty("client_uuid")
        String clientUuid,

        @JsonProperty("occurred_at")
        OffsetDateTime occurredAt
) {
}