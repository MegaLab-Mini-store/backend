package kg.megalab.ministorebackend.dto;

import kg.megalab.ministorebackend.entity.Debt;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DebtSummaryResponse(
        Long id,
        Long clientId,
        String clientName,
        Long saleId,
        Long initialAmount,
        Long remainingAmount,
        String status,
        LocalDate reminderDate,
        OffsetDateTime createdAt
) {
    public static DebtSummaryResponse from(Debt debt) {
        return new DebtSummaryResponse(
                debt.getId(),
                debt.getClient().getId(),
                debt.getClient().getName(),
                debt.getSaleId(),
                debt.getInitialAmount(),
                debt.getRemainingAmount(),
                debt.getStatus().name().toLowerCase(),
                debt.getReminderDate(),
                debt.getCreatedAt()
        );
    }
}
