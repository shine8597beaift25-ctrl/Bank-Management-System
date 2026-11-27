package filehandling;

import exceptions.InvalidAmountException;
import model.AccountStatus;
import model.AccountType;
import model.BankAccount;
import model.Customer;
import model.Transaction;
import util.ValidationUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * All file work in the project.
 *
 *  CHARACTER streams (text): FileWriter / FileReader (wrapped in BufferedWriter / BufferedReader)
 *                            for CSV export and import and for saving text reports.
 *  BYTE streams (raw bytes): FileOutputStream writes the SQL backup file, FileInputStream reads it back to check it.
 *  StreamTokenizer          : splits a file into words, numbers and quoted text.
 *
 * Every stream is opened in try-with-resources, so it is closed even if an error happens.
 */
public class FileManager {

    public static final String DATA_FOLDER = "data";
    public static final String CUSTOMERS_CSV = DATA_FOLDER + "/customers.csv";
    public static final String ACCOUNTS_CSV = DATA_FOLDER + "/accounts.csv";
    public static final String TRANSACTIONS_CSV = DATA_FOLDER + "/transactions.csv";

    public static final String[] CUSTOMER_HEADER = {"customer_id", "name", "age", "email", "phone", "address"};
    public static final String[] ACCOUNT_HEADER =
            {"account_number", "customer_id", "account_type", "balance", "account_status", "creation_date"};
    public static final String[] TRANSACTION_HEADER = {"transaction_id", "account_number", "transaction_type",
            "amount", "transaction_date", "description", "status", "reference_id"};

    // =====================================================================
    // EXPORT (character streams)
    // =====================================================================

    public int exportCustomers(List<Customer> customers, String filePath) throws IOException {
        // Lambda turns one Customer into one row of text values
        return exportToCsv(filePath, CUSTOMER_HEADER, customers, customer -> new String[] {
                customer.getCustomerId(), customer.getName(), String.valueOf(customer.getAge()),
                customer.getEmail(), customer.getPhone(), customer.getAddress()});
    }

    public int exportAccounts(List<BankAccount> accounts, String filePath) throws IOException {
        return exportToCsv(filePath, ACCOUNT_HEADER, accounts, account -> new String[] {
                account.getAccountNumber(), account.getCustomerId(), account.getAccountType().name(),
                account.getBalance().toPlainString(), account.getStatus().name(), account.getCreationDate().toString()});
    }

    public int exportTransactions(List<Transaction> transactions, String filePath) throws IOException {
        return exportToCsv(filePath, TRANSACTION_HEADER, transactions, transaction -> new String[] {
                String.valueOf(transaction.getTransactionId()), transaction.getAccountNumber(),
                transaction.getType().name(), transaction.getAmount().toPlainString(),
                transaction.getTransactionDate().format(Transaction.DATE_TIME_FORMAT),
                transaction.getDescription(), transaction.getStatus().name(), transaction.getReferenceId()});
    }

    /**
     * GENERIC method used by all three exports (no duplicated code).
     * <T> is the record type; rowMapper is a Function (functional interface) that turns a T into text values.
     */
    public <T> int exportToCsv(String filePath, String[] header, List<T> records,
                               Function<T, String[]> rowMapper) throws IOException {
        createParentFolder(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            writer.write(toCsvLine(header));
            writer.newLine();
            for (T record : records) {
                writer.write(toCsvLine(rowMapper.apply(record)));
                writer.newLine();
            }
        }
        return records.size();
    }

    /** Saves any text (for example a banking report) to a file. */
    public void writeTextFile(String filePath, String text) throws IOException {
        createParentFolder(filePath);
        try (FileWriter writer = new FileWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(text);
        }
    }

    // =====================================================================
    // IMPORT (character streams + validation)
    // =====================================================================

    /** Reads customers.csv. Bad rows are reported, good rows are returned for saving. */
    public ImportResult<Customer> readCustomers(String filePath) throws IOException {
        ImportResult<Customer> result = new ImportResult<>();
        Set<String> idsInFile = new HashSet<>();   // HashSet finds duplicates quickly

        for (Map.Entry<Integer, String[]> row : readCsv(filePath, CUSTOMER_HEADER).entrySet()) {
            int lineNumber = row.getKey();
            String[] fields = row.getValue();
            if (fields.length != CUSTOMER_HEADER.length) {
                result.addError("Line " + lineNumber + ": expected " + CUSTOMER_HEADER.length
                        + " values but found " + fields.length + ".");
                continue;
            }
            try {
                Customer customer = new Customer(fields[0], fields[1], ValidationUtil.validateAge(fields[2]),
                        fields[3], fields[4], fields[5]);
                ValidationUtil.validateCustomer(customer);
                if (!idsInFile.add(customer.getCustomerId())) {   // add() returns false if already present
                    result.addError("Line " + lineNumber + ": customer ID " + customer.getCustomerId()
                            + " appears more than once in the file.");
                    continue;
                }
                result.addValidRecord(customer);
            } catch (IllegalArgumentException e) {
                result.addError("Line " + lineNumber + ": " + e.getMessage());
            }
        }
        return result;
    }

