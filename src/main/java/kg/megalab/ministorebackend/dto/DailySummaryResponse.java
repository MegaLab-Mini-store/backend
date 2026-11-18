package kg.megalab.ministorebackend.dto;

import java.time.LocalDate;
import java.util.List;

public record DailySummaryResponse(
        LocalDate date,
        long salesTotal,
        long salesCount,
        long purchasesTotal,
        long deliveriesCount,
        List<ProductDayMovement> products) {}