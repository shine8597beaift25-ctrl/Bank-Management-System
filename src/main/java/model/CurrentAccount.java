package model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Current account: meant for businesses, no interest, higher withdrawal limit.
 *
 * Rules of this simulation:
 *  - Interest rate      : none
 *  - Minimum opening    : Rs. 1,000.00
 *  - Withdrawal limit   : Rs. 2,00,000.00 per withdrawal / transfer
 *  - Overdraft          : not allowed (balance can never go below zero)
 */
public class CurrentAccount extends BankAccount {

    private static final BigDecimal MINIMUM_OPENING_BALANCE = new BigDecimal("1000.00");
    private static final BigDecimal WITHDRAWAL_LIMIT = new BigDecimal("200000.00");

    public CurrentAccount(String accountNumber, String customerId, String accountHolderName,
                          BigDecimal balance, LocalDate creationDate, AccountStatus status) {
        super(accountNumber, customerId, accountHolderName, balance, creationDate, status);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CURRENT;
    }

    @Override
    public BigDecimal getWithdrawalLimit() {
        return WITHDRAWAL_LIMIT;
    }

    @Override
    public BigDecimal getMinimumOpeningBalance() {
        return MINIMUM_OPENING_BALANCE;
    }

    /** Current accounts do not earn interest. */
    @Override
    public BigDecimal calculateAnnualInterest() {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public String getDisplayText() {
        return super.getDisplayText() + '\n'
                + "Interest Rate    : Not applicable (current account)" + '\n'
                + "Overdraft        : Not allowed";
    }
}
