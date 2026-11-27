package test;

import database.DatabaseManager;
import exceptions.AccountNotFoundException;
import exceptions.CustomerNotFoundException;
import exceptions.DuplicateAccountException;
import exceptions.DuplicateCustomerException;
import exceptions.InsufficientBalanceException;
import exceptions.InvalidAmountException;
import filehandling.FileManager;
import filehandling.ImportResult;
import model.AccountStatus;
import model.AccountType;
import model.BankAccount;
import model.Customer;
import model.Transaction;
import model.TransactionStatus;
import model.TransactionType;
import repository.AccountRepository;
import repository.CustomerRepository;
import repository.TransactionRepository;
import service.AccountService;
import service.CustomerService;
import service.ReportService;
import service.TransactionService;
import util.ValidationUtil;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Automated checks for the core banking logic. Plain Java (no test framework needed).
 *
 * Run with:  ./run-tests.sh   (or run-tests.bat on Windows)
 * It uses a SEPARATE MySQL database (bank_management_test), which it deletes and re-creates
 * at the start of every run, so your real data is never touched.
 * The MySQL user in db.properties needs permission to create databases and triggers.
 * Each check prints PASS or FAIL; the program exits with code 1 if any check fails.
 */
public class BankSystemTestRunner {

    private static int passed;
    private static int failed;
    private static int skipped;
    private static final List<String> failures = new ArrayList<>();

    private static CustomerService customerService;
    private static AccountService accountService;
    private static TransactionService transactionService;
    private static ReportService reportService;
    private static final FileManager fileManager = new FileManager();

    /** A test step that may throw anything. */
    @FunctionalInterface
    interface Step {
        void run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        String testDatabase = DatabaseManager.getDatabaseName();
        if (!testDatabase.endsWith("_test")) {
            System.out.println("Refusing to run: start with -Ddb.name=bank_management_test so real data is not changed.");
            System.exit(2);
        }
        try (Connection server = DatabaseManager.getServerConnection(); Statement statement = server.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS " + testDatabase);   // start from an empty test database
        }
        DatabaseManager.initialiseDatabase();
        createServices();
        accountService.loadAccountSequence();

        System.out.println("=== BANK MANAGEMENT SYSTEM - AUTOMATED TESTS ===");
        System.out.println("Test database: " + DatabaseManager.getDatabaseDescription());
        System.out.println();

        customerTests();
        accountTests();
        moneyTests();
        transferTests();
        closeAndDeleteTests();
        csvTests();
        reportTests();
        concurrencyTests();
        databaseConstraintTests();
        persistenceTest();

        System.out.println();
        System.out.println("=== RESULT: " + passed + " passed, " + failed + " failed"
                + (skipped > 0 ? ", " + skipped + " skipped" : "") + " ===");
        for (String failure : failures) {
            System.out.println("FAILED: " + failure);
        }
        System.exit(failed == 0 ? 0 : 1);
    }

    private static void createServices() {
        CustomerRepository customerRepository = new CustomerRepository();
        AccountRepository accountRepository = new AccountRepository();
        TransactionRepository transactionRepository = new TransactionRepository();
        customerService = new CustomerService(customerRepository);
        transactionService = new TransactionService(transactionRepository);
        accountService = new AccountService(accountRepository, customerRepository, transactionService);
        reportService = new ReportService(customerRepository, accountRepository, transactionRepository);
    }

    // =====================================================================
    // Tests
    // =====================================================================

