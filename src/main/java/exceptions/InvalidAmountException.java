package exceptions;

/**
 * UNCHECKED custom exception (extends RuntimeException).
 * Thrown when an amount is zero, negative, not a number, has more than
 * two decimal places, or is above the allowed limit.
 * The compiler does not force callers to catch it, but Main catches it
 * so the program never stops unexpectedly.
 */
public class InvalidAmountException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidAmountException(String message) {
        super(message);
    }

    public InvalidAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}
