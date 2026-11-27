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
import kg.megalab.ministorebackend.entity.DebtPayment;
import kg.megalab.ministorebackend.entity.PaymentMethod;
import kg.megalab.ministorebackend.repository.DebtPaymentRepository;
import java.util.Objects;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final ClientService clientService;
    private final DebtPaymentRepository debtPaymentRepository;

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

        List<DebtPaymentResponse> payments = debtPaymentRepository
                .findAllByDebtIdOrderByCreatedAtDescIdDesc(debtId)
                .stream()
                .map(payment -> DebtPaymentResponse.from(payment, false))
                .toList();

        return DebtResponse.from(debt, payments);
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

    @Transactional
    public DebtPaymentResponse payDebt(Long debtId, DebtPaymentRequest request) {

        if (request.clientUuid() != null) {
            debtPaymentRepository.lockByClientUuid(request.clientUuid());
        }

        Debt debt = debtRepository.findByIdForUpdate(debtId)
                .orElseThrow(() -> new DebtNotFoundException(debtId));

        if (request.amount() == null || request.amount() <= 0) {
            throw new InvalidDebtException(
                    "Сумма погашения должна быть больше нуля"
            );
        }

        if (request.paymentMethod() == null) {
            throw new InvalidDebtException(
                    "Не указан способ оплаты"
            );
        }

        PaymentMethod paymentMethod = switch (request.paymentMethod()) {
            case "cash" -> PaymentMethod.CASH;
            case "card" -> PaymentMethod.CARD;
            default -> throw new InvalidDebtException(
                    "Недопустимый способ оплаты"
            );
        };

        if (request.clientUuid() != null) {
            var existingPayment = debtPaymentRepository
                    .findByClientUuid(request.clientUuid());

            if (existingPayment.isPresent()) {
                DebtPayment payment = existingPayment.get();

                if (!Objects.equals(payment.getDebt().getId(), debtId)
                        || !Objects.equals(payment.getAmount(), request.amount())
                        || payment.getPaymentMethod() != paymentMethod) {

                    throw new InvalidDebtException(
                            "client_uuid уже используется для другого платежа"
                    );
                }

                return DebtPaymentResponse.from(payment, true);
            }
        }

        debt.applyPayment(request.amount());

        DebtPayment payment = DebtPayment.create(
                debt,
                request.clientUuid(),
                request.amount(),
                paymentMethod,
                request.occurredAt()
        );

        DebtPayment saved = debtPaymentRepository.save(payment);

        return DebtPaymentResponse.from(saved, false);
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