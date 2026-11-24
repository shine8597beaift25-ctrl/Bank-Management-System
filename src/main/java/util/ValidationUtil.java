package util;

import exceptions.InvalidAmountException;
import model.Customer;

import java.math.BigDecimal;

/**
 * All input checks live here so the same rules are used everywhere
 * (keyboard input, updates and CSV import). No duplicated validation code.
 *
 * Concepts shown:
 *  - String methods: trim, isEmpty, matches (regular expressions), toUpperCase,
 *    toLowerCase, split, charAt, equals vs ==.
 *  - StringBuilder for building the title-cased name.
 *  - Wrapper classes and parsing: Integer.parseInt, new BigDecimal(text).
 *  - Unchecked exceptions: IllegalArgumentException and InvalidAmountException.
 */
public final class ValidationUtil {

    public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("1000000.00");   // Rs. 10 lakh

    private static final String CUSTOMER_ID_PATTERN = "C\\d{4}";                  // e.g. C1001
    private static final String NAME_PATTERN = "[A-Za-z][A-Za-z .]{1,49}";        // letters, spaces, dots
    private static final String EMAIL_PATTERN = "[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+";
    private static final String PHONE_PATTERN = "[6-9]\\d{9}";                    // 10-digit Indian mobile
    private static final String ACCOUNT_NUMBER_PATTERN = "(SB|CA)\\d{4,}";       // e.g. SB1001

    private ValidationUtil() {
    }

    // ---------------- Customer fields ----------------

    public static String validateCustomerId(String customerId) {
        String cleaned = requireText(customerId, "Customer ID").toUpperCase();
        if (!cleaned.matches(CUSTOMER_ID_PATTERN)) {
            throw new IllegalArgumentException("Customer ID must be 'C' followed by 4 digits, for example C1001.");
        }
        return cleaned;
    }

    /** Checks the name and returns it in Title Case: "  rAvi  kumar " -> "Ravi Kumar". */
    public static String validateName(String name) {
        String cleaned = requireText(name, "Name").replaceAll("\\s+", " ");
        if (!cleaned.matches(NAME_PATTERN)) {
            throw new IllegalArgumentException("Name must be 2-50 characters and contain only letters, spaces and dots.");
        }
        return toTitleCase(cleaned);
    }

    /** Parses the age text into an int (wrapper class parsing + autounboxing). */
    public static int validateAge(String ageText) {
        String cleaned = requireText(ageText, "Age");
        int age;
        try {
            Integer parsedAge = Integer.valueOf(cleaned);   // String -> Integer object (wrapper)
            age = parsedAge;                                // auto-UNBOXING: Integer -> int
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Age must be a whole number.");
        }
        return validateAge(age);
    }

    /** Overloaded version that checks an int age directly. */
    public static int validateAge(int age) {
        if (age < 18 || age > 100) {
            throw new IllegalArgumentException("Age must be between 18 and 100 to open a bank account.");
        }
        return age;
    }

    public static String validateEmail(String email) {
        String cleaned = requireText(email, "Email").toLowerCase();
        if (!cleaned.matches(EMAIL_PATTERN)) {
            throw new IllegalArgumentException("Email address is not valid (example: name@example.com).");
        }
        return cleaned;
    }

    public static String validatePhone(String phone) {
        // Remove spaces and dashes the user may type: "98765 43210" -> "9876543210"
        String cleaned = requireText(phone, "Phone").replace(" ", "").replace("-", "");
        if (cleaned.startsWith("+91")) {
            cleaned = cleaned.substring(3);
        }
        if (!cleaned.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException("Phone must be a 10-digit mobile number starting with 6, 7, 8 or 9.");
        }
        return cleaned;
    }

    public static String validateAddress(String address) {
        String cleaned = requireText(address, "Address").replaceAll("\\s+", " ");
        if (cleaned.length() < 5 || cleaned.length() > 100) {
            throw new IllegalArgumentException("Address must be between 5 and 100 characters.");
        }
        return cleaned;
    }

    /**
     * Validates every field of a customer and stores the cleaned values back.
     * Used for registration, update and CSV import.
     */
    public static void validateCustomer(Customer customer) {
        customer.setCustomerId(validateCustomerId(customer.getCustomerId()));
        customer.setName(validateName(customer.getName()));
        customer.setAge(validateAge(customer.getAge()));
        customer.setEmail(validateEmail(customer.getEmail()));
        customer.setPhone(validatePhone(customer.getPhone()));
        customer.setAddress(validateAddress(customer.getAddress()));
    }

    // ---------------- Accounts and money ----------------

    public static String validateAccountNumber(String accountNumber) {
        String cleaned = requireText(accountNumber, "Account number").toUpperCase();
        if (!cleaned.matches(ACCOUNT_NUMBER_PATTERN)) {
            throw new IllegalArgumentException("Account number must look like SB1001 (savings) or CA1001 (current).");
        }
        return cleaned;
    }

    /**
     * Turns typed text into a money amount.
     * Rules: must be a number, greater than zero, at most 2 decimal places,
     * and not more than Rs. 10,00,000 in one transaction.
     */
    public static BigDecimal parseAmount(String amountText) {
        BigDecimal amount = parseMoney(amountText);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount cannot be more than "
                    + ConsoleUtil.formatMoney(MAX_TRANSACTION_AMOUNT) + " in one transaction.");
        }
        return amount;
    }

    /** Parses a balance (zero is allowed, negative is not). Used by CSV import. */
    public static BigDecimal parseBalance(String balanceText) {
        BigDecimal balance = parseMoney(balanceText);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Balance cannot be negative.");
        }
        return balance;
    }

    private static BigDecimal parseMoney(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidAmountException("Amount is required.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(text.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("'" + text.trim() + "' is not a valid number.", e);
        }
        if (amount.scale() > 2) {
            throw new InvalidAmountException("Amount can have at most 2 decimal places (paise).");
        }
        return amount.setScale(2);
    }

    // ---------------- Small String helpers ----------------

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();   // trim() returns a NEW String; the original is unchanged (immutability)
    }

    /** "ravi KUMAR" -> "Ravi Kumar" using split, charAt and StringBuilder. */
    public static String toTitleCase(String text) {
        String[] words = text.trim().toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
