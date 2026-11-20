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
import kg.megalab.ministorebackend.entity.Client; 
import kg.megalab.ministorebackend.entity.PaymentMethod;
import kg.megalab.ministorebackend.entity.Product;
import kg.megalab.ministorebackend.entity.Sale;
import kg.megalab.ministorebackend.entity.SaleItem;
import kg.megalab.ministorebackend.exception.SaleRejectedException;
import kg.megalab.ministorebackend.repository.ClientRepository; 
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
    private final ClientRepository clientRepository; 
    private final StockService stockService;
    private final Clock clock;

    @Transactional
    public CompleteSaleResult completeSale(Long userId, CompleteSaleRequest request) {
        if (request.paymentMethod() == PaymentMethod.DEBT) {
            throw new SaleRejectedException("DEBT_CLIENT_REQUIRED",
                    "Продажа в долг возможна только с клиентом");
        }

        Map<Long, Product> products = loadProducts(request);
        Client client = loadClient(request); 

        Sale sale = Sale.create(userId, request.paymentMethod());
        sale.setClientUuid(request.clientUuid());
        sale.setReplacesClientUuid(request.replacesClientUuid());
        sale.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : OffsetDateTime.now(clock));
        if (client != null) {                  
            sale.setClientId(client.getId());  
        }                                      

        for (CompleteSaleItemRequest item : request.items()) {
            sale.addItem(SaleItem.of(products.get(item.productId()), item.quantity(), item.unitPrice()));
        }
        sale.setPaidAmount(sale.getTotalAmount());

        Sale saved = saleRepository.saveAndFlush(sale);

        Map<Long, Integer> quantityByProduct = new LinkedHashMap<>();
        for (CompleteSaleItemRequest item : request.items()) {
            quantityByProduct.merge(item.productId(), item.quantity(), Integer::sum);
        }
        quantityByProduct.forEach((productId, quantity) ->
                stockService.apply(productId, -quantity, OperationType.SALE, saved.getId(), "Продажа"));

        return new CompleteSaleResult(saved.getId(), saved.getTotalAmount(), saved.getEarnedBonuses());
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

  
    private Client loadClient(CompleteSaleRequest request) {
        if (request.clientCode() == null) {
            return null;
        }
        return clientRepository.findByClientCodeAndIsActiveTrue(request.clientCode().trim())
                .orElseThrow(() -> new SaleRejectedException("CLIENT_NOT_FOUND",
                        "Клиент не найден: " + request.clientCode()));
    }
}