    /** Reads accounts.csv and checks every value before anything is saved. */
    public ImportResult<BankAccount> readAccounts(String filePath) throws IOException {
        ImportResult<BankAccount> result = new ImportResult<>();
        Set<String> numbersInFile = new HashSet<>();

        for (Map.Entry<Integer, String[]> row : readCsv(filePath, ACCOUNT_HEADER).entrySet()) {
            int lineNumber = row.getKey();
            String[] fields = row.getValue();
            if (fields.length != ACCOUNT_HEADER.length) {
                result.addError("Line " + lineNumber + ": expected " + ACCOUNT_HEADER.length
                        + " values but found " + fields.length + ".");
                continue;
            }
            try {
                BankAccount account = parseAccount(fields);
                if (!numbersInFile.add(account.getAccountNumber())) {
                    result.addError("Line " + lineNumber + ": account " + account.getAccountNumber()
                            + " appears more than once in the file.");
                    continue;
                }
                result.addValidRecord(account);
            } catch (IllegalArgumentException | InvalidAmountException | DateTimeParseException e) {   // multi-catch
                result.addError("Line " + lineNumber + ": " + e.getMessage());
            }
        }
        return result;
    }

    private BankAccount parseAccount(String[] fields) {
        String accountNumber = ValidationUtil.validateAccountNumber(fields[0]);
        String customerId = ValidationUtil.validateCustomerId(fields[1]);
        AccountType type = AccountType.fromText(fields[2]);
        if (!accountNumber.startsWith(type.getPrefix())) {
            throw new IllegalArgumentException("Account " + accountNumber + " must start with "
                    + type.getPrefix() + " for a " + type.getLabel() + " account.");
        }
        BigDecimal balance = ValidationUtil.parseBalance(fields[3]);
        AccountStatus status = AccountStatus.fromText(fields[4]);
        if (status == AccountStatus.CLOSED && balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("A CLOSED account must have a zero balance.");
        }
        LocalDate creationDate = LocalDate.parse(fields[5].trim());   // expects yyyy-MM-dd
        if (creationDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Creation date " + creationDate + " is in the future.");
        }
        // Holder name is filled in from the customers table when the account is saved
        return BankAccount.createAccount(type, accountNumber, customerId, "", balance, creationDate, status);
    }