    private static void customerTests() throws Exception {
        System.out.println("--- Customer management ---");
        customerService.registerCustomer(new Customer("C1001", "  aarav   SHARMA ", 28, "Aarav@Example.com", "98765 43210", "12 MG Road, Bengaluru"));
        Customer saved = customerService.getCustomer("c1001");
        check("TC01 Register customer and search by ID (lower-case input)", "Aarav Sharma", saved.getName());
        check("TC02 Name cleaned to Title Case, email lower-cased, phone spaces removed",
                "Aarav Sharma|aarav@example.com|9876543210", saved.getName() + "|" + saved.getEmail() + "|" + saved.getPhone());
        expectException("TC03 Duplicate customer ID is rejected", DuplicateCustomerException.class,
                () -> customerService.registerCustomer(new Customer("C1001", "Other Person", 30, "other@example.com", "9876500000", "Some Street 1")));
        expectException("TC04 Duplicate email is rejected", DuplicateCustomerException.class,
                () -> customerService.registerCustomer(new Customer("C1002", "Other Person", 30, "aarav@example.com", "9876500000", "Some Street 1")));
        expectException("TC05 Age below 18 is rejected", IllegalArgumentException.class,
                () -> customerService.registerCustomer(new Customer("C1003", "Young Person", 16, "young@example.com", "9876500001", "Some Street 1")));
        expectException("TC06 Invalid phone is rejected", IllegalArgumentException.class,
                () -> ValidationUtil.validatePhone("12345"));
        expectException("TC07 Invalid email is rejected", IllegalArgumentException.class,
                () -> ValidationUtil.validateEmail("not-an-email"));
        expectException("TC08 Search for unknown customer throws CustomerNotFoundException", CustomerNotFoundException.class,
                () -> customerService.getCustomer("C9999"));

        customerService.registerCustomer(new Customer("C1002", "Priya Nair", 34, "priya@example.com", "9845012345", "45 Marine Drive, Kochi"));
        customerService.registerCustomer(new Customer("C1003", "Rohan Mehta", 41, "rohan@example.com", "9123456780", "8 Park Street, Kolkata"));
        Customer update = customerService.getCustomer("C1003");
        update.setAddress("99 New Address, Kolkata");
        customerService.updateCustomer(update);
        check("TC09 Update customer address", "99 New Address, Kolkata", customerService.getCustomer("C1003").getAddress());
        check("TC10 Suggest next customer ID", "C1004", customerService.suggestNextCustomerId());
    }

    private static void accountTests() throws Exception {
        System.out.println("--- Account management ---");
        BankAccount savings = accountService.openAccount("C1001", AccountType.SAVINGS, new BigDecimal("10000.00"));
        check("TC11 Open savings account gets SB number and SavingsAccount class",
                "SB1001|SavingsAccount|10000.00", savings.getAccountNumber() + "|" + savings.getClass().getSimpleName() + "|" + savings.getBalance());
        BankAccount current = accountService.openAccount("C1002", AccountType.CURRENT, new BigDecimal("5000.00"));
        check("TC12 Open current account gets CA number and CurrentAccount class",
                "CA1002|CurrentAccount", current.getAccountNumber() + "|" + current.getClass().getSimpleName());
        expectException("TC13 Second active savings account for same customer is rejected", DuplicateAccountException.class,
                () -> accountService.openAccount("C1001", AccountType.SAVINGS, new BigDecimal("1000.00")));
        expectException("TC14 Opening deposit below minimum is rejected", InvalidAmountException.class,
                () -> accountService.openAccount("C1003", AccountType.CURRENT, new BigDecimal("999.99")));
        expectException("TC15 Account for unknown customer is rejected", CustomerNotFoundException.class,
                () -> accountService.openAccount("C9999", AccountType.SAVINGS, new BigDecimal("1000.00")));
        accountService.openAccount("C1003", AccountType.SAVINGS, new BigDecimal("500.00"));
        check("TC16 Opening deposit saved as a DEPOSIT transaction", "1",
                String.valueOf(transactionService.getTransactions("SB1001", TransactionType.DEPOSIT).size()));
        expectException("TC17 Search unknown account throws AccountNotFoundException", AccountNotFoundException.class,
                () -> accountService.getAccount("SB9999"));
        check("TC18 Savings interest (4% of 10,000) via polymorphism", "400.00",
                accountService.getAccount("SB1001").calculateAnnualInterest().toPlainString());
    }

