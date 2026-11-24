package service;

import database.DatabaseManager;
import model.Transaction;
import model.TransactionStatus;
import model.TransactionType;
import repository.TransactionRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records transactions and reads transaction history.
 *
 * Concepts shown:
 *  - Method OVERLOADING: four getTransactions(...) methods with different parameters.
 *  - AtomicInteger: a thread-safe counter for reference IDs (atomicity without synchronized).
 */
public class TransactionService {

    private static final DateTimeFormatter REFERENCE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger REFERENCE_COUNTER = new AtomicInteger();

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** Creates a reference such as "TRF-20261120143005-007". Both sides of a transfer share it. */
    public String generateReferenceId(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(REFERENCE_TIME) + "-"
                + String.format("%03d", REFERENCE_COUNTER.incrementAndGet());
    }

    /** Saves a successful transaction using the caller's connection (part of a database transaction). */
    public Transaction recordSuccess(Connection connection, String accountNumber, TransactionType type,
                                     BigDecimal amount, String description, String referenceId) throws SQLException {
        Transaction transaction = new Transaction(accountNumber, type, amount, description,
                TransactionStatus.SUCCESS, referenceId);
        transactionRepository.save(connection, transaction);
        return transaction;
    }

    /**
     * Saves a FAILED attempt (for example, a withdrawal refused for insufficient balance).
     * This runs on its own connection AFTER the main work was rolled back,
     * so the balance is never changed by a failed attempt.
     * If even this note cannot be saved, we only print a warning - the original
     * error is more important and is still reported to the user.
     */
    public void recordFailure(String accountNumber, TransactionType type, BigDecimal amount,
                              String reason, String referenceId) {
        Transaction failed = new Transaction(accountNumber, type, amount, reason, TransactionStatus.FAILED, referenceId);
        try (Connection connection = DatabaseManager.getConnection()) {
            transactionRepository.save(connection, failed);
        } catch (SQLException e) {
            System.out.println("[WARNING] Could not save the failed-attempt record: " + e.getMessage());
        }
    }

    // ---------------- Overloaded history methods ----------------

    public List<Transaction> getTransactions() throws SQLException {
        return transactionRepository.findAll();
    }

    public List<Transaction> getTransactions(String accountNumber) throws SQLException {
        return transactionRepository.findByAccount(accountNumber);
    }

    public List<Transaction> getTransactions(TransactionType type) throws SQLException {
        return transactionRepository.findByType(type);
    }

    public List<Transaction> getTransactions(String accountNumber, TransactionType type) throws SQLException {
        return transactionRepository.findByAccountAndType(accountNumber, type);
    }

    public List<Transaction> getLatestTransactions(int limit) throws SQLException {
        return transactionRepository.findLatest(limit);
    }

    public List<Transaction> getTransactionsByReference(String referenceId) throws SQLException {
        return transactionRepository.findByReference(referenceId);
    }

    public long countTransactions() throws SQLException {
        return transactionRepository.count();
    }
}
