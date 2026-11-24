package repository;

import database.DatabaseManager;
import model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for the customers table (CRUD: Create, Read, Update, Delete).
 *
 * Concepts shown:
 *  - PreparedStatement with "?" placeholders and parameter binding (setString, setInt).
 *    This is used for ALL user-provided values, which prevents SQL injection.
 *  - Statement for a fixed query with no user input (count()).
 *  - executeQuery() for SELECT, executeUpdate() for INSERT/UPDATE/DELETE.
 *  - ResultSet iteration with while (rs.next()) and typed getters (getString, getInt).
 *  - Object mapping: one ResultSet row -> one Customer object.
 *  - try-with-resources closes Connection, PreparedStatement and ResultSet automatically.
 */
public class CustomerRepository implements DataRepository<Customer, String> {

    private static final String INSERT_SQL =
            "INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID_SQL = "SELECT * FROM customers WHERE customer_id = ?";
    private static final String SELECT_BY_EMAIL_SQL = "SELECT * FROM customers WHERE email = ?";
    private static final String SELECT_ALL_SQL = "SELECT * FROM customers ORDER BY customer_id";
    private static final String UPDATE_SQL =
            "UPDATE customers SET name = ?, age = ?, email = ?, phone = ?, address = ? WHERE customer_id = ?";
    private static final String DELETE_SQL = "DELETE FROM customers WHERE customer_id = ?";
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM customers";
    private static final String COUNT_ACCOUNTS_SQL = "SELECT COUNT(*) FROM accounts WHERE customer_id = ?";

    @Override
    public void save(Customer customer) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, customer.getCustomerId());
            statement.setString(2, customer.getName());
            statement.setInt(3, customer.getAge());
            statement.setString(4, customer.getEmail());
            statement.setString(5, customer.getPhone());
            statement.setString(6, customer.getAddress());
            statement.executeUpdate();
        }
    }

    @Override
    public Optional<Customer> findById(String customerId) throws SQLException {
        return findOne(SELECT_BY_ID_SQL, customerId);
    }

    public Optional<Customer> findByEmail(String email) throws SQLException {
        return findOne(SELECT_BY_EMAIL_SQL, email);
    }

    @Override
    public List<Customer> findAll() throws SQLException {
        List<Customer> customers = new ArrayList<>();   // diamond operator <> : type taken from the left side
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                customers.add(mapRow(resultSet));
            }
        }
        return customers;
    }

    /** Returns true if a row was changed. */
    public boolean update(Customer customer) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setString(1, customer.getName());
            statement.setInt(2, customer.getAge());
            statement.setString(3, customer.getEmail());
            statement.setString(4, customer.getPhone());
            statement.setString(5, customer.getAddress());
            statement.setString(6, customer.getCustomerId());
            return statement.executeUpdate() == 1;
        }
    }

    /** Returns true if a row was deleted. */
    public boolean deleteById(String customerId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setString(1, customerId);
            return statement.executeUpdate() == 1;
        }
    }

    /** Uses a plain Statement because the SQL is fixed and has no user input. */
    @Override
    public long count() throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(COUNT_SQL)) {
            return resultSet.next() ? resultSet.getLong(1) : 0;
        }
    }

    /** How many accounts (active or closed) belong to this customer. */
    public int countAccounts(String customerId) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNT_ACCOUNTS_SQL)) {
            statement.setString(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    // ---------------- Private helpers ----------------

    private Optional<Customer> findOne(String sql, String value) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    /** Object mapping: copies one database row into a new Customer object using typed getters. */
    private Customer mapRow(ResultSet resultSet) throws SQLException {
        return new Customer(
                resultSet.getString("customer_id"),
                resultSet.getString("name"),
                resultSet.getInt("age"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("address"));
    }
}
