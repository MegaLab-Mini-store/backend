package kg.megalab.ministorebackend.exception;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.util.Objects;

@NullMarked
public abstract class AppException extends ErrorResponseException {

    protected AppException(HttpStatus status, String title, String errorCode, String message) {
        super(status, problem(status, title, errorCode, message), null);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String code, String message) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
        pd.setTitle(title);
        pd.setProperty("errorCode", code);
        return pd;
    }

    @Override
    public String getMessage() {
        return Objects.requireNonNullElse(getBody().getDetail(), super.getMessage());
    }
}

