package model;

import interfaces.Displayable;
import util.ConsoleUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One row of the transactions table: a deposit, a withdrawal, or one side of a transfer.
 *
 * A transfer creates TWO Transaction rows that share the same referenceId:
 *   TRANSFER_OUT on the sender's account and TRANSFER_IN on the receiver's account.
 */
public class Transaction implements Displayable, Comparable<Transaction> {

    /** One shared formatter for all objects (static) - format used in the database. */
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int transactionId;            // 0 until the database gives it an ID
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final LocalDateTime transactionDate;
    private final String description;
    private final TransactionStatus status;
    private final String referenceId;

    /** Constructor for a NEW transaction (no ID yet, date = now). */
    public Transaction(String accountNumber, TransactionType type, BigDecimal amount,
                       String description, TransactionStatus status, String referenceId) {
        this(0, accountNumber, type, amount, LocalDateTime.now().withNano(0), description, status, referenceId);
    }

    /** Constructor for a transaction LOADED from the database (all fields known). */
    public Transaction(int transactionId, String accountNumber, TransactionType type, BigDecimal amount,
                       LocalDateTime transactionDate, String description, TransactionStatus status,
                       String referenceId) {
        this.transactionId = transactionId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.status = status;
        this.referenceId = referenceId;
    }

    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESS;
    }

    @Override
    public String getDisplayText() {
        return String.format("#%d | %s | %s | %s | %s | %s | %s | Ref: %s",
                transactionId, transactionDate.format(DATE_TIME_FORMAT), accountNumber, type.getLabel(),
                ConsoleUtil.formatMoney(amount), status, description, referenceId);
    }

    @Override
    public String[] toTableRow() {
        return new String[] {String.valueOf(transactionId), transactionDate.format(DATE_TIME_FORMAT),
                accountNumber, type.getLabel(), ConsoleUtil.formatMoney(amount), status.name(),
                description, referenceId};
    }

    /** Natural order = oldest first (by date, then by ID). */
    @Override
    public int compareTo(Transaction other) {
        int byDate = this.transactionDate.compareTo(other.transactionDate);
        return byDate != 0 ? byDate : Integer.compare(this.transactionId, other.transactionId);
    }

    @Override
    public String toString() {
        return getDisplayText();
    }

    // ---------------- Getters / setter ----------------

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
