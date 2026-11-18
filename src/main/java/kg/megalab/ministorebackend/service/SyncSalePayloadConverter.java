package kg.megalab.ministorebackend.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import kg.megalab.ministorebackend.dto.CompleteSaleRequest;
import kg.megalab.ministorebackend.exception.SaleRejectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class SyncSalePayloadConverter {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public CompleteSaleRequest toRequest(String clientUuid, Map<String, Object> payload) {
        Map<String, Object> data = new HashMap<>(payload);
        data.put("client_uuid", clientUuid);
        if (data.get("payment_method") instanceof String method) {
            data.put("payment_method", method.toUpperCase(Locale.ROOT));
        }

        CompleteSaleRequest request;
        try {
            request = objectMapper.convertValue(data, CompleteSaleRequest.class);
        } catch (RuntimeException e) {
            throw new SaleRejectedException("VALIDATION_ERROR", "Неверный формат данных продажи");
        }

        Set<ConstraintViolation<CompleteSaleRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            ConstraintViolation<CompleteSaleRequest> first = violations.iterator().next();
            throw new SaleRejectedException("VALIDATION_ERROR",
                    first.getPropertyPath() + ": " + first.getMessage());
        }
        return request;
    }
}
