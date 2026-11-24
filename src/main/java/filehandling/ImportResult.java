package filehandling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * GENERIC class that holds the outcome of a CSV import.
 *
 *   ImportResult<Customer>    - customers read from customers.csv
 *   ImportResult<BankAccount> - accounts read from accounts.csv
 *
 * The same class works for any type T, and the compiler makes sure a
 * Customer result only ever contains Customer objects (type safety).
 */
public class ImportResult<T> {

    private final List<T> validRecords = new ArrayList<>();     // read correctly from the file
    private final List<String> errors = new LinkedList<>();     // LinkedList: we only add and loop
    private int importedCount;                                  // actually saved to the database

    public void addValidRecord(T record) {
        validRecords.add(record);
    }

    public void addError(String message) {
        errors.add(message);
    }

    public void recordImported() {
        importedCount++;
    }

    /** Read-only view so outside code cannot change the list by mistake. */
    public List<T> getValidRecords() {
        return Collections.unmodifiableList(validRecords);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public int getImportedCount() {
        return importedCount;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
