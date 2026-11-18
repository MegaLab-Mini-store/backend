package kg.megalab.ministorebackend.exception;

import org.springframework.http.HttpStatus;

public class InvalidSyncRequestException extends AppException {

    public InvalidSyncRequestException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Неверный запрос синхронизации", "VALIDATION_ERROR", message);
    }
}
