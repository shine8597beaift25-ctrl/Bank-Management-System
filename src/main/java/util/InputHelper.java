package util;

import exceptions.InvalidAmountException;

import java.math.BigDecimal;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Reads keyboard input safely. Every method keeps asking until the input is valid,
 * so a typing mistake never crashes the program.
 *
 * One Scanner is shared by the whole program (static) because creating several
 * Scanners on System.in causes lost input.
 *
 * If the input stream ends (for example when input is piped from a file),
 * Scanner throws NoSuchElementException. Main catches it and exits cleanly.
 */
public final class InputHelper {

    private static final Scanner SCANNER = new Scanner(System.in);

    private InputHelper() {
    }

    /** Reads a whole line (may be empty). */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    /** Reads a line and returns the default value when the user just presses Enter. */
    public static String readLineOrDefault(String prompt, String defaultValue) {
        String value = readLine(prompt + " [" + defaultValue + "]: ");
        return value.isEmpty() ? defaultValue : value;
    }

    /** Reads a whole number between min and max (inclusive). */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt);
            try {
                int value = Integer.parseInt(text);   // parsing with the Integer wrapper class
                if (value >= min && value <= max) {
                    return value;
                }
                ConsoleUtil.printError("Please enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                ConsoleUtil.printError("'" + text + "' is not a valid number. Please try again.");
            }
        }
    }

    /** Reads a menu choice. */
    public static int readMenuChoice(int maxOption) {
        return readInt("Enter Your Choice: ", 1, maxOption);
    }

    /** Reads a money amount and keeps asking until it is valid. */
    public static BigDecimal readAmount(String prompt) {
        while (true) {
            try {
                return ValidationUtil.parseAmount(readLine(prompt));
            } catch (InvalidAmountException e) {
                ConsoleUtil.printError(e.getMessage());
            }
        }
    }

    /**
     * Generic method: reads text and checks it with ANY validator function.
     * Example: readValid("Email: ", ValidationUtil::validateEmail)
     * The validator throws IllegalArgumentException when the text is wrong.
     * <T> means the method can return any type (String, Integer ...).
     */
    public static <T> T readValid(String prompt, Function<String, T> validator) {
        while (true) {
            try {
                return validator.apply(readLine(prompt));
            } catch (IllegalArgumentException e) {
                ConsoleUtil.printError(e.getMessage());
            }
        }
    }

    /** Like readValid, but pressing Enter keeps the current value (used by "Update Customer"). */
    public static <T> T readValidOrKeep(String prompt, T currentValue, Function<String, T> validator) {
        while (true) {
            String text = readLine(prompt + " [" + currentValue + "]: ");
            if (text.isEmpty()) {
                return currentValue;
            }
            try {
                return validator.apply(text);
            } catch (IllegalArgumentException e) {
                ConsoleUtil.printError(e.getMessage());
            }
        }
    }

    /** Returns true for y / yes (any case). */
    public static boolean readYesNo(String prompt) {
        while (true) {
            String answer = readLine(prompt + " (y/n): ");
            // equalsIgnoreCase compares content, ignoring upper/lower case
            if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                return true;
            }
            if (answer.equalsIgnoreCase("n") || answer.equalsIgnoreCase("no")) {
                return false;
            }
            ConsoleUtil.printError("Please type y or n.");
        }
    }
}
