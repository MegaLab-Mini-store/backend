package kg.megalab.ministorebackend.debt;

import kg.megalab.ministorebackend.dto.*;
import kg.megalab.ministorebackend.entity.Client;
import kg.megalab.ministorebackend.entity.Debt;
import kg.megalab.ministorebackend.entity.DebtStatus;
import kg.megalab.ministorebackend.entity.LoyaltyLevel;
import kg.megalab.ministorebackend.exception.ClientNotFoundException;
import kg.megalab.ministorebackend.exception.DebtNotFoundException;
import kg.megalab.ministorebackend.exception.InvalidDebtException;
import kg.megalab.ministorebackend.repository.DebtRepository;
import kg.megalab.ministorebackend.service.ClientService;
import kg.megalab.ministorebackend.service.DebtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import kg.megalab.ministorebackend.repository.DebtPaymentRepository;
import kg.megalab.ministorebackend.entity.DebtPayment;
import kg.megalab.ministorebackend.exception.DebtOverpaymentException;
import kg.megalab.ministorebackend.entity.PaymentMethod;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtServiceTest {

    @Mock
    private DebtRepository debtRepository;

    @Mock
    private ClientService clientService;

    @Mock
    private DebtPaymentRepository debtPaymentRepository;

    @InjectMocks
    private DebtService debtService;

    private Client createTestClient(Long id, String name) {
        LoyaltyLevel level = LoyaltyLevel.create("Базовый", 0);
        ReflectionTestUtils.setField(level, "id", 1L);
        Client client = Client.create("CL-100" + id, name, "+99655500000" + id, level);
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }

    private Debt createTestDebt(Long id, Client client, Long saleId, long initial, long remaining, DebtStatus status) {
        Debt debt = Debt.create(client, saleId, initial, LocalDate.of(2026, 12, 10));
        ReflectionTestUtils.setField(debt, "id", id);
        ReflectionTestUtils.setField(debt, "remainingAmount", remaining);
        ReflectionTestUtils.setField(debt, "status", status);
        ReflectionTestUtils.setField(debt, "createdAt", OffsetDateTime.now());
        return debt;
    }

    @Test
    @DisplayName("Ручная запись долга сохраняет сумму, дату и клиента")
    void createManualDebt_Success_ReturnsDebtResponse() {
        Client client = createTestClient(1L, "Азамат");
        LocalDate reminder = LocalDate.of(2026, 12, 5);
        DebtCreateRequest request = new DebtCreateRequest(1L, 15000L, reminder);

        when(clientService.findActiveClientById(1L)).thenReturn(client);
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> {
            Debt saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            ReflectionTestUtils.setField(saved, "createdAt", OffsetDateTime.now());
            return saved;
        });

        DebtResponse response = debtService.createManualDebt(request);

        assertEquals(10L, response.id());
        assertEquals(1L, response.clientId());
        assertEquals("Азамат", response.clientName());
        assertNull(response.saleId());
        assertEquals(15000L, response.initialAmount());
        assertEquals(15000L, response.remainingAmount());
        assertEquals("open", response.status());
        assertEquals(reminder, response.reminderDate());
        assertNotNull(response.createdAt());
        assertTrue(response.payments().isEmpty());
        assertTrue(response.reminders().isEmpty());
    }

    @Test
    @DisplayName("Ручная запись долга для несуществующего клиента бросает ClientNotFoundException")
    void createManualDebt_WhenClientNotFound_ThrowsException() {
        DebtCreateRequest request = new DebtCreateRequest(999L, 5000L, null);
        when(clientService.findActiveClientById(999L)).thenThrow(new ClientNotFoundException(999L));

        assertThrows(ClientNotFoundException.class, () -> debtService.createManualDebt(request));
        verify(debtRepository, never()).save(any());
    }

    @Test
    @DisplayName("Создание долга по продаже привязывает saleId и предотвращает дубликат по saleId")
    void createDebtForSale_SuccessAndDuplicateCheck() {
        Client client = createTestClient(2L, "Азамат");

        when(debtRepository.existsBySaleId(55L)).thenReturn(false);
        when(debtRepository.save(any(Debt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Debt created = debtService.createDebtForSale(client, 55L, 32000L, null);
        assertEquals(55L, created.getSaleId());
        assertEquals(32000L, created.getInitialAmount());

        when(debtRepository.existsBySaleId(55L)).thenReturn(true);
        assertThrows(
                InvalidDebtException.class,
                () -> debtService.createDebtForSale(client, 55L, 32000L, null)
        );
    }

    @Test
    @DisplayName("Книга долгов фильтрует по статусу (open, all) и возвращает список")
    void getDebts_FiltersByStatusAndClient_ReturnsPage() {
        Client client = createTestClient(1L, "Азамат");
        Debt debt = createTestDebt(3L, client, 20L, 18000L, 10000L, DebtStatus.OPEN);
        Page<Debt> page = new PageImpl<>(List.of(debt), new OffsetBasedPageRequest(0, 50), 1L);

        when(debtRepository.findAllFiltered(eq(DebtStatus.OPEN), eq(1L), any(Pageable.class)))
                .thenReturn(page);
        when(debtRepository.findAllFiltered(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<DebtSummaryResponse> openPage = debtService.getDebts("open", 1L, 50, 0);
        assertEquals(1L, openPage.total());
        assertEquals(1, openPage.items().size());
        assertEquals("Азамат", openPage.items().getFirst().clientName());
        assertEquals(10000L, openPage.items().getFirst().remainingAmount());
        assertEquals("open", openPage.items().getFirst().status());

        PageResponse<DebtSummaryResponse> allPage = debtService.getDebts("all", null, 50, 0);
        assertEquals(1L, allPage.total());
    }

    @Test
    @DisplayName("Некорректный фильтр status в книге долгов бросает InvalidDebtException (422)")
    void getDebts_WithInvalidStatus_ThrowsInvalidDebtException() {
        InvalidDebtException ex = assertThrows(
                InvalidDebtException.class,
                () -> debtService.getDebts("unknown_status", null, 50, 0)
        );
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verifyNoInteractions(debtRepository);
    }

    @Test
    @DisplayName("Получение карточки долга по ID возвращает детальную информацию или 404 DEBT_NOT_FOUND")
    void getDebtById_FoundAndNotFound() {
        Client client = createTestClient(1L, "Азамат");
        Debt debt = createTestDebt(7L, client, null, 9000L, 9000L, DebtStatus.OPEN);

        when(debtRepository.findWithClientById(7L)).thenReturn(Optional.of(debt));
        when(debtRepository.findWithClientById(404L)).thenReturn(Optional.empty());

        DebtResponse response = debtService.getDebtById(7L);
        assertEquals(7L, response.id());
        assertEquals("Азамат", response.clientName());

        DebtNotFoundException ex = assertThrows(
                DebtNotFoundException.class,
                () -> debtService.getDebtById(404L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("DEBT_NOT_FOUND", ex.getBody().getProperties().get("errorCode"));
    }

    @Test
    @DisplayName("История долгов клиента возвращает список долгов активного клиента")
    void getClientDebts_ReturnsClientDebtsPage() {
        Client client = createTestClient(4L, "Азамат");
        Debt debt = createTestDebt(12L, client, 30L, 14000L, 7000L, DebtStatus.OPEN);
        Page<Debt> page = new PageImpl<>(List.of(debt), new OffsetBasedPageRequest(0, 50), 1L);

        when(clientService.findActiveClientById(4L)).thenReturn(client);
        when(debtRepository.findAllByClientIdOrderByCreatedAtDescIdDesc(eq(4L), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<DebtSummaryResponse> result = debtService.getClientDebts(4L, 50, 0);

        assertEquals(1L, result.total());
        assertEquals(12L, result.items().getFirst().id());
        assertEquals(7000L, result.items().getFirst().remainingAmount());
    }

    @Test
    @DisplayName("Частичное погашение наличными уменьшает остаток долга")
    void payDebt_PartialCashPayment_Success() {
        Client client = createTestClient(1L, "Азамат");

        Debt debt = createTestDebt(
                10L, client, null, 15000L, 15000L, DebtStatus.OPEN
        );

        DebtPaymentRequest request = new DebtPaymentRequest(
                5000L, "cash", "payment-001", null
        );

        when(debtRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(debt));

        when(debtPaymentRepository.findByClientUuid("payment-001"))
                .thenReturn(Optional.empty());

        when(debtPaymentRepository.save(any(DebtPayment.class)))
                .thenAnswer(invocation -> {
                    DebtPayment payment = invocation.getArgument(0);
                    ReflectionTestUtils.setField(payment, "id", 1L);
                    ReflectionTestUtils.setField(
                            payment, "createdAt", OffsetDateTime.now()
                    );
                    return payment;
                });

        DebtPaymentResponse response = debtService.payDebt(10L, request);

        assertEquals(1L, response.id());
        assertEquals(10L, response.debtId());
        assertEquals(5000L, response.amount());
        assertEquals("cash", response.paymentMethod());
        assertFalse(response.idempotentReplay());

        assertEquals(10000L, debt.getRemainingAmount());
        assertEquals(DebtStatus.OPEN, debt.getStatus());

        verify(debtPaymentRepository).lockByClientUuid("payment-001");
        verify(debtPaymentRepository).save(any(DebtPayment.class));
    }

    @Test
    @DisplayName("Полное погашение картой закрывает долг")
    void payDebt_FullCardPayment_Success() {
        Client client = createTestClient(1L, "Азамат");

        Debt debt = createTestDebt(
                10L, client, null, 15000L, 15000L, DebtStatus.OPEN
        );

        DebtPaymentRequest request = new DebtPaymentRequest(
                15000L, "card", "payment-002", null
        );

        when(debtRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(debt));

        when(debtPaymentRepository.findByClientUuid("payment-002"))
                .thenReturn(Optional.empty());

        when(debtPaymentRepository.save(any(DebtPayment.class)))
                .thenAnswer(invocation -> {
                    DebtPayment payment = invocation.getArgument(0);
                    ReflectionTestUtils.setField(payment, "id", 2L);
                    ReflectionTestUtils.setField(
                            payment, "createdAt", OffsetDateTime.now()
                    );
                    return payment;
                });

        DebtPaymentResponse response = debtService.payDebt(10L, request);

        assertEquals(2L, response.id());
        assertEquals(10L, response.debtId());
        assertEquals(15000L, response.amount());
        assertEquals("card", response.paymentMethod());
        assertFalse(response.idempotentReplay());

        assertEquals(0L, debt.getRemainingAmount());
        assertEquals(DebtStatus.CLOSED, debt.getStatus());

        verify(debtPaymentRepository).lockByClientUuid("payment-002");
        verify(debtPaymentRepository).save(any(DebtPayment.class));
    }

    @Test
    @DisplayName("Погашение сверх остатка долга запрещено")
    void payDebt_Overpayment_ThrowsException() {
        Client client = createTestClient(1L, "Азамат");

        Debt debt = createTestDebt(
                10L, client, null, 15000L, 10000L, DebtStatus.OPEN
        );

        DebtPaymentRequest request = new DebtPaymentRequest(
                12000L, "cash", null, null
        );

        when(debtRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(debt));

        DebtOverpaymentException ex = assertThrows(
                DebtOverpaymentException.class,
                () -> debtService.payDebt(10L, request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(
                "DEBT_OVERPAYMENT",
                ex.getBody().getProperties().get("errorCode")
        );

        assertEquals(10000L, debt.getRemainingAmount());
        assertEquals(DebtStatus.OPEN, debt.getStatus());

        verify(debtPaymentRepository, never())
                .save(any(DebtPayment.class));
    }

    @Test
    @DisplayName("Нулевая и отрицательная суммы погашения запрещены")
    void payDebt_InvalidAmount_ThrowsException() {
        Client client = createTestClient(1L, "Азамат");

        Debt debt = createTestDebt(
                10L, client, null, 15000L, 10000L, DebtStatus.OPEN
        );

        when(debtRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(debt));

        for (long amount : new long[]{0L, -5000L}) {
            DebtPaymentRequest request = new DebtPaymentRequest(
                    amount, "cash", null, null
            );

            InvalidDebtException ex = assertThrows(
                    InvalidDebtException.class,
                    () -> debtService.payDebt(10L, request)
            );

            assertEquals(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    ex.getStatusCode()
            );

            assertEquals(
                    "VALIDATION_ERROR",
                    ex.getBody().getProperties().get("errorCode")
            );
        }

        assertEquals(10000L, debt.getRemainingAmount());
        assertEquals(DebtStatus.OPEN, debt.getStatus());

        verify(debtPaymentRepository, never())
                .save(any(DebtPayment.class));
    }

    @Test
    @DisplayName("Некорректный способ погашения долга запрещён")
    void payDebt_InvalidPaymentMethod_ThrowsException() {
        Client client = createTestClient(1L, "Азамат");

        Debt debt = createTestDebt(
                10L, client, null, 15000L, 10000L, DebtStatus.OPEN
        );

        when(debtRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(debt));

        for (String method : new String[]{"debt", "unknown", "", null}) {
            DebtPaymentRequest request = new DebtPaymentRequest(
                    5000L, method, null, null
            );

            InvalidDebtException ex = assertThrows(
                    InvalidDebtException.class,
                    () -> debtService.payDebt(10L, request)
            );

            assertEquals(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    ex.getStatusCode()
            );

            assertEquals(
                    "VALIDATION_ERROR",
                    ex.getBody().getProperties().get("errorCode")
            );
        }

        assertEquals(10000L, debt.getRemainingAmount());
        assertEquals(DebtStatus.OPEN, debt.getStatus());

        verify(debtPaymentRepository, never())
                .save(any(DebtPayment.class));
    }

    @Test
    @DisplayName("Погашение несуществующего долга возвращает ошибку 404")
    void payDebt_DebtNotFound_ThrowsException() {
        DebtPaymentRequest request = new DebtPaymentRequest(
                5000L, "cash", null, null
        );

        when(debtRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        DebtNotFoundException ex = assertThrows(
                DebtNotFoundException.class,
                () -> debtService.payDebt(999L, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());

        assertEquals(
                "DEBT_NOT_FOUND",
                ex.getBody().getProperties().get("errorCode")
        );

        verifyNoInteractions(debtPaymentRepository);
    }

    @Test
    @DisplayName("Повторный платёж не уменьшает остаток долга повторно")
    void payDebt_IdempotentReplay_ReturnsExistingPayment() {
        Client client = createTestClient(1L, "Азамат");

        Debt debt = createTestDebt(
                10L, client, null, 15000L, 10000L, DebtStatus.OPEN
        );

        DebtPayment existingPayment = DebtPayment.create(
                debt, "payment-003", 5000L, PaymentMethod.CASH, null
        );

        ReflectionTestUtils.setField(existingPayment, "id", 3L);
        ReflectionTestUtils.setField(
                existingPayment, "createdAt", OffsetDateTime.now()
        );

        DebtPaymentRequest request = new DebtPaymentRequest(
                5000L, "cash", "payment-003", null
        );

        when(debtRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(debt));

        when(debtPaymentRepository.findByClientUuid("payment-003"))
                .thenReturn(Optional.of(existingPayment));

        DebtPaymentResponse response = debtService.payDebt(10L, request);

        assertEquals(3L, response.id());
        assertEquals(10L, response.debtId());
        assertEquals(5000L, response.amount());
        assertEquals("cash", response.paymentMethod());
        assertTrue(response.idempotentReplay());

        assertEquals(10000L, debt.getRemainingAmount());
        assertEquals(DebtStatus.OPEN, debt.getStatus());

        verify(debtPaymentRepository).lockByClientUuid("payment-003");

        verify(debtPaymentRepository, never())
                .save(any(DebtPayment.class));
    }

    @Test
    @DisplayName("Повторный ключ с другими данными платежа запрещён")
    void payDebt_IdempotencyConflict_ThrowsException() {
        Client client = createTestClient(1L, "Азамат");

        Debt debt = createTestDebt(
                10L, client, null, 15000L, 10000L, DebtStatus.OPEN
        );

        DebtPayment existingPayment = DebtPayment.create(
                debt, "payment-004", 5000L, PaymentMethod.CASH, null
        );

        when(debtRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(debt));

        when(debtPaymentRepository.findByClientUuid("payment-004"))
                .thenReturn(Optional.of(existingPayment));

        List<DebtPaymentRequest> requests = List.of(
                new DebtPaymentRequest(
                        3000L, "cash", "payment-004", null
                ),
                new DebtPaymentRequest(
                        5000L, "card", "payment-004", null
                )
        );

        for (DebtPaymentRequest request : requests) {
            InvalidDebtException ex = assertThrows(
                    InvalidDebtException.class,
                    () -> debtService.payDebt(10L, request)
            );

            assertEquals(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    ex.getStatusCode()
            );

            assertEquals(
                    "VALIDATION_ERROR",
                    ex.getBody().getProperties().get("errorCode")
            );
        }

        assertEquals(10000L, debt.getRemainingAmount());
        assertEquals(DebtStatus.OPEN, debt.getStatus());

        verify(debtPaymentRepository, never())
                .save(any(DebtPayment.class));
    }

    @Test
    @DisplayName("Карточка долга возвращает историю погашений")
    void getDebtById_ReturnsPaymentHistory() {
        Client client = createTestClient(1L, "Азамат");

        Debt debt = createTestDebt(
                10L, client, null, 15000L, 7000L, DebtStatus.OPEN
        );

        DebtPayment firstPayment = DebtPayment.create(
                debt, "payment-005", 5000L, PaymentMethod.CASH, null
        );

        DebtPayment secondPayment = DebtPayment.create(
                debt, "payment-006", 3000L, PaymentMethod.CARD, null
        );

        ReflectionTestUtils.setField(firstPayment, "id", 1L);
        ReflectionTestUtils.setField(secondPayment, "id", 2L);

        OffsetDateTime now = OffsetDateTime.now();

        ReflectionTestUtils.setField(
                firstPayment, "createdAt", now.minusDays(1)
        );

        ReflectionTestUtils.setField(
                secondPayment, "createdAt", now
        );

        when(debtRepository.findWithClientById(10L))
                .thenReturn(Optional.of(debt));

        when(debtPaymentRepository
                .findAllByDebtIdOrderByCreatedAtDescIdDesc(10L))
                .thenReturn(List.of(secondPayment, firstPayment));

        DebtResponse response = debtService.getDebtById(10L);

        assertEquals(10L, response.id());
        assertEquals(7000L, response.remainingAmount());
        assertEquals(2, response.payments().size());

        DebtPaymentResponse latest =
                (DebtPaymentResponse) response.payments().get(0);

        DebtPaymentResponse oldest =
                (DebtPaymentResponse) response.payments().get(1);

        assertEquals(2L, latest.id());
        assertEquals(3000L, latest.amount());
        assertEquals("card", latest.paymentMethod());

        assertEquals(1L, oldest.id());
        assertEquals(5000L, oldest.amount());
        assertEquals("cash", oldest.paymentMethod());

        assertFalse(latest.idempotentReplay());
        assertFalse(oldest.idempotentReplay());

        verify(debtPaymentRepository)
                .findAllByDebtIdOrderByCreatedAtDescIdDesc(10L);
    }


}