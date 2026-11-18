package kg.megalab.ministorebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SyncOperationResponse(
        @JsonProperty("client_uuid") String clientUuid,
        SyncStatus status,
        @JsonProperty("server_id") Long serverId,
        @JsonProperty("error_code") String errorCode,
        String message,
        @JsonProperty("debt_id") Long debtId,
        @JsonProperty("total_amount") Long totalAmount,
        @JsonProperty("earned_bonuses") Long earnedBonuses
) {

    public static SyncOperationResponse applied(String clientUuid, CompleteSaleResult sale) {
        return new SyncOperationResponse(clientUuid, SyncStatus.APPLIED, sale.saleId(),
                null, null, null, sale.totalAmount(), sale.earnedBonuses());
    }

    public static SyncOperationResponse duplicate(String clientUuid, CompleteSaleResult sale) {
        return new SyncOperationResponse(clientUuid, SyncStatus.DUPLICATE, sale.saleId(),
                null, null, null, sale.totalAmount(), sale.earnedBonuses());
    }

    public static SyncOperationResponse rejected(String clientUuid, String errorCode, String message) {
        return new SyncOperationResponse(clientUuid, SyncStatus.REJECTED,
                null, errorCode, message, null, null, null);
    }
}
