package kg.megalab.ministorebackend.dto;

import java.time.OffsetDateTime;

public record StockMovementResponse(
        Long id,
        String operationType,
        int quantityChange,
        Integer quantityAfter,
        Long referenceId,
        String reason,
        OffsetDateTime createdAt) {}