package kg.megalab.ministorebackend.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends AppException {
    public InsufficientStockException(String productName, int requested, int available) {
        super(
                HttpStatus.BAD_REQUEST,
                "Недостаточно товара на складе",
                "INSUFFICIENT_STOCK",
                String.format("Недостаточно товара '%s' на складе: запрошено %d, доступно %d",
                        productName, requested, available)
        );
    }
}
