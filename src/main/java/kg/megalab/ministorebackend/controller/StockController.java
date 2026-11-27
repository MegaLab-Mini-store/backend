package kg.megalab.ministorebackend.controller;

import kg.megalab.ministorebackend.dto.StockMovementsPage;
import kg.megalab.ministorebackend.service.StockQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /** GET /api/v1/products/5/movements?direction=IN&page=0&size=50 */
    @GetMapping("/{productId}/movements")
    public StockMovementsPage movements(@PathVariable Long productId,
                                        @RequestParam(required = false) String direction,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size) {
        return service.movements(productId, direction, page, size);
    }
}