package kg.megalab.ministorebackend.exception;

import org.springframework.http.HttpStatus;

public class ClientNotFoundException extends AppException {
    public ClientNotFoundException(String clientCode) {
        super(
                HttpStatus.NOT_FOUND,
                "Клиент не найден",
                "CLIENT_NOT_FOUND",
                String.format("Клиент с кодом '%s' не найден или неактивен", clientCode)
        );
    }
}
