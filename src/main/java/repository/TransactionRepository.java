package repository;

import database.DatabaseManager;
import model.Transaction;
import model.TransactionStatus;
import model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for the transactions table.
 *
 * Transactions are a permanent audit record: rows are only inserted and read,
 * never updated or deleted. That is why this class has no update/delete methods.
 */
public class TransactionRepository implements DataRepository<Transaction, Integer> {

    private static final String INSERT_SQL =
            "INSERT INTO transactions (account_number, transaction_type, amount, transaction_date, "
            + "description, status, reference_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM transactions WHERE transaction_id = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM transactions ORDER BY transaction_id";
    private static final String SELECT_BY_ACCOUNT_SQL =
            "SELECT * FROM transactions WHERE account_number = ? ORDER BY transaction_id";
    private static final String SELECT_BY_TYPE_SQL =
            "SELECT * FROM transactions WHERE transaction_type = ? ORDER BY transaction_id";
    private static final String SELECT_BY_ACCOUNT_AND_TYPE_SQL =
            "SELECT * FROM transactions WHERE account_number = ? AND transaction_type = ? ORDER BY transaction_id";
    private static final String SELECT_BY_REFERENCE_SQL =
            "SELECT * FROM transactions WHERE reference_id = ? ORDER BY transaction_id";
    private static final String SELECT_LATEST_SQL =
            "SELECT * FROM transactions ORDER BY transaction_id DESC LIMIT ?";
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM transactions";

    @Override
    public void save(Transaction transaction) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            save(connection, transaction);
        }
    }

    /** Inserts using an existing connection (part of a bigger database transaction). */
    public void save(Connection connection, Transaction transaction) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transaction.getAccountNumber());
            statement.setString(2, transaction.getType().name());
            statement.setBigDecimal(3, transaction.getAmount());
            statement.setObject(4, transaction.getTransactionDate());   // LocalDateTime -> DATETIME
            statement.setString(5, transaction.getDescription());
            statement.setString(6, transaction.getStatus().name());
            statement.setString(7, transaction.getReferenceId());
            statement.executeUpdate();

            // Read the ID that MySQL created (AUTO_INCREMENT) and store it in the object
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setTransactionId(keys.getInt(1));
                }
            }
        }
    }

    @Override
    public Optional<Transaction> findById(Integer transactionId) throws SQLException {
        List<Transaction> found = query(SELECT_BY_ID_SQL, transactionId);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    @Override
    public List<Transaction> findAll() throws SQLException {
        return query(SELECT_ALL_SQL);
    }

    public List<Transaction> findByAccount(String accountNumber) throws SQLException {
        return query(SELECT_BY_ACCOUNT_SQL, accountNumber);
    }

    public List<Transaction> findByType(TransactionType type) throws SQLException {
        return query(SELECT_BY_TYPE_SQL, type.name());
    }

    public List<Transaction> findByAccountAndType(String accountNumber, TransactionType type) throws SQLException {
        return query(SELECT_BY_ACCOUNT_AND_TYPE_SQL, accountNumber, type.name());
    }

    public List<Transaction> findByReference(String referenceId) throws SQLException {
        return query(SELECT_BY_REFERENCE_SQL, referenceId);
    }

    /** Newest first. LIMIT ? is bound with setInt (parameter binding works for numbers too). */
    public List<Transaction> findLatest(int limit) throws SQLException {
        return query(SELECT_LATEST_SQL, limit);
    }

    @Override
    public long count() throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(COUNT_SQL)) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        }
    }

    // ---------------- Private helpers ----------------

    /**
     * Runs a SELECT with any number of parameters ("varargs": Object... parameters).
     * setObject binds each value in order to the "?" placeholders.
     * One helper for every query = no duplicated JDBC code.
     */
    private List<Transaction> query(String sql, Object... parameters) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapRow(resultSet));
                }
            }
        }
        return transactions;
    }

    private Transaction mapRow(ResultSet resultSet) throws SQLException {
        return new Transaction(
                resultSet.getInt("transaction_id"),
                resultSet.getString("account_number"),
                TransactionType.valueOf(resultSet.getString("transaction_type")),
                resultSet.getBigDecimal("amount"),
                resultSet.getObject("transaction_date", LocalDateTime.class),
                resultSet.getString("description"),
                TransactionStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("reference_id"));
    }
}
