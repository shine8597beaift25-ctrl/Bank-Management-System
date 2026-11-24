package exceptions;

/**
 * CHECKED custom exception.
 * Thrown when a customer ID or email address is already registered.
 */
public class DuplicateCustomerException extends Exception {

    private static final long serialVersionUID = 1L;

    public DuplicateCustomerException(String message) {
        super(message);
    }
}
