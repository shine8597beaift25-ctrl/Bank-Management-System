package interfaces;

import exceptions.InsufficientBalanceException;

import java.math.BigDecimal;

/**
 * Basic money operations every bank account must support.
 * BankAccount implements this interface, so SavingsAccount and
 * CurrentAccount get these operations through inheritance.
 */
public interface TransactionOperations {

    /** Adds money. Throws InvalidAmountException (unchecked) for bad amounts. */
    void deposit(BigDecimal amount);

    /** Removes money. Throws InsufficientBalanceException (checked) if balance is too low. */
    void withdraw(BigDecimal amount) throws InsufficientBalanceException;

    BigDecimal getBalance();
}
