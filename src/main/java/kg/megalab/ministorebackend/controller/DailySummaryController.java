package kg.megalab.ministorebackend.controller;

import kg.megalab.ministorebackend.dto.DailySummaryResponse;
import kg.megalab.ministorebackend.dto.ProductMovementsResponse;
import kg.megalab.ministorebackend.service.DailySummaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/summary")
public class DailySummaryController {

    private final DailySummaryService service;

    public DailySummaryController(DailySummaryService service) {
        this.service = service;
    }

    /** GET /api/v1/summary/daily?date=2026-11-17 (без date: сегодня) */
    @GetMapping("/daily")
    public DailySummaryResponse daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.summary(date);
    }

    /** GET /api/v1/summary/daily/products/5?date=2026-11-17 */
    @GetMapping("/daily/products/{productId}")
    public ProductMovementsResponse productMovements(
            @PathVariable Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.productMovements(productId, date);
    }
}
