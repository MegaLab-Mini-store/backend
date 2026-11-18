package kg.megalab.ministorebackend.service;

import kg.megalab.ministorebackend.dto.ClientResponse;
import kg.megalab.ministorebackend.entity.Client;
import kg.megalab.ministorebackend.exception.ClientNotFoundException;
import kg.megalab.ministorebackend.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

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
}
