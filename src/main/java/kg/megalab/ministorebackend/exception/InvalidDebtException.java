package kg.megalab.ministorebackend.exception;

import org.springframework.http.HttpStatus;

public class InvalidDebtException extends AppException {
    public InvalidDebtException(String message) {
        super(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Некорректная операция с долгом",
                "VALIDATION_ERROR",
                message
        );
    }
}
