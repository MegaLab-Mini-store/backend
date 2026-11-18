package kg.megalab.ministorebackend.dto;

import java.time.LocalDate;

public record CashSummaryResponse(
        LocalDate date,
        long cashTotal,
        long cardTotal,
        long cashSalesCount,
        long cardSalesCount,
        long debtTotal,
        long bonusSpentTotal,
        long openingBalance,
        long expectedCashInDrawer, // начальный остаток + наличные
        long grandTotal) {} // наличные + безналичные.