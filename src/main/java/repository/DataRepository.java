package repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * GENERIC repository interface.
 *
 *   T = the type of object stored (Customer, BankAccount, Transaction)
 *   K = the type of its ID (String for customers/accounts, Integer for transactions)
 *
 * Because of generics the compiler checks types for us:
 * a DataRepository<Customer, String> can only save Customers and only
 * accepts String IDs. No casting is needed (type safety).
 *
 * Only operations that make sense for ALL three tables are here.
 * Update and delete are added in the specific repositories where they are allowed
 * (for example, transactions are a permanent record and are never edited or deleted).
 */
public interface DataRepository<T, K> {

    void save(T entity) throws SQLException;

    /** Optional means "maybe a value, maybe empty" - it avoids returning null. */
    Optional<T> findById(K id) throws SQLException;

    List<T> findAll() throws SQLException;

    long count() throws SQLException;

    /** Default method built on top of findById - implementing classes get it for free. */
    default boolean existsById(K id) throws SQLException {
        return findById(id).isPresent();
    }
}
