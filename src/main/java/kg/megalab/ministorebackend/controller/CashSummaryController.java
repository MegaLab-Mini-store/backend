package kg.megalab.ministorebackend.controller;

import kg.megalab.ministorebackend.dto.CashSummaryResponse;
import kg.megalab.ministorebackend.service.CashSummaryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/summary")
public class CashSummaryController {

    private final CashSummaryService service;

    public CashSummaryController(CashSummaryService service) {
        this.service = service;
    }

    /** GET /api/v1/summary/cash?date=2026-11-17 (без date: сегодня) */
    @GetMapping("/cash")
    public CashSummaryResponse cash(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.summary(date);
    }
}