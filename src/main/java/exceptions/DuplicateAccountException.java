package exceptions;

/**
 * CHECKED custom exception.
 * Thrown when:
 *  - an account number already exists (for example during CSV import), or
 *  - a customer tries to open a second account of the same type
 *    (rule: one savings and one current account per customer).
 */
public class DuplicateAccountException extends Exception {

    private static final long serialVersionUID = 1L;

    public DuplicateAccountException(String message) {
        super(message);
    }
}
