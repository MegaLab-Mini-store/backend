package kg.megalab.ministorebackend.dto;

import kg.megalab.ministorebackend.entity.LoyaltyLevel;

public record LoyaltyLevelResponse(
        Long id,
        String name,
        Integer cashbackPercentage
) {
    public static LoyaltyLevelResponse from(LoyaltyLevel level) {
        return new LoyaltyLevelResponse(
                level.getId(),
                level.getName(),
                level.getCashbackPercentage()
        );
    }
}