package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.*;
import kg.megalab.ministorebackend.entity.Client;
import kg.megalab.ministorebackend.entity.Debt;
import kg.megalab.ministorebackend.entity.DebtStatus;
import kg.megalab.ministorebackend.exception.DebtNotFoundException;
import kg.megalab.ministorebackend.exception.InvalidDebtException;
import kg.megalab.ministorebackend.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final ClientService clientService;

    @Transactional
    public DebtResponse createManualDebt(DebtCreateRequest request) {
        Client client = clientService.findActiveClientById(request.clientId());
        Debt debt = Debt.create(client, null, request.amount(), request.reminderDate());
        Debt saved = debtRepository.save(debt);
        return DebtResponse.from(saved);
    }

    @Transactional
    public Debt createDebtForSale(Client client, Long saleId, long amount, LocalDate reminderDate) {
        if (saleId != null && debtRepository.existsBySaleId(saleId)) {
            throw new InvalidDebtException("По продаже с ID " + saleId + " долг уже зафиксирован");
        }
        Debt debt = Debt.create(client, saleId, amount, reminderDate);
        return debtRepository.save(debt);
    }

    @Transactional(readOnly = true)
    public PageResponse<DebtSummaryResponse> getDebts(String statusParam, Long clientId, int limit, int offset) {
        DebtStatus statusFilter = parseStatusFilter(statusParam);
        OffsetBasedPageRequest pageable = new OffsetBasedPageRequest(offset, limit);

        Page<Debt> debtsPage = debtRepository.findAllFiltered(statusFilter, clientId, pageable);
        List<DebtSummaryResponse> items = debtsPage.getContent().stream()
                .map(DebtSummaryResponse::from)
                .toList();

        return new PageResponse<>(items, debtsPage.getTotalElements(), limit, offset);
    }

    @Transactional(readOnly = true)
    public DebtResponse getDebtById(Long debtId) {
        Debt debt = debtRepository.findWithClientById(debtId)
                .orElseThrow(() -> new DebtNotFoundException(debtId));
        return DebtResponse.from(debt);
    }

    @Transactional(readOnly = true)
    public PageResponse<DebtSummaryResponse> getClientDebts(Long clientId, int limit, int offset) {
        clientService.findActiveClientById(clientId);

        OffsetBasedPageRequest pageable = new OffsetBasedPageRequest(offset, limit);
        Page<Debt> debtsPage = debtRepository.findAllByClientIdOrderByCreatedAtDescIdDesc(clientId, pageable);

        List<DebtSummaryResponse> items = debtsPage.getContent().stream()
                .map(DebtSummaryResponse::from)
                .toList();

        return new PageResponse<>(items, debtsPage.getTotalElements(), limit, offset);
    }

    private DebtStatus parseStatusFilter(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return DebtStatus.OPEN;
        }
        String normalized = statusParam.trim().toLowerCase(Locale.ROOT);
        if ("all".equals(normalized)) {
            return null;
        }
        return switch (normalized) {
            case "open" -> DebtStatus.OPEN;
            case "closed" -> DebtStatus.CLOSED;
            case "voided" -> DebtStatus.VOIDED;
            default -> throw new InvalidDebtException(
                    "Некорректный статус долга '" + statusParam + "'. Допустимые значения: open, closed, voided, all"
            );
        };
    }
}