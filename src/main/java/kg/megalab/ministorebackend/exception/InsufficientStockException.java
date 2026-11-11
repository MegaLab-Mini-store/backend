package kg.megalab.ministorebackend.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productName, int requested, int available) {
        super(String.format(
                "Недостаточно товара '%s' на складе: запрошено %d, доступно %d",
                productName, requested, available
        ));
    }
}
