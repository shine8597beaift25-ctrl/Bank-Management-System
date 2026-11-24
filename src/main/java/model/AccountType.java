package model;

/**
 * The two kinds of bank account. Each type has a short prefix
 * that is used at the start of its account numbers (SB1001, CA1002 ...).
 */
public enum AccountType {
    SAVINGS("SB", "Savings"),
    CURRENT("CA", "Current");

    private final String prefix;
    private final String label;

    AccountType(String prefix, String label) {
        this.prefix = prefix;
        this.label = label;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLabel() {
        return label;
    }

    /** Converts text such as "savings" or " CURRENT " into an AccountType. */
    public static AccountType fromText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Account type is missing.");
        }
        String cleaned = text.trim().toUpperCase();
        for (AccountType type : values()) {
            if (type.name().equals(cleaned)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid account type: " + text + " (use SAVINGS or CURRENT)");
    }
}