    /**
     * Reads a CSV file into a LinkedHashMap of (line number -> values).
     * LinkedHashMap keeps the file order, and the line number makes error messages useful.
     * The first line must be the expected header.
     */
    private Map<Integer, String[]> readCsv(String filePath, String[] expectedHeader) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }
        Map<Integer, String[]> rows = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.trim().equalsIgnoreCase(String.join(",", expectedHeader))) {
                throw new IOException("Wrong file format. The first line must be: " + String.join(",", expectedHeader));
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!line.trim().isEmpty()) {
                    rows.put(lineNumber, parseCsvLine(line));
                }
            }
        }
        return rows;
    }

    // =====================================================================
    // BYTE STREAMS: database backup to an .sql file
    // =====================================================================

    /**
     * Writes every customer, account and transaction as SQL INSERT statements.
     * The text is turned into bytes (UTF-8) and written with a FileOutputStream (byte stream).
     * The file can be loaded back into an empty database with:
     *     mysql -u root -p bank_management < backup-file.sql
     *
     * @return the number of bytes written
     */
    public long writeSqlBackup(String filePath, String databaseName, List<Customer> customers,
                               List<BankAccount> accounts, List<Transaction> transactions) throws IOException {
        StringBuilder sql = new StringBuilder();
        sql.append("-- Bank Management System backup of database ").append(databaseName).append('\n');
        sql.append("-- Created: ").append(java.time.LocalDateTime.now().format(Transaction.DATE_TIME_FORMAT)).append('\n');
        sql.append("-- Restore into an EMPTY database: mysql -u root -p ").append(databaseName)
                .append(" < this-file.sql").append("\n\n");

        for (Customer customer : customers) {
            sql.append("INSERT INTO customers (customer_id, name, age, email, phone, address) VALUES (")
                    .append(sqlText(customer.getCustomerId())).append(", ").append(sqlText(customer.getName())).append(", ")
                    .append(customer.getAge()).append(", ").append(sqlText(customer.getEmail())).append(", ")
                    .append(sqlText(customer.getPhone())).append(", ").append(sqlText(customer.getAddress())).append(");\n");
        }
        for (BankAccount account : accounts) {
            sql.append("INSERT INTO accounts (account_number, customer_id, account_type, balance, account_status, creation_date) VALUES (")
                    .append(sqlText(account.getAccountNumber())).append(", ").append(sqlText(account.getCustomerId())).append(", ")
                    .append(sqlText(account.getAccountType().name())).append(", ").append(account.getBalance().toPlainString())
                    .append(", ").append(sqlText(account.getStatus().name())).append(", ")
                    .append(sqlText(account.getCreationDate().toString())).append(");\n");
        }
        for (Transaction transaction : transactions) {
            sql.append("INSERT INTO transactions (transaction_id, account_number, transaction_type, amount, transaction_date, "
                    + "description, status, reference_id) VALUES (")
                    .append(transaction.getTransactionId()).append(", ").append(sqlText(transaction.getAccountNumber())).append(", ")
                    .append(sqlText(transaction.getType().name())).append(", ").append(transaction.getAmount().toPlainString())
                    .append(", ").append(sqlText(transaction.getTransactionDate().format(Transaction.DATE_TIME_FORMAT)))
                    .append(", ").append(sqlText(transaction.getDescription())).append(", ")
                    .append(sqlText(transaction.getStatus().name())).append(", ")
                    .append(sqlText(transaction.getReferenceId())).append(");\n");
        }

        createParentFolder(filePath);
        byte[] bytes = sql.toString().getBytes(StandardCharsets.UTF_8);   // characters -> bytes
        try (FileOutputStream output = new FileOutputStream(filePath)) {
            output.write(bytes);
        }
        return bytes.length;
    }

    /** Reads a file byte by byte with a FileInputStream and returns its size (used to verify a backup). */
    public long countBytes(String filePath) throws IOException {
        long totalBytes = 0;
        byte[] buffer = new byte[4096];
        try (FileInputStream input = new FileInputStream(filePath)) {
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {   // -1 means end of file
                totalBytes += bytesRead;
            }
        }
        return totalBytes;
    }

    /** Puts text in single quotes for SQL, doubling any quote inside it: O'Neil -> 'O''Neil'. */
    private static String sqlText(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
    }

    // =====================================================================
    // StreamTokenizer: analyse a file
    // =====================================================================

    /**
     * Uses StreamTokenizer to count the words, numbers and quoted values in a text/CSV file.
     * Returns the counts in a LinkedHashMap (keeps the display order).
     */
    public Map<String, Integer> analyseFile(String filePath) throws IOException {
        int lines = 0;
        int words = 0;
        int numbers = 0;
        int quotedValues = 0;

        try (FileReader reader = new FileReader(filePath, StandardCharsets.UTF_8)) {
            StreamTokenizer tokenizer = new StreamTokenizer(reader);
            tokenizer.eolIsSignificant(true);      // report end-of-line as a token so we can count lines
            tokenizer.quoteChar('"');              // "text in quotes" is one token
            tokenizer.wordChars('@', '@');         // keep emails together as one word
            tokenizer.wordChars('_', '_');
            tokenizer.ordinaryChar(',');           // comma is just a separator

            int token;
            while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
                switch (token) {
                    case StreamTokenizer.TT_WORD:
                        words++;
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        numbers++;
                        break;
                    case StreamTokenizer.TT_EOL:
                        lines++;
                        break;
                    case '"':
                        quotedValues++;
                        break;
                    default:
                        break;   // commas and other symbols are ignored
                }
            }
        }

        Map<String, Integer> statistics = new LinkedHashMap<>();
        statistics.put("Lines", lines);          // autoboxing: int -> Integer
        statistics.put("Words", words);
        statistics.put("Numbers", numbers);
        statistics.put("Quoted values", quotedValues);
        return statistics;
    }

    // =====================================================================
    // CSV helpers
    // =====================================================================

    /**
     * Splits one CSV line into values. Handles values in double quotes that
     * contain commas, e.g.  C1001,"Flat 2, MG Road",30  ->  [C1001] [Flat 2, MG Road] [30]
     */
    public static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (insideQuotes) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');   // "" inside quotes means one real quote character
                    i++;
                } else if (ch == '"') {
                    insideQuotes = false;
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                insideQuotes = true;
            } else if (ch == ',') {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values.toArray(new String[0]);
    }

    /** Joins values into one CSV line, adding quotes only where needed. */
    public static String toCsvLine(String[] values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            String value = values[i] == null ? "" : values[i];
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                line.append('"').append(value.replace("\"", "\"\"")).append('"');
            } else {
                line.append(value);
            }
        }
        return line.toString();
    }

    private void createParentFolder(String filePath) throws IOException {
        File parent = new File(filePath).getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create folder: " + parent);
        }
    }
}
