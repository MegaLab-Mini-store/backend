package kg.megalab.ministorebackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sync_operations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_uuid", nullable = false, unique = true, length = 64)
    private String clientUuid;

    @Column(name = "operation_type", nullable = false)
    private String operationType;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SyncRecordStatus status;

    @Column(name = "server_record_id")
    private Long serverRecordId;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public static SyncOperation accepted(String clientUuid, String operationType, String payloadHash,
                                         String payload, Long serverRecordId, OffsetDateTime processedAt) {
        SyncOperation operation = base(clientUuid, operationType, payloadHash, payload, processedAt);
        operation.status = SyncRecordStatus.ACCEPTED;
        operation.serverRecordId = serverRecordId;
        return operation;
    }

    public static SyncOperation rejected(String clientUuid, String operationType, String payloadHash,
                                         String payload, String errorCode, String errorMessage,
                                         OffsetDateTime processedAt) {
        SyncOperation operation = base(clientUuid, operationType, payloadHash, payload, processedAt);
        operation.status = SyncRecordStatus.REJECTED;
        operation.errorCode = errorCode;
        operation.errorMessage = errorMessage;
        return operation;
    }

    private static SyncOperation base(String clientUuid, String operationType, String payloadHash,
                                      String payload, OffsetDateTime processedAt) {
        SyncOperation operation = new SyncOperation();
        operation.clientUuid = clientUuid;
        operation.operationType = operationType;
        operation.payloadHash = payloadHash;
        operation.payload = payload;
        operation.processedAt = processedAt;
        return operation;
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
