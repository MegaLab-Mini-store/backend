package kg.megalab.ministorebackend.debt;

import kg.megalab.ministorebackend.entity.Client;
import kg.megalab.ministorebackend.entity.Debt;
import kg.megalab.ministorebackend.entity.DebtStatus;
import kg.megalab.ministorebackend.entity.LoyaltyLevel;
import kg.megalab.ministorebackend.exception.DebtOverpaymentException;
import kg.megalab.ministorebackend.exception.InvalidDebtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DebtTest {

    private Client createClient() {
        LoyaltyLevel level = LoyaltyLevel.create("Базовый", 0);
        return Client.create("CL-TEST01", "Азамат", "+996700112233", level);
    }

    @Test
    @DisplayName("Создаёт открытый долг с равными initialAmount и remainingAmount")
    void create_WithValidData_CreatesOpenDebt() {
        Client client = createClient();
        LocalDate reminderDate = LocalDate.of(2026, 12, 1);

        Debt debt = Debt.create(client, 15L, 25000L, reminderDate);

        assertEquals(client, debt.getClient());
        assertEquals(15L, debt.getSaleId());
        assertEquals(25000L, debt.getInitialAmount());
        assertEquals(25000L, debt.getRemainingAmount());
        assertEquals(DebtStatus.OPEN, debt.getStatus());
        assertEquals(reminderDate, debt.getReminderDate());
    }

    @Test
    @DisplayName("Нельзя записать нулевую или отрицательную сумму долга")
    void create_WithZeroOrNegativeAmount_ThrowsInvalidDebtException() {
        Client client = createClient();

        InvalidDebtException zeroEx = assertThrows(
                InvalidDebtException.class,
                () -> Debt.create(client, null, 0L, null)
        );
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, zeroEx.getStatusCode());
        assertEquals("VALIDATION_ERROR", zeroEx.getBody().getProperties().get("errorCode"));

        assertThrows(
                InvalidDebtException.class,
                () -> Debt.create(client, null, -5000L, null)
        );
    }

    @Test
    @DisplayName("Частичное погашение уменьшает remainingAmount, оставляя статус OPEN")
    void applyPayment_PartialPayment_DecreasesRemainingAmount() {
        Debt debt = Debt.create(createClient(), null, 10000L, null);

        debt.applyPayment(4000L);

        assertEquals(10000L, debt.getInitialAmount());
        assertEquals(6000L, debt.getRemainingAmount());
        assertEquals(DebtStatus.OPEN, debt.getStatus());
    }

    @Test
    @DisplayName("Полное погашение обнуляет remainingAmount и переводит статус в CLOSED")
    void applyPayment_FullPayment_ClosesDebt() {
        Debt debt = Debt.create(createClient(), null, 10000L, null);

        debt.applyPayment(6000L);
        debt.applyPayment(4000L);

        assertEquals(0L, debt.getRemainingAmount());
        assertEquals(DebtStatus.CLOSED, debt.getStatus());
    }

    @Test
    @DisplayName("Погашение больше остатка выбрасывает DebtOverpaymentException (409 DEBT_OVERPAYMENT)")
    void applyPayment_Overpayment_ThrowsDebtOverpaymentException() {
        Debt debt = Debt.create(createClient(), null, 10000L, null);

        DebtOverpaymentException ex = assertThrows(
                DebtOverpaymentException.class,
                () -> debt.applyPayment(10001L)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("DEBT_OVERPAYMENT", ex.getBody().getProperties().get("errorCode"));
        assertEquals(10000L, debt.getRemainingAmount());
    }

    @Test
    @DisplayName("Нельзя погасить с суммой <= 0 или уже закрытый долг")
    void applyPayment_InvalidAmountOrClosedDebt_ThrowsInvalidDebtException() {
        Debt debt = Debt.create(createClient(), null, 10000L, null);

        assertThrows(InvalidDebtException.class, () -> debt.applyPayment(0L));
        assertThrows(InvalidDebtException.class, () -> debt.applyPayment(-100L));

        debt.applyPayment(10000L);
        assertEquals(DebtStatus.CLOSED, debt.getStatus());

        assertThrows(InvalidDebtException.class, () -> debt.applyPayment(100L));
    }

    @Test
    @DisplayName("Аннулирование долга обнуляет остаток и ставит статус VOIDED")
    void voidDebt_SetsStatusVoidedAndZeroRemaining() {
        Debt debt = Debt.create(createClient(), 5L, 12000L, null);

        debt.voidDebt();

        assertEquals(0L, debt.getRemainingAmount());
        assertEquals(DebtStatus.VOIDED, debt.getStatus());
    }
}
