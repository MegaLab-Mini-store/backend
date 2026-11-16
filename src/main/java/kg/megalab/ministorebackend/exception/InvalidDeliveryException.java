package kg.megalab.ministorebackend.exception;

import org.springframework.http.HttpStatus;

public class InvalidDeliveryException extends AppException {
    public InvalidDeliveryException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Некорректная поставка", "INVALID_DELIVERY", message);
    }
}
