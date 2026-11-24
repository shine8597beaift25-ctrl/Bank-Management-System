package model;

/** Every row in the transactions table has one of these types. */
public enum TransactionType {
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER_IN("Transfer In"),
    TRANSFER_OUT("Transfer Out");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
