package kg.megalab.ministorebackend.dto;

import kg.megalab.ministorebackend.entity.Debt;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DebtResponse(
        Long id,
        Long clientId,
        String clientName,
        Long saleId,
        Long initialAmount,
        Long remainingAmount,
        String status,
        LocalDate reminderDate,
        OffsetDateTime createdAt,
        List<Object> payments,
        List<Object> reminders
) {
    public static DebtResponse from(Debt debt) {
        return new DebtResponse(
                debt.getId(),
                debt.getClient().getId(),
                debt.getClient().getName(),
                debt.getSaleId(),
                debt.getInitialAmount(),
                debt.getRemainingAmount(),
                debt.getStatus().name().toLowerCase(),
                debt.getReminderDate(),
                debt.getCreatedAt(),
                List.of(),
                List.of()
        );
    }
}
