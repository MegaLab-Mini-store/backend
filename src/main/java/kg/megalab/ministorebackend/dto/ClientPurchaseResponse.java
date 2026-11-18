package kg.megalab.ministorebackend.dto;

import kg.megalab.ministorebackend.entity.Client;
import kg.megalab.ministorebackend.entity.PaymentMethod;
import kg.megalab.ministorebackend.entity.Sale;
import kg.megalab.ministorebackend.entity.SaleStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record ClientPurchaseResponse(
        Long id,
        String clientUuid,
        String clientCode,
        String clientName,
        Long totalAmount,
        Long bonusSpent,
        Long paidAmount,
        Long earnedBonuses,
        PaymentMethod paymentMethod,
        SaleStatus status,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt,
        List<ClientPurchaseItemResponse> items
) {
    public static ClientPurchaseResponse from(Sale sale, Client client) {
        List<ClientPurchaseItemResponse> saleItems = sale.getItems().stream()
                .map(ClientPurchaseItemResponse::from)
                .toList();

        return new ClientPurchaseResponse(
                sale.getId(),
                sale.getClientUuid(),
                client.getClientCode(),
                client.getName(),
                sale.getTotalAmount(),
                sale.getBonusSpent(),
                sale.getPaidAmount(),
                sale.getEarnedBonuses(),
                sale.getPaymentMethod(),
                sale.getStatus(),
                sale.getOccurredAt(),
                sale.getCreatedAt(),
                saleItems
        );
    }
}