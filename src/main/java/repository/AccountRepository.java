package repository;

import database.DatabaseManager;
import model.AccountStatus;
import model.AccountType;
import model.BankAccount;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for the accounts table.
 *
 * Some methods take a Connection parameter. Those are used inside a database
 * TRANSACTION (deposit, withdrawal, transfer) where several SQL statements must
 * run on the SAME connection so they can be committed or rolled back together.
 *
 * Accounts are never deleted - they are CLOSED instead, so their transaction
 * history is kept.
 */
public class AccountRepository implements DataRepository<BankAccount, String> {

    /** JOIN with customers so we also get the account holder's name. */
    private static final String SELECT_BASE =
            "SELECT a.account_number, a.customer_id, a.account_type, a.balance, a.account_status, "
            + "a.creation_date, c.name AS holder_name "
            + "FROM accounts a JOIN customers c ON a.customer_id = c.customer_id ";

    private static final String INSERT_SQL =
            "INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID_SQL = SELECT_BASE + "WHERE a.account_number = ?";
    /** FOR UPDATE locks the account row until COMMIT/ROLLBACK, so no other connection can change it meanwhile. */
    private static final String SELECT_BY_ID_FOR_UPDATE_SQL = SELECT_BY_ID_SQL + " FOR UPDATE";
    private static final String SELECT_ALL_SQL = SELECT_BASE + "ORDER BY a.account_number";
    private static final String SELECT_BY_CUSTOMER_SQL = SELECT_BASE + "WHERE a.customer_id = ? ORDER BY a.account_number";
    private static final String SELECT_ACTIVE_BY_CUSTOMER_AND_TYPE_SQL =
            SELECT_BASE + "WHERE a.customer_id = ? AND a.account_type = ? AND a.account_status = 'ACTIVE'";
    private static final String UPDATE_BALANCE_SQL = "UPDATE accounts SET balance = ? WHERE account_number = ?";
    private static final String UPDATE_STATUS_SQL = "UPDATE accounts SET account_status = ? WHERE account_number = ?";
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM accounts";
    private static final String ALL_NUMBERS_SQL = "SELECT account_number FROM accounts";

    // ---------------- Create ----------------

    @Override
    public void save(BankAccount account) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            save(connection, account);
        }
    }

    /** Overloaded save that joins an existing database transaction. */
    public void save(Connection connection, BankAccount account) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, account.getAccountNumber());
            statement.setString(2, account.getCustomerId());
            statement.setString(3, account.getAccountType().name());
            statement.setBigDecimal(4, account.getBalance());          // DECIMAL(15,2) <-> BigDecimal, exact
            statement.setString(5, account.getStatus().name());
            statement.setObject(6, account.getCreationDate());         // JDBC 4.2: LocalDate -> DATE
            statement.executeUpdate();
        }
    }

    // ---------------- Read ----------------

    @Override
    public Optional<BankAccount> findById(String accountNumber) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            return findById(connection, accountNumber);
        }
    }

    public Optional<BankAccount> findById(Connection connection, String accountNumber) throws SQLException {
        return findOne(connection, SELECT_BY_ID_SQL, accountNumber);
    }

    /**
     * Reads AND LOCKS the account row (SELECT ... FOR UPDATE). Must be called inside a
     * database transaction (autoCommit = false). The lock is released at commit() or rollback().
     * This protects the balance even if two copies of the program run at the same time.
     */
    public Optional<BankAccount> findByIdForUpdate(Connection connection, String accountNumber) throws SQLException {
        return findOne(connection, SELECT_BY_ID_FOR_UPDATE_SQL, accountNumber);
    }

    private Optional<BankAccount> findOne(Connection connection, String sql, String accountNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, accountNumber);
            List<BankAccount> found = readAll(statement);
            return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
        }
    }

    @Override
    public List<BankAccount> findAll() throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL)) {
            return readAll(statement);
        }
    }

    public List<BankAccount> findByCustomerId(String customerId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_CUSTOMER_SQL)) {
            statement.setString(1, customerId);
            return readAll(statement);
        }
    }

    /** Finds the customer's ACTIVE account of the given type, if any. */
    public Optional<BankAccount> findActiveByCustomerAndType(String customerId, AccountType type) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ACTIVE_BY_CUSTOMER_AND_TYPE_SQL)) {
            statement.setString(1, customerId);
            statement.setString(2, type.name());
            List<BankAccount> found = readAll(statement);
            return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
        }
    }

    @Override
    public long count() throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(COUNT_SQL)) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        }
    }

    /**
     * Highest number used after the prefix, e.g. SB1007 and CA1003 -> 1007.
     * New accounts continue from here so numbers stay unique after a restart.
     */
    public int findHighestSequence() throws SQLException {
        int highest = 0;
        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(ALL_NUMBERS_SQL)) {
            while (resultSet.next()) {
                String digits = resultSet.getString(1).substring(2);   // remove "SB" / "CA"
                try {
                    highest = Math.max(highest, Integer.parseInt(digits));
                } catch (NumberFormatException e) {
                    // Number not in the usual format - simply ignore it for the sequence
                }
            }
        }
        return highest;
    }

    // ---------------- Update ----------------

    /** Returns the number of rows changed (1 = success, 0 = account missing). */
    public int updateBalance(Connection connection, String accountNumber, BigDecimal newBalance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_BALANCE_SQL)) {
            statement.setBigDecimal(1, newBalance);
            statement.setString(2, accountNumber);
            return statement.executeUpdate();
        }
    }

    public boolean updateStatus(String accountNumber, AccountStatus status) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS_SQL)) {
            statement.setString(1, status.name());
            statement.setString(2, accountNumber);
            return statement.executeUpdate() == 1;
        }
    }

    // ---------------- Mapping ----------------

    private List<BankAccount> readAll(PreparedStatement statement) throws SQLException {
        List<BankAccount> accounts = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                accounts.add(mapRow(resultSet));
            }
        }
        return accounts;
    }

    /** Builds a SavingsAccount or CurrentAccount object from one row. */
    private BankAccount mapRow(ResultSet resultSet) throws SQLException {
        AccountType type = AccountType.fromText(resultSet.getString("account_type"));
        return BankAccount.createAccount(
                type,
                resultSet.getString("account_number"),
                resultSet.getString("customer_id"),
                resultSet.getString("holder_name"),
                resultSet.getBigDecimal("balance"),                       // DECIMAL -> exact BigDecimal
                resultSet.getObject("creation_date", LocalDate.class),    // DATE -> LocalDate (JDBC 4.2)
                AccountStatus.fromText(resultSet.getString("account_status")));
    }
}
