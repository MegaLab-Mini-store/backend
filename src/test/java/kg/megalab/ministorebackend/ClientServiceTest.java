package kg.megalab.ministorebackend;

import kg.megalab.ministorebackend.dto.ClientResponse;
import kg.megalab.ministorebackend.entity.Client;
import kg.megalab.ministorebackend.entity.LoyaltyLevel;
import kg.megalab.ministorebackend.exception.ClientNotFoundException;
import kg.megalab.ministorebackend.repository.ClientRepository;
import kg.megalab.ministorebackend.service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    @DisplayName("Находит активного клиента по QR-коду с обрезкой пробелов")
    void getClientByCode_WhenQrValid_ReturnsClientResponse() {
        LoyaltyLevel level = LoyaltyLevel.create("Серебряный", 100);
        Client client = Client.create("CL-1001", "Джон", "+996555123456", level);
        client.setBonusBalance(2500L);

        when(clientRepository.findByClientCodeAndIsActiveTrue("CL-1001"))
                .thenReturn(Optional.of(client));

        ClientResponse response = clientService.getClientByCode("  CL-1001 \n");

        assertThat(response.clientCode()).isEqualTo("CL-1001");
        assertThat(response.name()).isEqualTo("Джон");
        assertThat(response.bonusBalance()).isEqualTo(2500L);
        assertThat(response.level().name()).isEqualTo("Серебряный");
        assertThat(response.level().cashbackPercentage()).isEqualTo(100);
        verify(clientRepository).findByClientCodeAndIsActiveTrue("CL-1001");
    }

    @Test
    @DisplayName("Бросает ClientNotFoundException (404 CLIENT_NOT_FOUND), если QR недействителен")
    void getClientByCode_WhenQrInvalid_ThrowsClientNotFoundException() {
        when(clientRepository.findByClientCodeAndIsActiveTrue("INVALID-QR"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientByCode("INVALID-QR"))
                .isInstanceOf(ClientNotFoundException.class)
                .satisfies(ex -> {
                    ClientNotFoundException appEx = (ClientNotFoundException) ex;
                    assertThat(appEx.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(appEx.getBody().getProperties()).containsEntry("errorCode", "CLIENT_NOT_FOUND");
                });
    }
}
