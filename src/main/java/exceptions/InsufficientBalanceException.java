package exceptions;

import java.math.BigDecimal;

/**
 * CHECKED custom exception.
 * Thrown when a withdrawal or transfer asks for more money than
 * the account holds. This is how the system prevents negative balances.
 */
public class InsufficientBalanceException extends Exception {

    private static final long serialVersionUID = 1L;

    private final BigDecimal availableBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(BigDecimal availableBalance, BigDecimal requestedAmount) {
        super(String.format("Insufficient balance. Available: Rs. %,.2f, Requested: Rs. %,.2f",
                availableBalance, requestedAmount));
        this.availableBalance = availableBalance;
        this.requestedAmount = requestedAmount;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
