package kg.megalab.ministorebackend.dto;

import java.util.List;

public record SyncPushResponse(List<SyncOperationResponse> results) {
}
