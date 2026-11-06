package kg.megalab.ministorebackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SyncStatus {
    @JsonProperty("applied") APPLIED,
    @JsonProperty("duplicate") DUPLICATE,
    @JsonProperty("rejected") REJECTED
}
