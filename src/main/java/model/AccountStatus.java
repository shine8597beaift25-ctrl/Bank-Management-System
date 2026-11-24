package model;

/** An account is either open for transactions or closed. */
public enum AccountStatus {
    ACTIVE,
    CLOSED;

    public static AccountStatus fromText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Account status is missing.");
        }
        String cleaned = text.trim().toUpperCase();
        for (AccountStatus status : values()) {
            if (status.name().equals(cleaned)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid account status: " + text + " (use ACTIVE or CLOSED)");
    }
}
