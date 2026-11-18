package kg.megalab.ministorebackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kg.megalab.ministorebackend.dto.SaleCalculationResponse;
import kg.megalab.ministorebackend.dto.SaleCheckoutRequest;
import kg.megalab.ministorebackend.dto.SaleItemResponse;
import kg.megalab.ministorebackend.service.SaleCheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Tag(name = "Оформление продажи", description = "Сканирование товаров в чек, пересчет сумм и проверка остатков")
public class SaleCheckoutController {

    private final SaleCheckoutService saleCheckoutService;

    @GetMapping("/scan/{barcode}")
    @Operation(
            summary = "Сканировать товар для добавления в чек",
            description = "Находит активный товар по штрихкоду, проверяет наличие на складе и возвращает данные позиции"
    )
    public ResponseEntity<SaleItemResponse> scanProduct(@PathVariable String barcode) {
        return ResponseEntity.ok(saleCheckoutService.scanProductForSale(barcode));
    }

    @PostMapping("/calculate")
    @Operation(
            summary = "Рассчитать чек и проверить достаточность остатков",
            description = "Пересчитывает суммы позиций и итоговую сумму чека, проверяет остатки всех позиций перед завершением продажи"
    )
    public ResponseEntity<SaleCalculationResponse> calculateAndValidate(
            @Valid @RequestBody SaleCheckoutRequest request
    ) {
        return ResponseEntity.ok(saleCheckoutService.calculateAndValidateReceipt(request));
    }
}
