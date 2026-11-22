package kg.megalab.ministorebackend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record DebtCreateRequest(
        @NotNull(message = "ID клиента обязателен")
        @Positive(message = "ID клиента должен быть положительным")
        @JsonProperty("client_id")
        @JsonAlias("clientId")
        Long clientId,

        @NotNull(message = "Сумма долга обязательна")
        @Positive(message = "Сумма долга должна быть больше нуля")
        Long amount,

        @JsonProperty("reminder_date")
        @JsonAlias("reminderDate")
        LocalDate reminderDate
) {
}
