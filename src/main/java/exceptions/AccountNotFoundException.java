package exceptions;

/**
 * CHECKED custom exception.
 * Thrown when an account number does not exist in the database.
 */
public class AccountNotFoundException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String accountNumber;

    public AccountNotFoundException(String accountNumber) {
        super("Account not found with number: " + accountNumber);
        this.accountNumber = accountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
