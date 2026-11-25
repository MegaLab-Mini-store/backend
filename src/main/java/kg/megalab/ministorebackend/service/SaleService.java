package kg.megalab.ministorebackend.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kg.megalab.ministorebackend.dto.CompleteSaleItemRequest;
import kg.megalab.ministorebackend.dto.CompleteSaleRequest;
import kg.megalab.ministorebackend.dto.CompleteSaleResult;
import kg.megalab.ministorebackend.dto.OperationType;
import kg.megalab.ministorebackend.entity.*;
import kg.megalab.ministorebackend.exception.ClientNotFoundException;
import kg.megalab.ministorebackend.exception.InvalidDebtException;
import kg.megalab.ministorebackend.exception.SaleRejectedException;
import kg.megalab.ministorebackend.repository.ProductRepository;
import kg.megalab.ministorebackend.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final ClientService clientService;
    private final DebtService debtService;
    private final BonusService bonusService; // US-27
    private final Clock clock;

    @Transactional
    public CompleteSaleResult completeSale(Long userId, CompleteSaleRequest request) {
        Client client = resolveClient(request.clientCode());

        if (request.paymentMethod() == PaymentMethod.DEBT && client == null) {
            throw new SaleRejectedException("DEBT_CLIENT_REQUIRED",
                    "Продажа в долг возможна только с клиентом");
        }

        Map<Long, Product> products = loadProducts(request);

        Sale sale = Sale.create(userId, request.paymentMethod());
        sale.setClientId(client != null ? client.getId() : null);
        sale.setClientUuid(request.clientUuid());
        sale.setReplacesClientUuid(request.replacesClientUuid());
        sale.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : OffsetDateTime.now(clock));

        for (CompleteSaleItemRequest item : request.items()) {
            sale.addItem(SaleItem.of(products.get(item.productId()), item.quantity(), item.unitPrice()));
        }

        if (request.paymentMethod() == PaymentMethod.DEBT) {
            sale.setPaidAmount(0L);
        } else {
            sale.setPaidAmount(sale.getTotalAmount());
        }

        Sale saved = saleRepository.saveAndFlush(sale);

        Map<Long, Integer> quantityByProduct = new LinkedHashMap<>();
        for (CompleteSaleItemRequest item : request.items()) {
            quantityByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }
        quantityByProduct.forEach((productId, quantity) ->
                stockService.apply(productId, -quantity, OperationType.SALE, saved.getId(), "Продажа"));

        if (client != null) {                                                  // US-27
            saved.setEarnedBonuses(bonusService.accrueForSale(saved, client)); // US-27
        }                                                                      // US-27

        Long debtId = null;
        if (request.paymentMethod() == PaymentMethod.DEBT) {
            long debtAmount = saved.getTotalAmount() - saved.getBonusSpent();
            try {
                Debt debt = debtService.createDebtForSale(client, saved.getId(), debtAmount, request.debtReminderDate());
                debtId = debt.getId();
            } catch (InvalidDebtException e) {
                throw new SaleRejectedException("VALIDATION_ERROR", e.getMessage());
            }
        }

        return new CompleteSaleResult(saved.getId(), saved.getTotalAmount(), saved.getEarnedBonuses(), debtId);
    }

    private Client resolveClient(String clientCode) {
        if (clientCode == null || clientCode.isBlank()) {
            return null;
        }
        try {
            return clientService.getActiveClientByCode(clientCode);
        } catch (ClientNotFoundException e) {
            throw new SaleRejectedException("CLIENT_NOT_FOUND", e.getMessage());
        }
    }

    private Map<Long, Product> loadProducts(CompleteSaleRequest request) {
        Set<Long> ids = request.items().stream()
                .map(CompleteSaleItemRequest::productId)
                .collect(Collectors.toSet());

        Map<Long, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (Long id : ids) {
            if (!products.containsKey(id)) {
                throw new SaleRejectedException("PRODUCT_NOT_FOUND", "Товар не найден: id " + id);
            }
        }
        return products;
    }
}
