package exceptions;

/**
 * CHECKED custom exception.
 * Thrown when a customer ID does not exist in the database.
 * Because it extends Exception, the compiler forces callers to
 * either catch it or declare it with "throws".
 */
public class CustomerNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String customerId;

    public CustomerNotFoundException(String customerId) {
        super("Customer not found with ID: " + customerId);
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }
}
