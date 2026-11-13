package kg.megalab.ministorebackend.dto;

import kg.megalab.ministorebackend.entity.Client;

import java.time.OffsetDateTime;

public record ClientResponse(
        Long id,
        String clientCode,
        String name,
        String phone,
        Long telegramId,
        LoyaltyLevelResponse level,
        Long bonusBalance,
        Long totalDebt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Boolean isActive
) {
    public static ClientResponse from(Client client, Long totalDebt) {
        return new ClientResponse(
                client.getId(),
                client.getClientCode(),
                client.getName(),
                client.getPhone(),
                client.getTelegramId(),
                LoyaltyLevelResponse.from(client.getLoyaltyLevel()),
                client.getBonusBalance(),
                totalDebt,
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getIsActive()
        );
    }
}