    private static void moneyTests() throws Exception {
        System.out.println("--- Deposit and withdrawal ---");
        BankAccount afterDeposit = accountService.deposit("sb1001", new BigDecimal("2500.50"));
        check("TC19 Deposit updates balance", "12500.50", afterDeposit.getBalance().toPlainString());
        check("TC20 Deposit is saved in the database", "12500.50", accountService.getAccount("SB1001").getBalance().toPlainString());
        expectException("TC21 Zero deposit rejected", InvalidAmountException.class, () -> ValidationUtil.parseAmount("0"));
        expectException("TC22 Negative amount rejected", InvalidAmountException.class, () -> ValidationUtil.parseAmount("-50"));
        expectException("TC23 Non-numeric amount rejected", InvalidAmountException.class, () -> ValidationUtil.parseAmount("abc"));
        expectException("TC24 More than 2 decimal places rejected", InvalidAmountException.class, () -> ValidationUtil.parseAmount("10.555"));

        BankAccount afterWithdraw = accountService.withdraw("SB1001", new BigDecimal("2000.00"));
        check("TC25 Withdrawal updates balance", "10500.50", afterWithdraw.getBalance().toPlainString());
        expectException("TC26 Withdrawal above balance throws InsufficientBalanceException", InsufficientBalanceException.class,
                () -> accountService.withdraw("SB1003", new BigDecimal("600.00")));
        check("TC27 Balance unchanged after refused withdrawal", "500.00", accountService.getAccount("SB1003").getBalance().toPlainString());
        List<Transaction> failedAttempts = transactionService.getTransactions("SB1003", TransactionType.WITHDRAWAL);
        check("TC28 Refused withdrawal stored with status FAILED", "FAILED",
                failedAttempts.isEmpty() ? "none" : failedAttempts.get(0).getStatus().name());
        accountService.deposit("SB1001", new BigDecimal("60000.00"));
        expectException("TC29 Savings withdrawal above Rs. 50,000 limit rejected", InvalidAmountException.class,
                () -> accountService.withdraw("SB1001", new BigDecimal("50000.01")));
    }

