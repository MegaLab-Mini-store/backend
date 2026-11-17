package kg.megalab.ministorebackend.dto;

import java.time.LocalDate;
import java.util.List;

public record ProductMovementsResponse(
        LocalDate date,
        Long productId,
        String productName,
        int incoming,
        int outgoing,
        List<StockMovementResponse> movements) {}