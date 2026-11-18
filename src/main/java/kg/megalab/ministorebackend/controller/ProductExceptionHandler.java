package kg.megalab.ministorebackend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ProductExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleValidationError(
            IllegalArgumentException exception) {

        return Map.of(
                "code", "INVALID_PRODUCT_DATA",
                "message", exception.getMessage()
        );
    }
}