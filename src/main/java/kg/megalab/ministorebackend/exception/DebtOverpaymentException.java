package kg.megalab.ministorebackend.exception;

import org.springframework.http.HttpStatus;

public class DebtOverpaymentException extends AppException {
    public DebtOverpaymentException(Long remainingAmount, Long paymentAmount) {
        super(
                HttpStatus.CONFLICT,
                "Переплата по долгу",
                "DEBT_OVERPAYMENT",
                String.format("Сумма погашения (%d) превышает остаток долга (%d)", paymentAmount, remainingAmount)
        );
    }
}
