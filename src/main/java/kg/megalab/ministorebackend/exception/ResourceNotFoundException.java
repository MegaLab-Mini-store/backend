package kg.megalab.ministorebackend.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(
                HttpStatus.NOT_FOUND,
                "Ресурс не найден",
                "RESOURCE_NOT_FOUND",
                message
        );
    }
}
