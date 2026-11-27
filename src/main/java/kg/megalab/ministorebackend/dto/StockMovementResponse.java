package kg.megalab.ministorebackend.dto;

import java.time.OffsetDateTime;

public record StockMovementResponse(
        Long id,
        String operationType,
        String operationLabel,
        int quantityChange,
        Integer quantityAfter,
        Long referenceId,
        String reason,
        OffsetDateTime createdAt) {}