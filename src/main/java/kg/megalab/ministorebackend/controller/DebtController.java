package kg.megalab.ministorebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kg.megalab.ministorebackend.dto.DebtCreateRequest;
import kg.megalab.ministorebackend.dto.DebtResponse;
import kg.megalab.ministorebackend.dto.DebtSummaryResponse;
import kg.megalab.ministorebackend.dto.PageResponse;
import kg.megalab.ministorebackend.service.DebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/debts")
@RequiredArgsConstructor
@Tag(name = "Долги", description = "Книга долгов клиентов")
public class DebtController {

    private final DebtService debtService;

    @GetMapping
    @Operation(
            summary = "Книга долгов - все должники с фильтрами",
            description = "Возвращает список долгов (краткий формат, без payments и reminders). "
                    + "Параметр status принимает: open (по умолчанию), closed, voided, all."
    )
    public ResponseEntity<PageResponse<DebtSummaryResponse>> getDebts(
            @RequestParam(defaultValue = "open") String status,
            @RequestParam(name = "client_id", required = false) Long clientId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return ResponseEntity.ok(debtService.getDebts(status, clientId, limit, offset));
    }

    @PostMapping
    @Operation(
            summary = "Записать долг вручную",
            description = "Создаёт открытую запись о долге для постоянного клиента (без привязки к чеку)"
    )
    public ResponseEntity<DebtResponse> createDebt(@Valid @RequestBody DebtCreateRequest request) {
        DebtResponse created = debtService.createManualDebt(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{debt_id}")
    @Operation(
            summary = "Долг по ID с историей погашений и напоминаниями",
            description = "Возвращает детальную карточку долга"
    )
    public ResponseEntity<DebtResponse> getDebtById(@PathVariable("debt_id") Long debtId) {
        return ResponseEntity.ok(debtService.getDebtById(debtId));
    }
}
