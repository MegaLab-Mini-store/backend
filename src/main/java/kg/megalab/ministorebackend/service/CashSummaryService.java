package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.CashSummaryResponse;
import kg.megalab.ministorebackend.repository.CashSummaryRepository;
import kg.megalab.ministorebackend.repository.CashSummaryRepository.MethodTotals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class CashSummaryService {

    // Начальный остаток кассы: правило не подтверждено заказчиком, пока 0.
    private static final long OPENING_BALANCE = 0L;

    private final CashSummaryRepository repository;
    private final Clock clock;

    public CashSummaryService(CashSummaryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public CashSummaryResponse summary(LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        OffsetDateTime from = day.atStartOfDay(clock.getZone()).toOffsetDateTime();
        OffsetDateTime to = day.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime();

        MethodTotals cash = repository.byMethod("CASH", from, to);
        MethodTotals card = repository.byMethod("CARD", from, to);

        return new CashSummaryResponse(
                day,
                cash.paid(),
                card.paid(),
                cash.count(),
                card.count(),
                repository.debtTotal(from, to),
                repository.bonusSpentTotal(from, to),
                OPENING_BALANCE,
                OPENING_BALANCE + cash.paid(),
                cash.paid() + card.paid());
    }
}