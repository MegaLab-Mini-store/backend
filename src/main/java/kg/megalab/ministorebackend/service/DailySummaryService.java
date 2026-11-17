package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.DailySummaryResponse;
import kg.megalab.ministorebackend.dto.ProductMovementsResponse;
import kg.megalab.ministorebackend.dto.StockMovementResponse;
import kg.megalab.ministorebackend.exception.ResourceNotFoundException;
import kg.megalab.ministorebackend.repository.DailySummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DailySummaryService {

    private final DailySummaryRepository repository;
    private final Clock clock;

    public DailySummaryService(DailySummaryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DailySummaryResponse summary(LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        OffsetDateTime from = start(day);
        OffsetDateTime to = start(day.plusDays(1));

        return new DailySummaryResponse(
                day,
                repository.salesTotal(from, to),
                repository.salesCount(from, to),
                repository.purchasesTotal(from, to),
                repository.deliveriesCount(from, to),
                repository.productMovements(from, to));
    }

    @Transactional(readOnly = true)
    public ProductMovementsResponse productMovements(Long productId, LocalDate date) {
        LocalDate day = date != null ? date : LocalDate.now(clock);
        String name = repository.productName(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Товар не найден (id " + productId + ")"));

        List<StockMovementResponse> movements =
                repository.movementsOfProduct(productId, start(day), start(day.plusDays(1)));

        int incoming = movements.stream().mapToInt(StockMovementResponse::quantityChange)
                .filter(q -> q > 0).sum();
        int outgoing = -movements.stream().mapToInt(StockMovementResponse::quantityChange)
                .filter(q -> q < 0).sum();

        return new ProductMovementsResponse(day, productId, name, incoming, outgoing, movements);
    }

    /** Начало дня в часовом поясе магазина (из бина Clock). */
    private OffsetDateTime start(LocalDate day) {
        return day.atStartOfDay(clock.getZone()).toOffsetDateTime();
    }
}
