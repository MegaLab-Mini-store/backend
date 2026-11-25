package kg.megalab.ministorebackend;

import kg.megalab.ministorebackend.dto.*;
import kg.megalab.ministorebackend.entity.*;
import kg.megalab.ministorebackend.exception.ClientNotFoundException;
import kg.megalab.ministorebackend.exception.ResourceNotFoundException;
import kg.megalab.ministorebackend.repository.ClientRepository;
import kg.megalab.ministorebackend.repository.DebtRepository;
import kg.megalab.ministorebackend.repository.LoyaltyLevelRepository;
import kg.megalab.ministorebackend.repository.SaleRepository;
import kg.megalab.ministorebackend.service.ClientService;
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

import java.util.List;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private LoyaltyLevelRepository loyaltyLevelRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private DebtRepository debtRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    @DisplayName("Создаёт клиента с минимальными данными, базовым уровнем 0% и уникальным Client ID")
    void createClient_WithMinimalData_CreatesDefaultLevelAndUniqueCode() {
        CreateClientRequest request = new CreateClientRequest("  Джон  ", "   ", null);

        LoyaltyLevel baseLevel = LoyaltyLevel.create("Базовый", 0);
        ReflectionTestUtils.setField(baseLevel, "id", 1L);

        when(loyaltyLevelRepository.findFirstByOrderByCashbackPercentageAsc())
                .thenReturn(Optional.empty());
        when(loyaltyLevelRepository.save(any(LoyaltyLevel.class)))
                .thenReturn(baseLevel);
        when(clientRepository.existsByClientCode(anyString()))
                .thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 10L);
            return saved;
        });

        ClientResponse response = clientService.createClient(request);

        assertEquals(10L, response.id());
        assertEquals("Джон", response.name());
        assertNull(response.phone());
        assertNull(response.telegramId());
        assertNotNull(response.clientCode());
        assertTrue(response.clientCode().startsWith("CL-"));
        assertEquals(9, response.clientCode().length());
        assertEquals(0L, response.bonusBalance());
        assertEquals(0L, response.totalDebt());
        assertTrue(response.isActive());
        assertEquals(1L, response.level().id());
        assertEquals("Базовый", response.level().name());
        assertEquals(0, response.level().cashbackPercentage());

        verify(loyaltyLevelRepository).save(any(LoyaltyLevel.class));
        verify(clientRepository).save(any(Client.class));
        verifyNoInteractions(debtRepository);
    }

    @Test
    @DisplayName("Перегенерирует clientCode при совпадении в БД и привязывает выбранный уровень лояльности")
    void createClient_WithPhoneAndLevel_RegeneratesCodeOnCollision() {
        CreateClientRequest request = new CreateClientRequest("Джон", " +996700112233 ", 2L);

        LoyaltyLevel silverLevel = LoyaltyLevel.create("Серебряный", 100);
        ReflectionTestUtils.setField(silverLevel, "id", 2L);

        when(loyaltyLevelRepository.findById(2L))
                .thenReturn(Optional.of(silverLevel));
        when(clientRepository.existsByClientCode(anyString()))
                .thenReturn(true, false);
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            return saved;
        });

        ClientResponse response = clientService.createClient(request);

        assertEquals(11L, response.id());
        assertEquals("Джон", response.name());
        assertEquals("+996700112233", response.phone());
        assertEquals(2L, response.level().id());
        assertEquals(100, response.level().cashbackPercentage());
        verify(clientRepository, times(2)).existsByClientCode(anyString());
    }

    @Test
    @DisplayName("Выбрасывает ResourceNotFoundException, если указанный loyaltyLevelId не найден")
    void createClient_WhenLoyaltyLevelNotFound_ThrowsException() {
        CreateClientRequest request = new CreateClientRequest("Джон", null, 999L);

        when(loyaltyLevelRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clientService.createClient(request));
        verify(clientRepository, never()).save(any());
    }

    @Test
    @DisplayName("Возвращает карточку активного клиента по ID с бонусным балансом и актуальным долгом")
    void getClientById_WhenActiveClientExists_ReturnsCardWithTotalDebt() {
        LoyaltyLevel level = LoyaltyLevel.create("Базовый", 0);
        ReflectionTestUtils.setField(level, "id", 1L);

        Client client = Client.create("CL-ABC123", "Джон", "+996555001122", level);
        ReflectionTestUtils.setField(client, "id", 5L);
        client.setBonusBalance(1500L);

        when(clientRepository.findWithLevelById(5L))
                .thenReturn(Optional.of(client));
        when(debtRepository.sumOpenRemainingAmountByClientId(5L))
                .thenReturn(12500L);

        ClientResponse response = clientService.getClientById(5L);

        assertEquals(5L, response.id());
        assertEquals("CL-ABC123", response.clientCode());
        assertEquals(1500L, response.bonusBalance());
        assertEquals(12500L, response.totalDebt());
    }

    @Test
    @DisplayName("Выбрасывает ClientNotFoundException (404), если клиент по ID не найден или неактивен")
    void getClientById_WhenNotFoundOrInactive_ThrowsClientNotFoundException() {
        LoyaltyLevel level = LoyaltyLevel.create("Базовый", 0);
        Client inactiveClient = Client.create("CL-OLD001", "Архивный", null, level);
        inactiveClient.setIsActive(false);

        when(clientRepository.findWithLevelById(99L))
                .thenReturn(Optional.of(inactiveClient));

        ClientNotFoundException ex = assertThrows(
                ClientNotFoundException.class,
                () -> clientService.getClientById(99L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("CLIENT_NOT_FOUND", ex.getBody().getProperties().get("errorCode"));
        verifyNoInteractions(debtRepository);
    }

    @Test
    @DisplayName("Возвращает список клиентов с поддержкой поиска и батч-подсчётом открытых долгов")
    void getClients_WithAndWithoutSearch_ReturnsListWithDebts() {
        LoyaltyLevel level = LoyaltyLevel.create("Базовый", 0);
        Client client = Client.create("CL-111222", "Sam", null, level);
        ReflectionTestUtils.setField(client, "id", 1L);

        DebtRepository.ClientDebtTotalProjection debtProjection = new DebtRepository.ClientDebtTotalProjection() {
            @Override
            public Long getClientId() {
                return 1L;
            }

            @Override
            public Long getTotalDebt() {
                return 8000L;
            }
        };

        when(clientRepository.findAllByIsActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(client));
        when(clientRepository.searchActiveClients("Sam"))
                .thenReturn(List.of(client));
        when(debtRepository.sumOpenRemainingAmountByClientIds(List.of(1L)))
                .thenReturn(List.of(debtProjection));

        List<ClientResponse> all = clientService.getClients(null);
        List<ClientResponse> filtered = clientService.getClients("  Sam  ");

        assertEquals(1, all.size());
        assertEquals(8000L, all.getFirst().totalDebt());
        assertEquals(1, filtered.size());
        assertEquals("Sam", filtered.getFirst().name());
        assertEquals(8000L, filtered.getFirst().totalDebt());
        verify(clientRepository).findAllByIsActiveTrueOrderByCreatedAtDesc();
        verify(clientRepository).searchActiveClients("Sam");
    }

    @Test
    @DisplayName("Возвращает историю покупок клиента с позициями чека")
    void getClientPurchases_WhenSalesExist_ReturnsMappedPage() {
        LoyaltyLevel level = LoyaltyLevel.create("Базовый", 0);
        ReflectionTestUtils.setField(level, "id", 1L);

        Client client = Client.create("CL-ABC123", "Сэм", "+996555001122", level);
        ReflectionTestUtils.setField(client, "id", 5L);

        Product cola = Product.create("470000000001", "Кола", 3000L, 5500L);
        ReflectionTestUtils.setField(cola, "id", 101L);

        Sale sale = Sale.create(1L, PaymentMethod.CASH);
        ReflectionTestUtils.setField(sale, "id", 77L);
        sale.setClientId(5L);
        sale.setClientUuid("uuid-1234-5678");
        sale.setPaidAmount(11000L);
        SaleItem item = SaleItem.of(cola, 2, 5500L);
        ReflectionTestUtils.setField(item, "id", 501L);
        sale.addItem(item);

        Page<Sale> salesPage = new PageImpl<>(
                List.of(sale),
                new OffsetBasedPageRequest(0, 50),
                1L
        );

        when(clientRepository.findWithLevelById(5L))
                .thenReturn(Optional.of(client));
        when(saleRepository.findAllByClientIdOrderByOccurredAtDescCreatedAtDesc(eq(5L), any(Pageable.class)))
                .thenReturn(salesPage);
        when(saleRepository.findAllWithItemsByIdIn(List.of(77L)))
                .thenReturn(List.of(sale));

        PageResponse<ClientPurchaseResponse> response = clientService.getClientPurchases(5L, 50, 0);

        assertEquals(1L, response.total());
        assertEquals(50, response.limit());
        assertEquals(0, response.offset());
        assertEquals(1, response.items().size());

        ClientPurchaseResponse purchase = response.items().getFirst();
        assertEquals(77L, purchase.id());
        assertEquals("uuid-1234-5678", purchase.clientUuid());
        assertEquals("CL-ABC123", purchase.clientCode());
        assertEquals("Сэм", purchase.clientName());
        assertEquals(11000L, purchase.totalAmount());
        assertEquals(PaymentMethod.CASH, purchase.paymentMethod());
        assertEquals(1, purchase.items().size());
        assertEquals("Кола", purchase.items().getFirst().productName());
        assertEquals(2, purchase.items().getFirst().quantity());

        verify(saleRepository).findAllWithItemsByIdIn(List.of(77L));
    }

    @Test
    @DisplayName("Возвращает пустую страницу без лишнего запроса позиций, если у клиента нет покупок")
    void getClientPurchases_WhenNoSales_ReturnsEmptyPageWithoutFetchingItems() {
        LoyaltyLevel level = LoyaltyLevel.create("Базовый", 0);
        Client client = Client.create("CL-EMPTY1", "Sam", null, level);
        ReflectionTestUtils.setField(client, "id", 1L);

        when(clientRepository.findWithLevelById(1L))
                .thenReturn(Optional.of(client));
        when(saleRepository.findAllByClientIdOrderByOccurredAtDescCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(Page.empty());

        PageResponse<ClientPurchaseResponse> page = clientService.getClientPurchases(1L, 50, 0);

        assertTrue(page.items().isEmpty());
        assertEquals(0L, page.total());
        assertEquals(50, page.limit());
        assertEquals(0, page.offset());
        verify(saleRepository, never()).findAllWithItemsByIdIn(anyList());
    }

    @Test
    @DisplayName("Выбрасывает ClientNotFoundException (404) при запросе покупок несуществующего клиента")
    void getClientPurchases_WhenClientNotFound_ThrowsClientNotFoundException() {
        when(clientRepository.findWithLevelById(404L))
                .thenReturn(Optional.empty());

        assertThrows(ClientNotFoundException.class, () -> clientService.getClientPurchases(404L, 50, 0));
        verifyNoInteractions(saleRepository);
    }

    @Test
    @DisplayName("Находит активного клиента по QR-коду с обрезкой пробелов и актуальным долгом")
    void getClientByCode_WhenQrValid_ReturnsClientResponse() {
        LoyaltyLevel level = LoyaltyLevel.create("Серебряный", 100);
        Client client = Client.create("CL-1001", "Азамат", "+996555123456", level);
        ReflectionTestUtils.setField(client, "id", 7L);
        client.setBonusBalance(2500L);

        when(clientRepository.findByClientCodeAndIsActiveTrue("CL-1001"))
                .thenReturn(Optional.of(client));
        when(debtRepository.sumOpenRemainingAmountByClientId(7L))
                .thenReturn(4500L);

        ClientResponse response = clientService.getClientByCode("  CL-1001 \n");

        assertEquals("CL-1001", response.clientCode());
        assertEquals("Азамат", response.name());
        assertEquals(2500L, response.bonusBalance());
        assertEquals(4500L, response.totalDebt());
        assertEquals("Серебряный", response.level().name());
        assertEquals(100, response.level().cashbackPercentage());
        verify(clientRepository).findByClientCodeAndIsActiveTrue("CL-1001");
    }

    @Test
    @DisplayName("Бросает ClientNotFoundException (404 CLIENT_NOT_FOUND), если QR недействителен")
    void getClientByCode_WhenQrInvalid_ThrowsClientNotFoundException() {
        when(clientRepository.findByClientCodeAndIsActiveTrue("INVALID-QR"))
                .thenReturn(Optional.empty());

        ClientNotFoundException ex = assertThrows(
                ClientNotFoundException.class,
                () -> clientService.getClientByCode("INVALID-QR")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("CLIENT_NOT_FOUND", ex.getBody().getProperties().get("errorCode"));
    }
}
