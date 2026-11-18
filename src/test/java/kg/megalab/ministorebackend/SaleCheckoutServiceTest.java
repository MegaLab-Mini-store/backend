package kg.megalab.ministorebackend;

import kg.megalab.ministorebackend.dto.*;
import kg.megalab.ministorebackend.entity.Product;
import kg.megalab.ministorebackend.exception.ClientNotFoundException;
import kg.megalab.ministorebackend.exception.InsufficientStockException;
import kg.megalab.ministorebackend.exception.ResourceNotFoundException;
import kg.megalab.ministorebackend.repository.ProductRepository;
import kg.megalab.ministorebackend.service.ClientService;
import kg.megalab.ministorebackend.service.SaleCheckoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleCheckoutServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private SaleCheckoutService saleCheckoutService;

    private Product createTestProduct(Long id, String barcode, String name, Long salePrice, int stockQuantity) {
        Product product = Product.create(barcode, name, 3000L, salePrice);
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "stockQuantity", stockQuantity);
        return product;
    }

    @Test
    @DisplayName("Успешное сканирование товара возвращает название, цену, кол-во 1 и сумму (не затрагивая сервис клиентов)")
    void scanProductForSale_Success() {
        Product cola = createTestProduct(101L, "470000000001", "Кола", 5500L, 20);

        when(productRepository.findByBarcodeAndIsActiveTrue("470000000001"))
                .thenReturn(Optional.of(cola));

        SaleItemResponse response = saleCheckoutService.scanProductForSale("470000000001");

        assertEquals(101L, response.productId());
        assertEquals("Кола", response.productName());
        assertEquals(5500L, response.unitPrice());
        assertEquals(1, response.quantity());
        assertEquals(5500L, response.subtotal());
        assertEquals(20, response.availableStock());
        verifyNoInteractions(clientService);
    }

    @Test
    @DisplayName("Сканирование товара с нулевым остатком выбрасывает InsufficientStockException")
    void scanProductForSale_ZeroStock_ThrowsException() {
        Product outOfStockProduct = createTestProduct(104L, "470000000004", "Кофе", 1500L, 0);

        when(productRepository.findByBarcodeAndIsActiveTrue("470000000004"))
                .thenReturn(Optional.of(outOfStockProduct));

        assertThrows(InsufficientStockException.class,
                () -> saleCheckoutService.scanProductForSale("470000000004"));
    }

    @Test
    @DisplayName("Сканирование несуществующего штрихкода выбрасывает ResourceNotFoundException")
    void scanProductForSale_NotFound_ThrowsException() {
        when(productRepository.findByBarcodeAndIsActiveTrue("999999"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> saleCheckoutService.scanProductForSale("999999"));
    }

    @Test
    @DisplayName("Расчет чека корректно считает суммы позиций (subtotal) объединяет дубли и считает итог (totalAmount)")
    void calculateAndValidateReceipt_Success() {
        Product cola = createTestProduct(101L, "470000000001", "Кола", 5500L, 20);
        Product snickers = createTestProduct(102L, "470000000002", "Сникерс", 4500L, 15);

        SaleCheckoutRequest request = new SaleCheckoutRequest(
                List.of(
                        new SaleItemRequest(101L, 1),
                        new SaleItemRequest(102L, 1),
                        new SaleItemRequest(101L, 2)
                ),
                null
        );

        when(productRepository.findAllById(Set.of(101L, 102L)))
                .thenReturn(List.of(cola, snickers));

        SaleCalculationResponse response = saleCheckoutService.calculateAndValidateReceipt(request);

        assertEquals(2, response.items().size());
        assertEquals(3, response.items().get(0).quantity());
        assertEquals(16500L, response.items().get(0).subtotal());
        assertEquals(1, response.items().get(1).quantity());
        assertEquals(4500L, response.items().get(1).subtotal());
        assertEquals(21000L, response.totalAmount());
        assertNull(response.client());
        verifyNoInteractions(clientService);
    }

    @Test
    @DisplayName("Нельзя оформить продажу если запрошенное количество больше остатка на складе")
    void calculateAndValidateReceipt_InsufficientStock_ThrowsException() {
        Product chips = createTestProduct(103L, "470000000003", "Чипсы", 9500L, 2);

        SaleCheckoutRequest request = new SaleCheckoutRequest(
                List.of(new SaleItemRequest(103L, 3)),
                null
        );

        when(productRepository.findAllById(Set.of(103L)))
                .thenReturn(List.of(chips));

        InsufficientStockException ex = assertThrows(
                InsufficientStockException.class,
                () -> saleCheckoutService.calculateAndValidateReceipt(request)
        );

        assertTrue(ex.getMessage().contains("Чипсы"));
    }

    @Test
    @DisplayName("При корректном QR-коде клиент связывается с текущей продажей")
    void calculateAndValidateReceipt_WithValidClientQr_LinksClient() {
        Product cola = createTestProduct(101L, "470000000001", "Кола", 5500L, 20);

        ClientResponse mockClient = new ClientResponse(
                10L,
                "CL-1001",
                "Джон",
                "+996555123456",
                null,
                new LoyaltyLevelResponse(1L, "Серебряный", 100),
                2500L,
                0L,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                true
        );

        SaleCheckoutRequest request = new SaleCheckoutRequest(
                List.of(new SaleItemRequest(101L, 2)),
                "CL-1001"
        );

        when(productRepository.findAllById(Set.of(101L)))
                .thenReturn(List.of(cola));
        when(clientService.getClientByCode("CL-1001"))
                .thenReturn(mockClient);

        SaleCalculationResponse response = saleCheckoutService.calculateAndValidateReceipt(request);

        assertEquals(11000L, response.totalAmount());
        assertNotNull(response.client());
        assertEquals(10L, response.client().id());
        assertEquals("CL-1001", response.client().clientCode());
        assertEquals("Джон", response.client().name());
        assertEquals(2500L, response.client().bonusBalance());
        verify(clientService).getClientByCode("CL-1001");
    }

    @Test
    @DisplayName("При недействительном QR система сообщает об ошибке и позволяет продолжить продажу без привязки клиента")
    void calculateAndValidateReceipt_InvalidQrThenContinueWithoutClient_Success() {
        Product cola = createTestProduct(101L, "470000000001", "Кола", 5500L, 20);

        when(productRepository.findAllById(Set.of(101L)))
                .thenReturn(List.of(cola));
        when(clientService.getClientByCode("INVALID-QR"))
                .thenThrow(new ClientNotFoundException("INVALID-QR"));

        // 1. Попытка с недействительным QR -> выбрасывает ClientNotFoundException
        SaleCheckoutRequest requestWithInvalidQr = new SaleCheckoutRequest(
                List.of(new SaleItemRequest(101L, 2)),
                "INVALID-QR"
        );

        assertThrows(
                ClientNotFoundException.class,
                () -> saleCheckoutService.calculateAndValidateReceipt(requestWithInvalidQr)
        );

        // 2. Продолжение той же продажи без привязки клиента (пустой/null clientCode)
        SaleCheckoutRequest requestWithoutClient = new SaleCheckoutRequest(
                List.of(new SaleItemRequest(101L, 2)),
                "   "
        );

        SaleCalculationResponse response = saleCheckoutService.calculateAndValidateReceipt(requestWithoutClient);

        assertEquals(11000L, response.totalAmount());
        assertNull(response.client());
    }
}