package kg.megalab.ministorebackend.exception;

import org.springframework.http.HttpStatus;

public class DebtNotFoundException extends AppException {
    public DebtNotFoundException(Long debtId) {
        super(
                HttpStatus.NOT_FOUND,
                "Долг не найден",
                "DEBT_NOT_FOUND",
                String.format("Долг с id %d не найден", debtId)
        );
    }
}
