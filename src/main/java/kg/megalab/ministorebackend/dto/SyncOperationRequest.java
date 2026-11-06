package kg.megalab.ministorebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record SyncOperationRequest(
        @NotNull @Pattern(regexp = "sale|delivery|debt_payment") String op,
        @NotNull @Size(min = 8, max = 64) @JsonProperty("client_uuid") String clientUuid,
        @NotNull Map<String, Object> payload
) {}

