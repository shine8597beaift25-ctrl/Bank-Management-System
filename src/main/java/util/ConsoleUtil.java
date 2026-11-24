package util;

import interfaces.Displayable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Helper methods for clean, formatted console output.
 *
 * Concepts shown:
 *  - static methods (called as ConsoleUtil.printHeader(...) without creating an object).
 *  - Method OVERLOADING (compile-time polymorphism): formatMoney(...) and printTable(...)
 *    each have two versions with different parameters.
 *  - Multidimensional array: String[][] rows holds a whole table.
 *  - StringBuilder, String.format and String manipulation (repeat, substring, padding).
 *  - Wildcard generics: List<? extends Displayable> accepts List<Customer>, List<Transaction> ...
 */
public final class ConsoleUtil {

    public static final int MENU_WIDTH = 52;
    private static final int MAX_COLUMN_WIDTH = 32;

    /** Private constructor: this class only has static methods, so no objects are needed. */
    private ConsoleUtil() {
    }

    public static String line(char symbol, int width) {
        return String.valueOf(symbol).repeat(width);
    }

    /** Puts text in the middle of a line of the given width. */
    public static String center(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int leftPadding = (width - text.length()) / 2;
        return " ".repeat(leftPadding) + text;
    }

    public static void printHeader(String title) {
        System.out.println();
        System.out.println(line('=', MENU_WIDTH));
        System.out.println(center(title.toUpperCase(), MENU_WIDTH));
        System.out.println(line('=', MENU_WIDTH));
    }

    public static void printSubHeader(String title) {
        System.out.println();
        System.out.println(line('-', MENU_WIDTH));
        System.out.println(center(title, MENU_WIDTH));
        System.out.println(line('-', MENU_WIDTH));
    }

    public static void printSuccess(String message) {
        System.out.println("[SUCCESS] " + message);
    }

    public static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }

    // ---------------- Overloaded money formatting ----------------

    /** formatMoney(1500) -> "Rs. 1,500.00" */
    public static String formatMoney(BigDecimal amount) {
        return formatMoney(amount, true);
    }

    /** formatMoney(1500, false) -> "1,500.00" (overloaded version with an extra parameter). */
    public static String formatMoney(BigDecimal amount, boolean withSymbol) {
        if (amount == null) {
            return "-";
        }
        String number = String.format("%,.2f", amount);   // %f formats BigDecimal exactly
        return withSymbol ? "Rs. " + number : number;
    }

    // ---------------- Overloaded table printing ----------------

    /** Prints any list of Displayable objects as a table. */
    public static void printTable(String[] headers, List<? extends Displayable> items) {
        String[][] rows = new String[items.size()][];
        for (int i = 0; i < items.size(); i++) {
            rows[i] = items.get(i).toTableRow();
        }
        printTable(headers, rows);
    }

    /** Prints a table from a 2-D array (rows x columns). */
    public static void printTable(String[] headers, String[][] rows) {
        if (rows.length == 0) {
            printInfo("No records found.");
            return;
        }
        int[] widths = calculateColumnWidths(headers, rows);
        String separator = buildSeparator(widths);

        StringBuilder table = new StringBuilder();
        table.append(separator).append('\n');
        table.append(buildRow(headers, widths)).append('\n');
        table.append(separator).append('\n');
        for (String[] row : rows) {
            table.append(buildRow(row, widths)).append('\n');
        }
        table.append(separator);
        System.out.println(table);
        System.out.println("Total records: " + rows.length);
    }

    /** Prints label : value pairs, keeping the order of the map (use a LinkedHashMap). */
    public static void printKeyValues(Map<String, String> values) {
        int labelWidth = 0;
        for (String label : values.keySet()) {
            labelWidth = Math.max(labelWidth, label.length());
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            System.out.println(padRight(entry.getKey(), labelWidth) + " : " + entry.getValue());
        }
    }

    // ---------------- Private helpers ----------------

    private static int[] calculateColumnWidths(String[] headers, String[][] rows) {
        int[] widths = new int[headers.length];
        for (int column = 0; column < headers.length; column++) {
            widths[column] = headers[column].length();
            for (String[] row : rows) {
                String cell = column < row.length && row[column] != null ? row[column] : "";
                widths[column] = Math.max(widths[column], cell.length());
            }
            widths[column] = Math.min(widths[column], MAX_COLUMN_WIDTH);
        }
        return widths;
    }

    private static String buildSeparator(int[] widths) {
        StringBuilder separator = new StringBuilder("+");
        for (int width : widths) {
            separator.append(line('-', width + 2)).append('+');
        }
        return separator.toString();
    }

    private static String buildRow(String[] cells, int[] widths) {
        StringBuilder row = new StringBuilder("|");
        for (int column = 0; column < widths.length; column++) {
            String cell = column < cells.length && cells[column] != null ? cells[column] : "";
            row.append(' ').append(padRight(shorten(cell, widths[column]), widths[column])).append(" |");
        }
        return row.toString();
    }

    /** Cuts long text and adds "..." so the table stays tidy. */
    private static String shorten(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private static String padRight(String text, int width) {
        return String.format("%-" + width + "s", text);
    }
}
