package kg.megalab.ministorebackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SaleRejectedException extends AppException {

    private final String errorCode;

    public SaleRejectedException(String errorCode, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Продажа отклонена", errorCode, message);
        this.errorCode = errorCode;
    }
}
