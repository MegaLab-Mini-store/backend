package kg.megalab.ministorebackend.exception;

import lombok.Getter;

@Getter
public class SaleRejectedException extends RuntimeException {

    private final String errorCode;

    public SaleRejectedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
