package model;

import exceptions.InsufficientBalanceException;
import exceptions.InvalidAmountException;
import interfaces.Displayable;
import interfaces.TransactionOperations;
import util.ConsoleUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * ABSTRACT parent class of SavingsAccount and CurrentAccount.
 *
 * You cannot write "new BankAccount(...)" because the class is abstract.
 * It holds everything the two account types share and leaves the
 * type-specific rules (withdrawal limit, interest, minimum opening deposit)
 * as abstract methods that each subclass must override.
 *
 * Concepts shown:
 *  - Abstract class and abstract methods.
 *  - Interfaces (Displayable, TransactionOperations).
 *  - static variable (nextAccountSequence) shared by ALL account objects.
 *  - static synchronized method so two threads never get the same number.
 *  - Encapsulation: balance can only change through deposit() and withdraw().
 *  - BigDecimal for exact money values (never float/double).
 */
public abstract class BankAccount implements Displayable, TransactionOperations, Comparable<BankAccount> {

    /** static final = a constant shared by every object. */
    public static final String BANK_NAME = "Java National Bank";

    /** static variable: one copy for the whole class, not one per object. */
    private static int nextAccountSequence = 1001;

    private String accountNumber;          // null for a brand-new account until a number is assigned
    private final String customerId;
    private String accountHolderName;
    private BigDecimal balance;
    private final LocalDate creationDate;
    private AccountStatus status;

    /** Parameterized constructor used by both subclasses through super(...). */
    protected BankAccount(String accountNumber, String customerId, String accountHolderName,
                          BigDecimal balance, LocalDate creationDate, AccountStatus status) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountHolderName = accountHolderName;
        this.balance = balance.setScale(2, RoundingMode.HALF_UP);
        this.creationDate = creationDate;
        this.status = status;
    }

    // ---------------- Abstract methods (each subclass decides) ----------------

    public abstract AccountType getAccountType();

    /** Largest amount allowed in a single withdrawal or transfer. */
    public abstract BigDecimal getWithdrawalLimit();

    /** Smallest first deposit needed to open this kind of account. */
    public abstract BigDecimal getMinimumOpeningBalance();

    /** Interest earned in one year on the current balance. */
    public abstract BigDecimal calculateAnnualInterest();

    // ---------------- static methods ----------------

    /**
     * Creates the next account number, for example "SB1005".
     * synchronized on the class so two threads can never receive the same number.
     */
    public static synchronized String generateAccountNumber(AccountType type) {
        String number = type.getPrefix() + nextAccountSequence;
        nextAccountSequence++;
        return number;
    }

    /** Called at start-up (and after CSV import) so new numbers continue after the highest saved one. */
    public static synchronized void updateSequence(int highestUsedSequence) {
        if (highestUsedSequence >= nextAccountSequence) {
            nextAccountSequence = highestUsedSequence + 1;
        }
    }

    /**
     * Static "factory" method. It returns the parent type BankAccount,
     * but the real object is a SavingsAccount or a CurrentAccount.
     * When code later calls account.getWithdrawalLimit(), Java picks the
     * subclass version at run time - this is RUNTIME POLYMORPHISM.
     */
    public static BankAccount createAccount(AccountType type, String accountNumber, String customerId,
                                            String holderName, BigDecimal balance,
                                            LocalDate creationDate, AccountStatus status) {
        switch (type) {
            case SAVINGS:
                return new SavingsAccount(accountNumber, customerId, holderName, balance, creationDate, status);
            case CURRENT:
                return new CurrentAccount(accountNumber, customerId, holderName, balance, creationDate, status);
            default:
                throw new IllegalArgumentException("Unknown account type: " + type);
        }
    }

    // ---------------- TransactionOperations ----------------

    /** Adds money to this account (in memory). The service then saves it to the database. */
    @Override
    public void deposit(BigDecimal amount) {
        ensureActive();
        ensurePositive(amount);
        balance = balance.add(amount);   // BigDecimal is immutable: add() returns a NEW object
    }

    /** Removes money from this account (in memory). Prevents negative balances. */
    @Override
    public void withdraw(BigDecimal amount) throws InsufficientBalanceException {
        ensureActive();
        ensurePositive(amount);
        if (amount.compareTo(getWithdrawalLimit()) > 0) {
            throw new InvalidAmountException("Amount exceeds the single withdrawal limit of "
                    + ConsoleUtil.formatMoney(getWithdrawalLimit()) + " for a "
                    + getAccountType().getLabel() + " account.");
        }
        if (amount.compareTo(balance) > 0) {
            throw new InsufficientBalanceException(balance, amount);   // "throw" a checked exception
        }
        balance = balance.subtract(amount);
    }

    @Override
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Gives a NEW account its number. Called only after all opening checks have passed,
     * so a refused opening does not use up an account number.
     */
    public void assignAccountNumber(String newAccountNumber) {
        if (this.accountNumber != null) {
            throw new IllegalStateException("Account number is already assigned: " + this.accountNumber);
        }
        this.accountNumber = newAccountNumber;
    }

    /** Closes the account. Only allowed when the balance is exactly zero. */
    public void close() {
        if (status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account " + accountNumber + " is already closed.");
        }
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account " + accountNumber + " still has a balance of "
                    + ConsoleUtil.formatMoney(balance) + ". Withdraw or transfer it before closing.");
        }
        status = AccountStatus.CLOSED;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    // ---------------- Small private helper methods ----------------

    private void ensureActive() {
        if (!isActive()) {
            // IllegalStateException is a built-in UNCHECKED exception
            throw new IllegalStateException("Account " + accountNumber + " is closed. No transactions allowed.");
        }
    }

    private void ensurePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
    }

    // ---------------- Displayable ----------------

    @Override
    public String getDisplayText() {
        StringBuilder text = new StringBuilder();
        text.append("Bank             : ").append(BANK_NAME).append('\n');
        text.append("Account Number   : ").append(accountNumber).append('\n');
        text.append("Account Type     : ").append(getAccountType().getLabel()).append('\n');
        text.append("Customer ID      : ").append(customerId).append('\n');
        text.append("Account Holder   : ").append(accountHolderName).append('\n');
        text.append("Balance          : ").append(ConsoleUtil.formatMoney(balance)).append('\n');
        text.append("Status           : ").append(status).append('\n');
        text.append("Opened On        : ").append(creationDate).append('\n');
        text.append("Withdrawal Limit : ").append(ConsoleUtil.formatMoney(getWithdrawalLimit()));
        return text.toString();
    }

    @Override
    public String[] toTableRow() {
        return new String[] {accountNumber, customerId, accountHolderName, getAccountType().getLabel(),
                ConsoleUtil.formatMoney(balance), status.name(), creationDate.toString()};
    }

    /** Natural order = by account number. */
    @Override
    public int compareTo(BankAccount other) {
        return this.accountNumber.compareTo(other.accountNumber);
    }

    @Override
    public String toString() {
        return accountNumber + " (" + getAccountType().getLabel() + ", " + accountHolderName + ", "
                + ConsoleUtil.formatMoney(balance) + ")";
    }

    // ---------------- Getters ----------------

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
