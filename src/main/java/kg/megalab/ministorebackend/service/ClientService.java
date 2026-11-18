package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.*;
import kg.megalab.ministorebackend.entity.Client;
import kg.megalab.ministorebackend.entity.LoyaltyLevel;
import kg.megalab.ministorebackend.entity.Sale;
import kg.megalab.ministorebackend.exception.ClientNotFoundException;
import kg.megalab.ministorebackend.exception.ResourceNotFoundException;
import kg.megalab.ministorebackend.repository.ClientRepository;
import kg.megalab.ministorebackend.repository.LoyaltyLevelRepository;
import kg.megalab.ministorebackend.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private static final String CLIENT_CODE_PREFIX = "CL-";
    private static final String CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;

    private final ClientRepository clientRepository;
    private final SaleRepository saleRepository;
    private final LoyaltyLevelRepository loyaltyLevelRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public ClientResponse getClientByCode(String clientCode) {
        Client client = getActiveClientByCode(clientCode);
        Long totalDebt = 0L;
        return ClientResponse.from(client, totalDebt);
    }

    @Transactional(readOnly = true)
    public Client getActiveClientByCode(String clientCode) {
        return clientRepository.findByClientCodeAndIsActiveTrue(clientCode.trim())
                .orElseThrow(() -> new ClientNotFoundException(clientCode));
    }

    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        String normalizedName = request.name().trim();
        String normalizedPhone = (request.phone() == null || request.phone().isBlank())
                ? null
                : request.phone().trim();

        LoyaltyLevel loyaltyLevel = resolveLoyaltyLevel(request.loyaltyLevelId());
        String uniqueClientCode = generateUniqueClientCode();

        Client client = Client.create(uniqueClientCode, normalizedName, normalizedPhone, loyaltyLevel);
        Client savedClient = clientRepository.save(client);

        return ClientResponse.from(savedClient, 0L);
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findWithLevelById(id)
                .filter(Client::getIsActive)
                .orElseThrow(() -> new ClientNotFoundException(id));
        return ClientResponse.from(client, 0L);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> getClients(String search) {
        List<Client> clients = (search == null || search.isBlank())
                ? clientRepository.findAllByIsActiveTrueOrderByCreatedAtDesc()
                : clientRepository.searchActiveClients(search.trim());

        return clients.stream()
                .map(client -> ClientResponse.from(client, 0L))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientPurchaseResponse> getClientPurchases(Long id, int limit, int offset) {
        Client client = findActiveClientById(id);

        OffsetBasedPageRequest pageable = new OffsetBasedPageRequest(offset, limit);
        Page<Sale> salesPage = saleRepository.findAllByClientIdOrderByOccurredAtDescCreatedAtDesc(id, pageable);

        List<Sale> pageContent = salesPage.getContent();
        if (pageContent.isEmpty()) {
            return new PageResponse<>(List.of(), salesPage.getTotalElements(), limit, offset);
        }

        List<Long> saleIds = pageContent.stream()
                .map(Sale::getId)
                .toList();

        Map<Long, Sale> salesWithItemsById = saleRepository.findAllWithItemsByIdIn(saleIds).stream()
                .collect(Collectors.toMap(Sale::getId, Function.identity()));

        List<ClientPurchaseResponse> items = saleIds.stream()
                .map(salesWithItemsById::get)
                .map(sale -> ClientPurchaseResponse.from(sale, client))
                .toList();

        return new PageResponse<>(items, salesPage.getTotalElements(), limit, offset);
    }


    private LoyaltyLevel resolveLoyaltyLevel(Long loyaltyLevelId) {
        if (loyaltyLevelId != null) {
            return loyaltyLevelRepository.findById(loyaltyLevelId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Уровень лояльности с ID " + loyaltyLevelId + " не найден"
                    ));
        }

        return loyaltyLevelRepository.findFirstByOrderByCashbackPercentageAsc()
                .orElseGet(() -> loyaltyLevelRepository.save(LoyaltyLevel.create("Базовый", 0)));
    }

    private String generateUniqueClientCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CLIENT_CODE_PREFIX);
            for (int i = 0; i < CODE_LENGTH; i++) {
                int index = secureRandom.nextInt(CODE_ALPHABET.length());
                sb.append(CODE_ALPHABET.charAt(index));
            }
            code = sb.toString();
        } while (clientRepository.existsByClientCode(code));

        return code;
    }

    private Client findActiveClientById(Long id) {
        return clientRepository.findWithLevelById(id)
                .filter(Client::getIsActive)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }
}
