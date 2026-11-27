package kg.megalab.ministorebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import kg.megalab.ministorebackend.entity.DebtPayment;

import java.time.OffsetDateTime;
import java.util.Locale;

public record DebtPaymentResponse(

        Long id,

        @JsonProperty("debt_id")
        Long debtId,

        Long amount,

        @JsonProperty("payment_method")
        String paymentMethod,

        @JsonProperty("created_at")
        OffsetDateTime createdAt,

        @JsonProperty("idempotent_replay")
        boolean idempotentReplay
) {

    public static DebtPaymentResponse from(
            DebtPayment payment,
            boolean idempotentReplay
    ) {
        return new DebtPaymentResponse(
                payment.getId(),
                payment.getDebt().getId(),
                payment.getAmount(),
                payment.getPaymentMethod().name()
                        .toLowerCase(Locale.ROOT),
                payment.getCreatedAt(),
                idempotentReplay
        );
    }
}