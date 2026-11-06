package kg.megalab.ministorebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SyncOperationResponse(
        @JsonProperty("client_uuid") String clientUuid,
        SyncStatus status,
        @JsonProperty("server_id") Integer serverId,
        @JsonProperty("error_code") String errorCode,
        String message,
        @JsonProperty("dept_id") Integer deptId
) {
}
