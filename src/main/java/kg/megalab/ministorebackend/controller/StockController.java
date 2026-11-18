package kg.megalab.ministorebackend.controller;

import kg.megalab.ministorebackend.dto.StockMovementResponse;
import kg.megalab.ministorebackend.service.StockQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
public class StockController {

    private final StockQueryService service;

    public StockController(StockQueryService service) {
        this.service = service;
    }

    @GetMapping("/{productId}/stock")
    public Map<String, Object> stock(@PathVariable Long productId) {
        return Map.of("productId", productId, "stockQuantity", service.currentStock(productId));
    }

    @GetMapping("/{productId}/movements")
    public List<StockMovementResponse> movements(@PathVariable Long productId,
                                                 @RequestParam(defaultValue = "50") int limit) {
        return service.movements(productId, limit);
    }
}