    private static void transferTests() throws Exception {
        System.out.println("--- Money transfer ---");
        BigDecimal senderBefore = accountService.getAccount("SB1001").getBalance();     // 70500.50
        BigDecimal receiverBefore = accountService.getAccount("CA1002").getBalance();   // 5000.00
        String reference = accountService.transfer("SB1001", "CA1002", new BigDecimal("1500.25"));
        check("TC30 Transfer debits sender", senderBefore.subtract(new BigDecimal("1500.25")).toPlainString(),
                accountService.getAccount("SB1001").getBalance().toPlainString());
        check("TC31 Transfer credits receiver", receiverBefore.add(new BigDecimal("1500.25")).toPlainString(),
                accountService.getAccount("CA1002").getBalance().toPlainString());
        List<Transaction> bothSides = transactionService.getTransactionsByReference(reference);
        check("TC32 Transfer creates TRANSFER_OUT and TRANSFER_IN with the same reference", "TRANSFER_OUT,TRANSFER_IN",
                bothSides.size() == 2 ? bothSides.get(0).getType() + "," + bothSides.get(1).getType() : "rows=" + bothSides.size());

        expectException("TC33 Transfer to the same account rejected", IllegalArgumentException.class,
                () -> accountService.transfer("SB1001", "sb1001", new BigDecimal("10.00")));
        BigDecimal smallBefore = accountService.getAccount("SB1003").getBalance();
        expectException("TC34 Transfer with insufficient balance rejected", InsufficientBalanceException.class,
                () -> accountService.transfer("SB1003", "CA1002", new BigDecimal("9999.00")));
        check("TC35 Sender unchanged after refused transfer", smallBefore.toPlainString(),
                accountService.getAccount("SB1003").getBalance().toPlainString());
        expectException("TC36 Transfer to unknown account rejected", AccountNotFoundException.class,
                () -> accountService.transfer("SB1001", "CA9999", new BigDecimal("10.00")));

        // ROLLBACK TEST: make the receiver's balance update fail AFTER the sender was debited.
        // A temporary MySQL trigger refuses any balance change on CA1002. The whole transfer must be undone.
        try (Connection connection = DatabaseManager.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TRIGGER fail_receiver_update BEFORE UPDATE ON accounts FOR EACH ROW "
                    + "BEGIN IF NEW.account_number = 'CA1002' AND NEW.balance <> OLD.balance THEN "
                    + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Simulated failure while crediting receiver'; "
                    + "END IF; END");
        } catch (java.sql.SQLException e) {
            // Creating a trigger needs extra rights when MySQL binary logging is on (root has them).
            skip("TC37-TC40 rollback test", "this MySQL user may not create triggers (" + e.getMessage()
                    + "). Run the tests as root, or set log_bin_trust_function_creators=1.");
            return;
        }
        BigDecimal senderBeforeRollback = accountService.getAccount("SB1001").getBalance();
        BigDecimal receiverBeforeRollback = accountService.getAccount("CA1002").getBalance();
        long transactionsBefore = transactionService.countTransactions();
        String rollbackError = "no exception";
        try {
            accountService.transfer("SB1001", "CA1002", new BigDecimal("700.00"));
        } catch (Exception e) {
            // Drivers may add their own prefix to the message, so we only check that our text is inside it
            boolean expected = e instanceof java.sql.SQLException && String.valueOf(e.getMessage())
                    .contains("Simulated failure while crediting receiver");
            rollbackError = expected ? "SQLException: Simulated failure while crediting receiver"
                    : e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        try (Connection connection = DatabaseManager.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TRIGGER fail_receiver_update");
        }
        check("TC37 Failure while crediting receiver raises an error",
                "SQLException: Simulated failure while crediting receiver", rollbackError);
        check("TC38 ROLLBACK: sender balance restored after failed credit", senderBeforeRollback.toPlainString(),
                accountService.getAccount("SB1001").getBalance().toPlainString());
        check("TC39 ROLLBACK: receiver balance unchanged", receiverBeforeRollback.toPlainString(),
                accountService.getAccount("CA1002").getBalance().toPlainString());
        check("TC40 ROLLBACK: no TRANSFER_OUT row left behind", String.valueOf(transactionsBefore),
                String.valueOf(transactionService.countTransactions()));
    }

    private static void closeAndDeleteTests() throws Exception {
        System.out.println("--- Close account and delete customer ---");
        expectException("TC41 Closing an account with money is rejected", IllegalStateException.class,
                () -> accountService.closeAccount("SB1003"));
        accountService.withdraw("SB1003", new BigDecimal("500.00"));
        accountService.closeAccount("SB1003");
        check("TC42 Account with zero balance can be closed", "CLOSED", accountService.getAccount("SB1003").getStatus().name());
        expectException("TC43 Deposit into closed account rejected", IllegalStateException.class,
                () -> accountService.deposit("SB1003", new BigDecimal("10.00")));
        expectException("TC44 Transfer into closed account rejected", IllegalStateException.class,
                () -> accountService.transfer("SB1001", "SB1003", new BigDecimal("10.00")));
        expectException("TC45 Customer with accounts cannot be deleted", IllegalStateException.class,
                () -> customerService.deleteCustomer("C1001"));
        customerService.registerCustomer(new Customer("C1009", "Temp Customer", 22, "temp@example.com", "9000000009", "Temporary Street"));
        customerService.deleteCustomer("C1009");
        check("TC46 Customer without accounts can be deleted", "false", String.valueOf(customerService.customerExists("C1009")));
    }

