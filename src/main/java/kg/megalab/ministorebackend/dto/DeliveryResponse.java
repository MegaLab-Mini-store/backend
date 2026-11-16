package kg.megalab.ministorebackend.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record DeliveryResponse(
        Long id,
        Long userId,
        String supplierInfo,
        long totalAmount,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt,
        List<DeliveryItemResponse> items
) {}
