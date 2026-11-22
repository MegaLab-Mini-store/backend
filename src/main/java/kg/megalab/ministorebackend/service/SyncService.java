package kg.megalab.ministorebackend.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kg.megalab.ministorebackend.dto.CompleteSaleRequest;
import kg.megalab.ministorebackend.dto.CompleteSaleResult;
import kg.megalab.ministorebackend.dto.SyncOperationRequest;
import kg.megalab.ministorebackend.dto.SyncOperationResponse;
import kg.megalab.ministorebackend.dto.SyncPushRequest;
import kg.megalab.ministorebackend.dto.SyncPushResponse;
import kg.megalab.ministorebackend.entity.*;
import kg.megalab.ministorebackend.exception.SaleRejectedException;
import kg.megalab.ministorebackend.repository.DebtRepository;
import kg.megalab.ministorebackend.repository.SaleRepository;
import kg.megalab.ministorebackend.repository.SyncOperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import kg.megalab.ministorebackend.exception.InvalidSyncRequestException;

@Service
@RequiredArgsConstructor
public class SyncService {

    private static final String OP_SALE = "sale";
    private static final List<String> KNOWN_OPS = List.of("sale", "delivery", "debt_payment");

    private final SyncOperationRepository syncRepository;
    private final SaleRepository saleRepository;
    private final DebtRepository debtRepository;
    private final SaleService saleService;
    private final SyncSalePayloadConverter saleConverter;
    private final PayloadHasher payloadHasher;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SyncPushResponse push(Long userId, SyncPushRequest request) {
        if (request.operations() == null || request.operations().isEmpty()
                || request.operations().size() > 200) {
            throw new InvalidSyncRequestException("Нужно от 1 до 200 операций");
        }
        List<SyncOperationResponse> results = new ArrayList<>();
        for (SyncOperationRequest operation : request.operations()) {
            results.add(processOne(userId, operation));
        }
        return new SyncPushResponse(results);
    }

    private SyncOperationResponse processOne(Long userId, SyncOperationRequest operation) {
        String clientUuid = operation.clientUuid();

        if (clientUuid == null || clientUuid.length() < 8 || clientUuid.length() > 64) {
            return SyncOperationResponse.rejected(clientUuid, "VALIDATION_ERROR",
                    "client_uuid должен быть от 8 до 64 символов");
        }
        if (operation.op() == null || !KNOWN_OPS.contains(operation.op()) || operation.payload() == null) {
            return SyncOperationResponse.rejected(clientUuid, "VALIDATION_ERROR",
                    "Неизвестный тип операции или пустые данные");
        }
        if (!OP_SALE.equals(operation.op())) {
            return SyncOperationResponse.rejected(clientUuid, "OPERATION_NOT_SUPPORTED",
                    "Этот тип операции пока не поддерживается");
        }

        String hash = payloadHasher.hash(operation.op(), operation.payload());

        Optional<SyncOperationResponse> repeated = checkRepeat(clientUuid, hash);
        if (repeated.isPresent()) {
            return repeated.get();
        }

        try {
            return transactionTemplate.execute(status -> applySale(userId, operation, hash));
        } catch (SaleRejectedException e) {
            transactionTemplate.executeWithoutResult(status -> syncRepository.save(
                    SyncOperation.rejected(clientUuid, operation.op(), hash, toJson(operation.payload()),
                            e.getErrorCode(), e.getMessage(), now())));
            return SyncOperationResponse.rejected(clientUuid, e.getErrorCode(), e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return checkRepeat(clientUuid, hash).orElseThrow(() -> e);
        }
    }

    private SyncOperationResponse applySale(Long userId, SyncOperationRequest operation, String hash) {
        CompleteSaleRequest saleRequest = saleConverter.toRequest(operation.clientUuid(), operation.payload());
        CompleteSaleResult result = saleService.completeSale(userId, saleRequest);
        syncRepository.save(SyncOperation.accepted(operation.clientUuid(), operation.op(), hash,
                toJson(operation.payload()), result.saleId(), now()));
        return SyncOperationResponse.applied(operation.clientUuid(), result);
    }

    private Optional<SyncOperationResponse> checkRepeat(String clientUuid, String hash) {
        return syncRepository.findByClientUuid(clientUuid).map(saved -> {
            if (!saved.getPayloadHash().equals(hash)) {
                return SyncOperationResponse.rejected(clientUuid, "IDEMPOTENCY_PAYLOAD_MISMATCH",
                        "Этот номер операции уже использован с другими данными");
            }
            if (saved.getStatus() == SyncRecordStatus.REJECTED) {
                return SyncOperationResponse.rejected(clientUuid, saved.getErrorCode(), saved.getErrorMessage());
            }
            Sale sale = saleRepository.findByClientUuid(clientUuid)
                    .orElseThrow(() -> new IllegalStateException("Нет продажи для " + clientUuid));
            Long debtId = sale.getPaymentMethod() == PaymentMethod.DEBT
                    ? debtRepository.findBySaleId(sale.getId()).map(Debt::getId).orElse(null)
                    : null;
            return SyncOperationResponse.duplicate(clientUuid,
                    new CompleteSaleResult(sale.getId(), sale.getTotalAmount(), sale.getEarnedBonuses(), debtId));
        });
    }

    private String toJson(Map<String, Object> payload) {
        return objectMapper.writeValueAsString(payload);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
