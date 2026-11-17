package kg.megalab.ministorebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank(message = "Имя клиента обязательно")
        @Size(max = 128)
        String name,
        @Size(max = 32)
        String phone,
        Long loyaltyLevelId
) {
}