    private static void csvTests() throws Exception {
        System.out.println("--- CSV import / export ---");
        String exportFile = "data/test_export_customers.csv";
        int exported = fileManager.exportCustomers(customerService.getAllCustomers(), exportFile);
        ImportResult<Customer> reRead = fileManager.readCustomers(exportFile);
        check("TC47 Exported customers can be read back (address with comma kept)", exported + "|12 MG Road, Bengaluru",
                reRead.getValidRecords().size() + "|" + reRead.getValidRecords().get(0).getAddress());
        new File(exportFile).delete();

        ImportResult<Customer> invalid = fileManager.readCustomers("data/sample/customers_invalid.csv");
        customerService.importCustomers(invalid);
        check("TC48 Invalid customer CSV: 1 imported, 7 problems reported", "1|7",
                invalid.getImportedCount() + "|" + invalid.getErrors().size());

        ImportResult<BankAccount> invalidAccounts = fileManager.readAccounts("data/sample/accounts_invalid.csv");
        accountService.importAccounts(invalidAccounts);
        check("TC49 Invalid account CSV: 1 imported, 8 problems reported", "1|8",
                invalidAccounts.getImportedCount() + "|" + invalidAccounts.getErrors().size());
        BankAccount next = accountService.openAccount("C1002", AccountType.SAVINGS, new BigDecimal("800.00"));
        check("TC50 Account numbers continue after imported SB2001", "SB2002", next.getAccountNumber());
        Map<String, Integer> statistics = fileManager.analyseFile("data/sample/accounts_sample.csv");
        check("TC51 StreamTokenizer counts 10 lines in accounts_sample.csv", "10", String.valueOf(statistics.get("Lines")));
    }

    private static void reportTests() throws Exception {
        System.out.println("--- Reports (streams) ---");
        Map<String, String> summary = reportService.getBankSummary();
        check("TC52 Report: total customers", String.valueOf(customerService.getAllCustomers().size()),
                summary.get("Total registered customers"));
        List<BankAccount> active = new ArrayList<>();
        for (BankAccount account : accountService.getAllAccounts()) {
            if (account.isActive()) {
                active.add(account);
            }
        }
        check("TC53 Report: total active accounts", String.valueOf(active.size()), summary.get("Total active accounts"));
        List<BankAccount> top = reportService.getTopAccountsByBalance(5);
        boolean sortedDescending = true;
        for (int i = 1; i < top.size(); i++) {
            sortedDescending &= top.get(i - 1).getBalance().compareTo(top.get(i).getBalance()) >= 0;
        }
        check("TC54 Top accounts sorted highest first and at most 5", "true", String.valueOf(sortedDescending && top.size() <= 5));
        List<Customer> alphabetical = reportService.getCustomersAlphabetical();
        check("TC55 Customers alphabetical: first is Aarav Sharma", "Aarav Sharma", alphabetical.get(0).getName());
        check("TC56 Generic sumBalances equals manual total", manualTotal(active).toPlainString(),
                ReportService.sumBalances(active).toPlainString());
    }

    private static void concurrencyTests() throws Exception {
        System.out.println("--- Concurrency ---");
        BigDecimal start = accountService.getAccount("CA1002").getBalance();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 25; i++) {
                    try {
                        accountService.deposit("CA1002", new BigDecimal("10.00"), "Concurrency test");
                    } catch (Exception e) {
                        System.out.println("   thread error: " + e.getMessage());
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        check("TC57 4 threads x 25 deposits of Rs. 10: no lost updates", start.add(new BigDecimal("1000.00")).toPlainString(),
                accountService.getAccount("CA1002").getBalance().toPlainString());
        long successfulConcurrent = transactionService.getTransactions("CA1002", TransactionType.DEPOSIT).stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS
                        && "Concurrency test".equals(transaction.getDescription()))
                .count();
        check("TC58 All 100 concurrent deposits recorded", "100", String.valueOf(successfulConcurrent));

        // ROW LOCK TEST: another connection locks SB1001 with SELECT ... FOR UPDATE and holds the lock
        // for 800 ms. A deposit started meanwhile must WAIT for that lock, then succeed.
        BigDecimal before = accountService.getAccount("SB1001").getBalance();
        long waitedMillis;
        try (Connection other = DatabaseManager.getConnection()) {
            other.setAutoCommit(false);
            try (Statement lock = other.createStatement()) {
                lock.executeQuery("SELECT balance FROM accounts WHERE account_number = 'SB1001' FOR UPDATE").close();
            }
            long[] depositTime = new long[1];
            Thread depositor = new Thread(() -> {
                long startedAt = System.currentTimeMillis();
                try {
                    accountService.deposit("SB1001", new BigDecimal("1.00"), "Row lock test");
                } catch (Exception e) {
                    System.out.println("   deposit error: " + e.getMessage());
                }
                depositTime[0] = System.currentTimeMillis() - startedAt;
            });
            depositor.start();
            Thread.sleep(800);
            other.commit();          // releases the row lock
            depositor.join();
            waitedMillis = depositTime[0];
        }
        check("TC65 Deposit waits while another connection holds a FOR UPDATE row lock (>= 700 ms)", "true",
                String.valueOf(waitedMillis >= 700));
        check("TC66 Deposit completes correctly after the lock is released", before.add(new BigDecimal("1.00")).toPlainString(),
                accountService.getAccount("SB1001").getBalance().toPlainString());
        check("TC67 DATE column round-trip: new account opened today", java.time.LocalDate.now().toString(),
                accountService.getAccount("SB2002").getCreationDate().toString());
    }

