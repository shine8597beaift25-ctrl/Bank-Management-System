package model;

import util.ConsoleUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Savings account: earns interest, lower withdrawal limit.
 *
 * Rules of this simulation:
 *  - Interest rate      : 4.00% per year
 *  - Minimum opening    : Rs. 500.00
 *  - Withdrawal limit   : Rs. 50,000.00 per withdrawal / transfer
 *
 * Concepts shown: inheritance (extends BankAccount), method overriding,
 * calling the parent version with super.getDisplayText().
 */
public class SavingsAccount extends BankAccount {

    public static final BigDecimal INTEREST_RATE_PERCENT = new BigDecimal("4.00");
    private static final BigDecimal MINIMUM_OPENING_BALANCE = new BigDecimal("500.00");
    private static final BigDecimal WITHDRAWAL_LIMIT = new BigDecimal("50000.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public SavingsAccount(String accountNumber, String customerId, String accountHolderName,
                          BigDecimal balance, LocalDate creationDate, AccountStatus status) {
        super(accountNumber, customerId, accountHolderName, balance, creationDate, status);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
    }

    @Override
    public BigDecimal getWithdrawalLimit() {
        return WITHDRAWAL_LIMIT;
    }

    @Override
    public BigDecimal getMinimumOpeningBalance() {
        return MINIMUM_OPENING_BALANCE;
    }

    /** balance x 4 / 100, rounded to 2 decimal places. */
    @Override
    public BigDecimal calculateAnnualInterest() {
        return getBalance().multiply(INTEREST_RATE_PERCENT)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    /** Adds savings-only details to the parent's text. */
    @Override
    public String getDisplayText() {
        return super.getDisplayText() + '\n'
                + "Interest Rate    : " + INTEREST_RATE_PERCENT + "% per year" + '\n'
                + "Yearly Interest  : " + ConsoleUtil.formatMoney(calculateAnnualInterest()) + " (estimated)";
    }
}
