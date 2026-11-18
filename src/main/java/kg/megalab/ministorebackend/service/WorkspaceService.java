package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.*;
import kg.megalab.ministorebackend.repository.StoreStatsRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
public class WorkspaceService {
    private final StoreStatsRepository stats;
    private final Clock clock;

    public WorkspaceService(StoreStatsRepository stats, Clock clock) {
        this.stats = stats; this.clock = clock;
    }

    public WorkspaceResponse build(String cashierLogin) {
        LocalDate today = LocalDate.now(clock);
        Instant from = today.atStartOfDay(clock.getZone()).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(clock.getZone()).toInstant();

        long products = stats.countProducts();
        long customers = stats.countCustomers();
        long salesToday = stats.countSales(from, to);
        long receiptsToday = stats.countReceipts(from, to);

        List<WorkspaceSection> sections = List.of(
                section(SectionCode.SALE, "Продажа", products == 0,
                        "В каталоге пока нет товаров. Добавьте товары или примите поставку, чтобы начать продавать."),
                section(SectionCode.PRODUCTS, "Товары", products == 0,
                        "Каталог пуст. Отсканируйте штрихкод товара, чтобы добавить первую позицию."),
                section(SectionCode.RECEIVING, "Приёмка", receiptsToday == 0 && products == 0,
                        "Поставок пока нет. Отсканируйте товар, чтобы принять его на склад."),
                section(SectionCode.CUSTOMERS, "Клиенты", customers == 0,
                        "Клиентов пока нет. Они появятся, когда вы привяжете карту лояльности к первой покупке."),
                section(SectionCode.CASH_SUMMARY, "Итоговая касса", salesToday == 0,
                        "Сегодня продаж ещё не было. Итоги появятся после первой продажи.")
        );
        boolean storeEmpty = products == 0 && customers == 0 && salesToday == 0;
        return new WorkspaceResponse(cashierLogin, storeEmpty, sections);
    }

    // enabled всегда true: пустое состояние не блокирует навигацию
    private WorkspaceSection section(SectionCode code, String title, boolean empty, String msg) {
        return new WorkspaceSection(code, title, true, empty, empty ? msg : null);
    }
}