    /** The database itself must refuse bad data, even if Java code had a bug. */
    private static void databaseConstraintTests() {
        System.out.println("--- Database constraints ---");
        expectException("TC61 DB CHECK rule refuses a negative balance", java.sql.SQLException.class,
                () -> runSql("UPDATE accounts SET balance = '-1.00' WHERE account_number = 'SB1001'"));
        expectException("TC62 DB FOREIGN KEY refuses an account for a missing customer", java.sql.SQLException.class,
                () -> runSql("INSERT INTO accounts VALUES ('SB7777', 'C7777', 'SAVINGS', '10.00', 'ACTIVE', '2026-01-01')"));
        expectException("TC63 DB CHECK rule refuses a zero transaction amount", java.sql.SQLException.class,
                () -> runSql("INSERT INTO transactions (account_number, transaction_type, amount, transaction_date, status, reference_id) "
                        + "VALUES ('SB1001', 'DEPOSIT', '0', '2026-01-01 10:00:00', 'SUCCESS', 'X')"));
        expectException("TC64 DB PRIMARY KEY refuses a duplicate account number", java.sql.SQLException.class,
                () -> runSql("INSERT INTO accounts VALUES ('SB1001', 'C1001', 'SAVINGS', '10.00', 'ACTIVE', '2026-01-01')"));
    }

    private static void runSql(String sql) throws Exception {
        try (Connection connection = DatabaseManager.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static void persistenceTest() throws Exception {
        System.out.println("--- Persistence ---");
        BigDecimal balance = accountService.getAccount("SB1001").getBalance();
        createServices();   // brand-new service and repository objects, nothing kept in memory
        check("TC59 Data read back by new objects from the database file", balance.toPlainString(),
                accountService.getAccount("SB1001").getBalance().toPlainString());
        check("TC60 Closed status persisted", AccountStatus.CLOSED.name(), accountService.getAccount("SB1003").getStatus().name());
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static BigDecimal manualTotal(List<BankAccount> accounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BankAccount account : accounts) {
            total = total.add(account.getBalance());
        }
        return total;
    }

    private static void skip(String name, String reason) {
        skipped++;
        System.out.println("SKIP  " + name + ": " + reason);
    }

    private static void check(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("PASS  " + name + "  [" + actual + "]");
        } else {
            failed++;
            failures.add(name + " (expected " + expected + ", got " + actual + ")");
            System.out.println("FAIL  " + name + "  expected [" + expected + "] but got [" + actual + "]");
        }
    }

    private static void expectException(String name, Class<? extends Exception> expectedType, Step step) {
        String actual;
        try {
            step.run();
            actual = "no exception";
        } catch (Exception e) {
            actual = expectedType.isInstance(e) ? expectedType.getSimpleName() : e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        check(name, expectedType.getSimpleName(), actual);
    }
}
