package main;

import collections.CollectionsDemo;
import collections.JavaConceptsDemo;
import database.DatabaseManager;
import exceptions.AccountNotFoundException;
import exceptions.CustomerNotFoundException;
import exceptions.DuplicateAccountException;
import exceptions.DuplicateCustomerException;
import exceptions.InsufficientBalanceException;
import exceptions.InvalidAmountException;
import filehandling.FileManager;
import filehandling.ImportResult;
import model.AccountType;
import model.BankAccount;
import model.Customer;
import model.Transaction;
import model.TransactionType;
import multithreading.MultithreadingDemo;
import repository.AccountRepository;
import repository.CustomerRepository;
import repository.TransactionRepository;
import service.AccountService;
import service.CustomerService;
import service.ReportService;
import service.TransactionService;
import util.ConsoleUtil;
import util.InputHelper;
import util.ValidationUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Entry point of the Bank Management System.
 *
 * Main only handles the MENUS (input and output). All rules live in the
 * service classes, and all SQL lives in the repository classes:
 *
 *     Main (menus)  ->  Services (business rules)  ->  Repositories (SQL)  ->  SQLite database
 *
 * Every menu option runs through runSafely(...), which catches every expected
 * exception and prints a clear message, so the program never stops unexpectedly.
 */
public class Main {

    private static final String[] CUSTOMER_HEADERS = {"Customer ID", "Name", "Age", "Email", "Phone", "Address"};
    private static final String[] ACCOUNT_HEADERS =
            {"Account No", "Customer ID", "Holder Name", "Type", "Balance", "Status", "Opened On"};
    private static final String[] TRANSACTION_HEADERS =
            {"ID", "Date & Time", "Account", "Type", "Amount", "Status", "Description", "Reference"};
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** A menu action may throw any exception; runSafely decides what message to show. */
    @FunctionalInterface
    private interface MenuAction {
        void run() throws Exception;
    }

    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final ReportService reportService;
    private final FileManager fileManager;
    private final MultithreadingDemo multithreadingDemo;

    /** Creates every object the program needs and connects them together. */
    public Main() {
        CustomerRepository customerRepository = new CustomerRepository();
        AccountRepository accountRepository = new AccountRepository();
        TransactionRepository transactionRepository = new TransactionRepository();

        this.customerService = new CustomerService(customerRepository);
        this.transactionService = new TransactionService(transactionRepository);
        this.accountService = new AccountService(accountRepository, customerRepository, transactionService);
        this.reportService = new ReportService(customerRepository, accountRepository, transactionRepository);
        this.fileManager = new FileManager();
        this.multithreadingDemo = new MultithreadingDemo(accountService, reportService, fileManager);
    }

    public static void main(String[] args) {
        Main application = new Main();
        try {
            DatabaseManager.initialiseDatabase();
            application.accountService.loadAccountSequence();
            ConsoleUtil.printInfo("Connected to database: " + DatabaseManager.getDatabasePath());
        } catch (SQLException | IOException e) {
            ConsoleUtil.printError("Could not start the application: " + e.getMessage());
            return;
        }
        application.runMainMenu();
    }

    // =====================================================================
    // Main menu
    // =====================================================================

    private void runMainMenu() {
        boolean running = true;
        while (running) {
            try {
                printMainMenu();
                int choice = InputHelper.readMenuChoice(15);
                switch (choice) {
                    case 1:  customerMenu(); break;
                    case 2:  accountCreationMenu(); break;
                    case 3:  runSafely(this::viewAllAccounts); break;
                    case 4:  runSafely(this::searchAccount); break;
                    case 5:  runSafely(this::depositMoney); break;
                    case 6:  runSafely(this::withdrawMoney); break;
                    case 7:  runSafely(this::transferMoney); break;
                    case 8:  runSafely(this::checkBalance); break;
                    case 9:  transactionHistoryMenu(); break;
                    case 10: reportsMenu(); break;
                    case 11: sortCustomersMenu(); break;
                    case 12: csvMenu(); break;
                    case 13: collectionsMenu(); break;
                    case 14: multithreadingMenu(); break;
                    case 15: running = false; break;
                    default: ConsoleUtil.printError("Invalid choice.");
                }
            } catch (NoSuchElementException e) {
                // Happens only when the input stream ends (e.g. input piped from a file)
                System.out.println();
                ConsoleUtil.printInfo("No more input available.");
                running = false;
            }
        }
        System.out.println();
        System.out.println("Thank you for banking with " + BankAccount.BANK_NAME + ". Goodbye!");
    }

    private void printMainMenu() {
        ConsoleUtil.printHeader("Bank Management System");
        System.out.println();
        System.out.println("1. Customer Management");
        System.out.println("2. Create Bank Account");
        System.out.println("3. View All Bank Accounts");
        System.out.println("4. Search Bank Account");
        System.out.println("5. Deposit Money");
        System.out.println("6. Withdraw Money");
        System.out.println("7. Transfer Money");
        System.out.println("8. Check Account Balance");
        System.out.println("9. Transaction History");
        System.out.println("10. Banking Reports");
        System.out.println("11. Sort Customer Records");
        System.out.println("12. Import / Export CSV");
        System.out.println("13. Collections Demonstration");
        System.out.println("14. Multithreading Demonstration");
        System.out.println("15. Exit");
        System.out.println();
        System.out.println(ConsoleUtil.line('=', ConsoleUtil.MENU_WIDTH));
    }

    /** Prints a numbered submenu. The last option is always "Return to Main Menu". */
    private int showSubMenu(String title, String... options) {
        ConsoleUtil.printHeader(title);
        for (int i = 0; i < options.length; i++) {
            System.out.println((i + 1) + ". " + options[i]);
        }
        System.out.println((options.length + 1) + ". Return to Main Menu");
        System.out.println(ConsoleUtil.line('=', ConsoleUtil.MENU_WIDTH));
        return InputHelper.readMenuChoice(options.length + 1);
    }

    /**
     * Runs one menu action and turns every expected exception into a friendly message.
     * This is the single place where errors are reported (no duplicated try/catch in every method).
     */
    private void runSafely(MenuAction action) {
        try {
            action.run();
        } catch (NoSuchElementException e) {
            throw e;   // input ended: let the main loop finish the program
        } catch (CustomerNotFoundException | AccountNotFoundException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (DuplicateCustomerException | DuplicateAccountException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (InsufficientBalanceException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (InvalidAmountException e) {
            ConsoleUtil.printError("Invalid amount: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (SQLException e) {
            ConsoleUtil.printError("Database error: " + e.getMessage());
        } catch (IOException e) {
            ConsoleUtil.printError("File error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ConsoleUtil.printError("The operation was interrupted.");
        } catch (Exception e) {
            ConsoleUtil.printError("Unexpected error: " + e);
        }
    }

    // =====================================================================
    // 1. Customer management
    // =====================================================================

    private void customerMenu() {
        while (true) {
            int choice = showSubMenu("Customer Management",
                    "Register Customer", "View All Customers", "Search Customer", "Update Customer", "Delete Customer");
            switch (choice) {
                case 1: runSafely(this::registerCustomer); break;
                case 2: runSafely(this::viewAllCustomers); break;
                case 3: runSafely(this::searchCustomer); break;
                case 4: runSafely(this::updateCustomer); break;
                case 5: runSafely(this::deleteCustomer); break;
                default: return;
            }
        }
    }

    private void registerCustomer() throws SQLException, DuplicateCustomerException {
        ConsoleUtil.printSubHeader("Register New Customer");
        String suggestedId = customerService.suggestNextCustomerId();
        String customerId = ValidationUtil.validateCustomerId(
                InputHelper.readLineOrDefault("Customer ID (press Enter to use the suggestion)", suggestedId));
        if (customerService.customerExists(customerId)) {
            throw new DuplicateCustomerException("Customer ID " + customerId + " is already registered.");
        }
        String name = InputHelper.readValid("Full Name   : ", ValidationUtil::validateName);
        int age = InputHelper.readValid("Age         : ", ValidationUtil::validateAge);   // autoboxing Integer -> int
        String email = InputHelper.readValid("Email       : ", ValidationUtil::validateEmail);
        String phone = InputHelper.readValid("Phone       : ", ValidationUtil::validatePhone);
        String address = InputHelper.readValid("Address     : ", ValidationUtil::validateAddress);

        Customer customer = new Customer(customerId, name, age, email, phone, address);
        customerService.registerCustomer(customer);
        ConsoleUtil.printSuccess("Customer registered successfully.");
        customer.display();
    }

    private void viewAllCustomers() throws SQLException {
        ConsoleUtil.printSubHeader("All Customers");
        ConsoleUtil.printTable(CUSTOMER_HEADERS, customerService.getAllCustomers());
    }

    private void searchCustomer() throws SQLException, CustomerNotFoundException {
        String customerId = InputHelper.readLine("Enter Customer ID: ");
        Customer customer = customerService.getCustomer(customerId);
        ConsoleUtil.printSubHeader("Customer Details");
        customer.display();
        System.out.println("\nAccounts of this customer:");
        ConsoleUtil.printTable(ACCOUNT_HEADERS, accountService.getAccountsForCustomer(customer.getCustomerId()));
    }

    private void updateCustomer() throws SQLException, CustomerNotFoundException, DuplicateCustomerException {
        String customerId = InputHelper.readLine("Enter Customer ID to update: ");
        Customer customer = customerService.getCustomer(customerId);
        ConsoleUtil.printSubHeader("Update Customer " + customer.getCustomerId());
        System.out.println("Press Enter to keep the current value shown in [brackets].");
        customer.setName(InputHelper.readValidOrKeep("Full Name", customer.getName(), ValidationUtil::validateName));
        customer.setAge(InputHelper.readValidOrKeep("Age", customer.getAge(), ValidationUtil::validateAge));
        customer.setEmail(InputHelper.readValidOrKeep("Email", customer.getEmail(), ValidationUtil::validateEmail));
        customer.setPhone(InputHelper.readValidOrKeep("Phone", customer.getPhone(), ValidationUtil::validatePhone));
        customer.setAddress(InputHelper.readValidOrKeep("Address", customer.getAddress(), ValidationUtil::validateAddress));
        customerService.updateCustomer(customer);
        ConsoleUtil.printSuccess("Customer details updated.");
        customer.display();
    }

    private void deleteCustomer() throws SQLException, CustomerNotFoundException {
        String customerId = InputHelper.readLine("Enter Customer ID to delete: ");
        Customer customer = customerService.getCustomer(customerId);
        customer.display();
        if (InputHelper.readYesNo("Are you sure you want to delete this customer?")) {
            customerService.deleteCustomer(customer.getCustomerId());
            ConsoleUtil.printSuccess("Customer " + customer.getCustomerId() + " deleted.");
        } else {
            ConsoleUtil.printInfo("Delete cancelled.");
        }
    }

    // =====================================================================
    // 2. Create / close accounts
    // =====================================================================

    private void accountCreationMenu() {
        while (true) {
            int choice = showSubMenu("Create Bank Account",
                    "Create Savings Account", "Create Current Account", "Close an Account", "Check Account Status");
            switch (choice) {
                case 1: runSafely(() -> createAccount(AccountType.SAVINGS)); break;
                case 2: runSafely(() -> createAccount(AccountType.CURRENT)); break;
                case 3: runSafely(this::closeAccount); break;
                case 4: runSafely(this::checkAccountStatus); break;
                default: return;
            }
        }
    }

    private void createAccount(AccountType type)
            throws SQLException, CustomerNotFoundException, DuplicateAccountException {
        ConsoleUtil.printSubHeader("Open " + type.getLabel() + " Account");
        if (type == AccountType.SAVINGS) {
            System.out.println("Rules: minimum opening deposit Rs. 500.00, 4% yearly interest, Rs. 50,000 withdrawal limit.");
        } else {
            System.out.println("Rules: minimum opening deposit Rs. 1,000.00, no interest, Rs. 2,00,000 withdrawal limit.");
        }
        Customer customer = customerService.getCustomer(InputHelper.readLine("Customer ID: "));
        System.out.println("Customer: " + customer.getName());
        BigDecimal openingDeposit = InputHelper.readAmount("Opening deposit amount: ");

        BankAccount account = accountService.openAccount(customer.getCustomerId(), type, openingDeposit);
        ConsoleUtil.printSuccess(type.getLabel() + " account created. Account number: " + account.getAccountNumber());
        account.display();   // calls SavingsAccount or CurrentAccount version (runtime polymorphism)
    }

    private void closeAccount() throws SQLException, AccountNotFoundException {
        BankAccount account = accountService.getAccount(InputHelper.readLine("Account number to close: "));
        account.display();
        if (InputHelper.readYesNo("Close this account?")) {
            accountService.closeAccount(account.getAccountNumber());
            ConsoleUtil.printSuccess("Account " + account.getAccountNumber() + " is now CLOSED.");
        } else {
            ConsoleUtil.printInfo("Account not closed.");
        }
    }

    private void checkAccountStatus() throws SQLException, AccountNotFoundException {
        BankAccount account = accountService.getAccount(InputHelper.readLine("Account number: "));
        System.out.println("Account " + account.getAccountNumber() + " (" + account.getAccountHolderName()
                + ") status: " + account.getStatus()
                + (account.isActive() ? " - open for transactions" : " - no transactions allowed"));
    }

    // =====================================================================
    // 3-8. Accounts and money
    // =====================================================================

    private void viewAllAccounts() throws SQLException {
        ConsoleUtil.printSubHeader("All Bank Accounts");
        ConsoleUtil.printTable(ACCOUNT_HEADERS, accountService.getAllAccounts());
    }

    private void searchAccount() throws SQLException, AccountNotFoundException, CustomerNotFoundException {
        BankAccount account = accountService.getAccount(InputHelper.readLine("Enter account number: "));
        ConsoleUtil.printSubHeader("Account Details");
        account.display();
        ConsoleUtil.printSubHeader("Account Holder Information");
        customerService.getCustomer(account.getCustomerId()).display();
    }

    private void depositMoney() throws SQLException, AccountNotFoundException {
        ConsoleUtil.printSubHeader("Deposit Money");
        BankAccount account = accountService.getAccount(InputHelper.readLine("Account number: "));
        showAccountLine(account);
        requireActive(account);
        BigDecimal amount = InputHelper.readAmount("Deposit amount: ");
        BankAccount updated = accountService.deposit(account.getAccountNumber(), amount);
        ConsoleUtil.printSuccess(ConsoleUtil.formatMoney(amount) + " deposited. Updated balance: "
                + ConsoleUtil.formatMoney(updated.getBalance()));
    }

    private void withdrawMoney() throws SQLException, AccountNotFoundException {
        ConsoleUtil.printSubHeader("Withdraw Money");
        BankAccount account = accountService.getAccount(InputHelper.readLine("Account number: "));
        showAccountLine(account);
        requireActive(account);
        BigDecimal amount = InputHelper.readAmount("Withdrawal amount: ");
        try {
            BankAccount updated = accountService.withdraw(account.getAccountNumber(), amount);
            ConsoleUtil.printSuccess(ConsoleUtil.formatMoney(amount) + " withdrawn. Updated balance: "
                    + ConsoleUtil.formatMoney(updated.getBalance()));
        } catch (InsufficientBalanceException e) {
            ConsoleUtil.printError(e.getMessage());
            ConsoleUtil.printInfo("Balance unchanged. The refused attempt was saved in the history as FAILED.");
        }
    }

    private void transferMoney() throws SQLException, AccountNotFoundException {
        ConsoleUtil.printSubHeader("Transfer Money");
        BankAccount sender = accountService.getAccount(InputHelper.readLine("Sender account number  : "));
        showAccountLine(sender);
        requireActive(sender);
        BankAccount receiver = accountService.getAccount(InputHelper.readLine("Receiver account number: "));
        showAccountLine(receiver);
        requireActive(receiver);
        if (sender.getAccountNumber().equals(receiver.getAccountNumber())) {
            ConsoleUtil.printError("Sender and receiver accounts must be different.");
            return;
        }
        BigDecimal amount = InputHelper.readAmount("Transfer amount: ");

        if (!InputHelper.readYesNo("Transfer " + ConsoleUtil.formatMoney(amount) + " from "
                + sender.getAccountNumber() + " to " + receiver.getAccountNumber() + "?")) {
            ConsoleUtil.printInfo("Transfer cancelled by user.");
            return;
        }
        try {
            String referenceId = accountService.transfer(sender.getAccountNumber(), receiver.getAccountNumber(), amount);
            ConsoleUtil.printSuccess("Transfer completed. Reference ID: " + referenceId);
            System.out.println("Sender   " + sender.getAccountNumber() + " new balance: "
                    + ConsoleUtil.formatMoney(accountService.getAccount(sender.getAccountNumber()).getBalance()));
            System.out.println("Receiver " + receiver.getAccountNumber() + " new balance: "
                    + ConsoleUtil.formatMoney(accountService.getAccount(receiver.getAccountNumber()).getBalance()));
        } catch (InsufficientBalanceException e) {
            ConsoleUtil.printError(e.getMessage());
            ConsoleUtil.printInfo("Transfer cancelled. No money was moved. The attempt was saved as FAILED.");
        } catch (SQLException e) {
            ConsoleUtil.printError("Transfer failed: " + e.getMessage());
            ConsoleUtil.printInfo("The database transaction was rolled back. No money was moved.");
        } catch (IllegalArgumentException | IllegalStateException | InvalidAmountException e) {
            ConsoleUtil.printError("Transfer refused: " + e.getMessage());
            ConsoleUtil.printInfo("No money was moved.");
        }
    }

    private void checkBalance() throws SQLException, AccountNotFoundException {
        BankAccount account = accountService.getAccount(InputHelper.readLine("Enter account number: "));
        ConsoleUtil.printSubHeader("Balance Inquiry");
        System.out.println("Account Number    : " + account.getAccountNumber());
        System.out.println("Account Holder    : " + account.getAccountHolderName());
        System.out.println("Account Type      : " + account.getAccountType().getLabel());
        System.out.println("Available Balance : " + ConsoleUtil.formatMoney(account.getBalance()));
        System.out.println("Account Status    : " + account.getStatus());
        // Polymorphism: SavingsAccount calculates 4% interest, CurrentAccount returns zero
        System.out.println("Yearly Interest   : " + ConsoleUtil.formatMoney(account.calculateAnnualInterest()) + " (estimate)");
    }

    /** Stops early (before asking for an amount) if the account is closed. */
    private void requireActive(BankAccount account) {
        if (!account.isActive()) {
            throw new IllegalStateException("Account " + account.getAccountNumber() + " is closed. No transactions allowed.");
        }
    }

    private void showAccountLine(BankAccount account) {
        System.out.println("   -> " + account.getAccountHolderName() + " | " + account.getAccountType().getLabel()
                + " | Balance " + ConsoleUtil.formatMoney(account.getBalance()) + " | " + account.getStatus());
    }

    // =====================================================================
    // 9. Transaction history
    // =====================================================================

    private void transactionHistoryMenu() {
        while (true) {
            int choice = showSubMenu("Transaction History",
                    "View All Transactions", "View Transactions of an Account", "View Deposits", "View Withdrawals",
                    "View Incoming Transfers", "View Outgoing Transfers", "View Latest Transactions",
                    "View Both Sides of a Transfer (by Reference ID)");
            switch (choice) {
                case 1: runSafely(() -> showTransactions("All Transactions", transactionService.getTransactions())); break;
                case 2: runSafely(this::showAccountTransactions); break;
                case 3: runSafely(() -> showTransactionsOfType(TransactionType.DEPOSIT)); break;
                case 4: runSafely(() -> showTransactionsOfType(TransactionType.WITHDRAWAL)); break;
                case 5: runSafely(() -> showTransactionsOfType(TransactionType.TRANSFER_IN)); break;
                case 6: runSafely(() -> showTransactionsOfType(TransactionType.TRANSFER_OUT)); break;
                case 7: runSafely(this::showLatestTransactions); break;
                case 8: runSafely(this::showTransferByReference); break;
                default: return;
            }
        }
    }

    private void showTransactions(String title, List<Transaction> transactions) {
        ConsoleUtil.printSubHeader(title);
        ConsoleUtil.printTable(TRANSACTION_HEADERS, transactions);
    }

    private void showAccountTransactions() throws SQLException, AccountNotFoundException {
        BankAccount account = accountService.getAccount(InputHelper.readLine("Account number: "));
        showTransactions("Transactions of " + account.getAccountNumber() + " (" + account.getAccountHolderName() + ")",
                transactionService.getTransactions(account.getAccountNumber()));
    }

    /** Uses the overloaded getTransactions(type) or getTransactions(account, type). */
    private void showTransactionsOfType(TransactionType type) throws SQLException, AccountNotFoundException {
        String accountNumber = InputHelper.readLine("Account number (press Enter for all accounts): ");
        if (accountNumber.isEmpty()) {
            showTransactions(type.getLabel() + " - all accounts", transactionService.getTransactions(type));
        } else {
            BankAccount account = accountService.getAccount(accountNumber);
            showTransactions(type.getLabel() + " - " + account.getAccountNumber(),
                    transactionService.getTransactions(account.getAccountNumber(), type));
        }
    }

    private void showLatestTransactions() throws SQLException {
        int count = InputHelper.readInt("How many latest transactions (1-100)? ", 1, 100);
        showTransactions("Latest " + count + " Transactions (newest first)", transactionService.getLatestTransactions(count));
    }

    private void showTransferByReference() throws SQLException {
        String referenceId = InputHelper.readLine("Reference ID (e.g. TRF-20261120143005-001): ").toUpperCase();
        showTransactions("Transactions with reference " + referenceId,
                transactionService.getTransactionsByReference(referenceId));
    }

    // =====================================================================
    // 10. Reports
    // =====================================================================

    private void reportsMenu() {
        while (true) {
            int choice = showSubMenu("Banking Reports",
                    "Bank Summary (all key figures)", "Top 5 Account Holders by Balance",
                    "Customer List in Alphabetical Order", "Account Summary by Type", "Transaction Summary by Type",
                    "Browse Accounts Page by Page", "Save Full Report to a Text File");
            switch (choice) {
                case 1: runSafely(this::showBankSummary); break;
                case 2: runSafely(this::showTopAccounts); break;
                case 3: runSafely(this::showCustomersAlphabetical); break;
                case 4: runSafely(() -> showGroupedTable("Account Summary by Type (active accounts)",
                        new String[] {"Type", "Accounts", "Total Balance", "Average Balance"},
                        reportService.getAccountSummaryByType())); break;
                case 5: runSafely(() -> showGroupedTable("Transaction Summary by Type (successful only)",
                        new String[] {"Type", "Count", "Total Amount"},
                        reportService.getTransactionSummaryByType())); break;
                case 6: runSafely(this::browseAccountPages); break;
                case 7: runSafely(this::saveReportToFile); break;
                default: return;
            }
        }
    }

    private void showBankSummary() throws SQLException {
        ConsoleUtil.printSubHeader("Bank Summary");
        Map<String, String> summary = reportService.getBankSummary();
        ConsoleUtil.printKeyValues(summary);
    }

    private void showTopAccounts() throws SQLException {
        ConsoleUtil.printSubHeader("Top 5 Account Holders by Balance");
        ConsoleUtil.printTable(ACCOUNT_HEADERS, reportService.getTopAccountsByBalance(5));
    }

    private void showCustomersAlphabetical() throws SQLException {
        ConsoleUtil.printSubHeader("Customers in Alphabetical Order");
        ConsoleUtil.printTable(CUSTOMER_HEADERS, reportService.getCustomersAlphabetical());
    }

    private void showGroupedTable(String title, String[] headers, String[][] rows) {
        ConsoleUtil.printSubHeader(title);
        ConsoleUtil.printTable(headers, rows);
    }

    private void browseAccountPages() throws SQLException {
        int pageSize = 3;
        int totalPages = reportService.getTotalPages(pageSize);
        int page = 1;
        while (true) {
            ConsoleUtil.printSubHeader("Accounts - Page " + page + " of " + totalPages + " (" + pageSize + " per page)");
            ConsoleUtil.printTable(ACCOUNT_HEADERS, reportService.getAccountsPage(page, pageSize));
            String command = InputHelper.readLine("[N]ext, [P]revious, [Q]uit: ").toUpperCase();
            if (command.equals("N") && page < totalPages) {
                page++;
            } else if (command.equals("P") && page > 1) {
                page--;
            } else if (command.equals("Q")) {
                return;
            } else {
                ConsoleUtil.printInfo("No more pages in that direction (or unknown command).");
            }
        }
    }

    private void saveReportToFile() throws SQLException, IOException {
        String path = "data/reports/bank_report_" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".txt";
        String report = reportService.buildFullReportText();
        fileManager.writeTextFile(path, report);
        System.out.println(report);
        ConsoleUtil.printSuccess("Report saved to " + path);
    }

    // =====================================================================
    // 11. Sort customers
    // =====================================================================

    private void sortCustomersMenu() {
        while (true) {
            int choice = showSubMenu("Sort Customer Records",
                    "Sort by Customer ID (Comparable)", "Sort by Name (Comparator)",
                    "Sort by Age (Comparator)", "Sort by Email (Comparator)");
            if (choice == 5) {
                return;
            }
            runSafely(() -> {
                boolean descending = InputHelper.readYesNo("Descending order?");
                List<Customer> sorted;
                switch (choice) {
                    case 1: sorted = customerService.getCustomersSortedById(descending); break;
                    case 2: sorted = customerService.getCustomersSorted(CustomerService.BY_NAME, descending); break;
                    case 3: sorted = customerService.getCustomersSorted(CustomerService.BY_AGE, descending); break;
                    default: sorted = customerService.getCustomersSorted(CustomerService.BY_EMAIL, descending); break;
                }
                ConsoleUtil.printSubHeader("Sorted Customers" + (descending ? " (descending)" : " (ascending)"));
                ConsoleUtil.printTable(CUSTOMER_HEADERS, sorted);
            });
        }
    }

    // =====================================================================
    // 12. CSV import / export and file handling
    // =====================================================================

    private void csvMenu() {
        while (true) {
            int choice = showSubMenu("Import / Export CSV",
                    "Export Customers", "Export Accounts", "Export Transactions", "Import Customers",
                    "Import Accounts", "Backup Database File (byte streams)", "Analyse a CSV File (StreamTokenizer)");
            switch (choice) {
                case 1: runSafely(() -> {
                    String path = InputHelper.readLineOrDefault("File to write", FileManager.CUSTOMERS_CSV);
                    int count = fileManager.exportCustomers(customerService.getAllCustomers(), path);
                    ConsoleUtil.printSuccess(count + " customer(s) exported to " + path);
                }); break;
                case 2: runSafely(() -> {
                    String path = InputHelper.readLineOrDefault("File to write", FileManager.ACCOUNTS_CSV);
                    int count = fileManager.exportAccounts(accountService.getAllAccounts(), path);
                    ConsoleUtil.printSuccess(count + " account(s) exported to " + path);
                }); break;
                case 3: runSafely(() -> {
                    String path = InputHelper.readLineOrDefault("File to write", FileManager.TRANSACTIONS_CSV);
                    int count = fileManager.exportTransactions(transactionService.getTransactions(), path);
                    ConsoleUtil.printSuccess(count + " transaction(s) exported to " + path);
                }); break;
                case 4: runSafely(this::importCustomers); break;
                case 5: runSafely(this::importAccounts); break;
                case 6: runSafely(this::backupDatabase); break;
                case 7: runSafely(this::analyseCsvFile); break;
                default: return;
            }
        }
    }

    private void importCustomers() throws IOException, SQLException {
        String path = InputHelper.readLineOrDefault("File to import", FileManager.CUSTOMERS_CSV);
        ImportResult<Customer> result = fileManager.readCustomers(path);
        customerService.importCustomers(result);
        printImportResult("customer", result);
    }

    private void importAccounts() throws IOException, SQLException {
        String path = InputHelper.readLineOrDefault("File to import", FileManager.ACCOUNTS_CSV);
        ImportResult<BankAccount> result = fileManager.readAccounts(path);
        accountService.importAccounts(result);
        printImportResult("account", result);
    }

    /** Wildcard parameter ImportResult<?>: accepts ImportResult<Customer> and ImportResult<BankAccount>. */
    private void printImportResult(String recordName, ImportResult<?> result) {
        ConsoleUtil.printSubHeader("Import Result");
        System.out.println("Valid rows read from file : " + result.getValidRecords().size());
        System.out.println("Saved to database         : " + result.getImportedCount());
        System.out.println("Problems found            : " + result.getErrors().size());
        for (String error : result.getErrors()) {
            System.out.println("   - " + error);
        }
        if (result.getImportedCount() > 0) {
            ConsoleUtil.printSuccess(result.getImportedCount() + " " + recordName + "(s) imported.");
        } else {
            ConsoleUtil.printInfo("No new " + recordName + "s were imported.");
        }
    }

    private void backupDatabase() throws IOException {
        String target = "data/backup/bank_management_" + LocalDateTime.now().format(FILE_TIMESTAMP) + ".db";
        long bytes = fileManager.copyFile(DatabaseManager.getDatabasePath(), target);
        ConsoleUtil.printSuccess("Database backed up to " + target + " (" + String.format("%,d", bytes) + " bytes copied).");
    }

    private void analyseCsvFile() throws IOException {
        String path = InputHelper.readLineOrDefault("File to analyse", FileManager.ACCOUNTS_CSV);
        Map<String, Integer> statistics = fileManager.analyseFile(path);
        ConsoleUtil.printSubHeader("StreamTokenizer Analysis of " + path);
        for (Map.Entry<String, Integer> entry : statistics.entrySet()) {
            System.out.printf("%-15s : %d%n", entry.getKey(), entry.getValue());
        }
    }

    // =====================================================================
    // 13. Collections demonstration
    // =====================================================================

    private void collectionsMenu() {
        while (true) {
            int choice = showSubMenu("Collections Demonstration",
                    "ArrayList and LinkedList", "HashSet, LinkedHashSet and TreeSet",
                    "HashMap, LinkedHashMap and TreeMap", "Queue, Deque and PriorityQueue",
                    "Iterator and ListIterator", "Comparable, Comparator and Collections methods",
                    "Strings, Arrays and Wrapper Classes", "Lambda, Streams and Generics");
            if (choice == 9) {
                return;
            }
            runSafely(() -> {
                CollectionsDemo demo = new CollectionsDemo(customerService.getAllCustomers(),
                        accountService.getAllAccounts(), transactionService.getTransactions());
                demo.printDataSourceNote();
                JavaConceptsDemo conceptsDemo = new JavaConceptsDemo(demo.getCustomers(), demo.getAccounts());
                switch (choice) {
                    case 1: demo.demonstrateLists(); break;
                    case 2: demo.demonstrateSets(); break;
                    case 3: demo.demonstrateMaps(); break;
                    case 4: demo.demonstrateQueues(); break;
                    case 5: demo.demonstrateIterators(); break;
                    case 6: demo.demonstrateSortingUtilities(); break;
                    case 7: conceptsDemo.demonstrateStringsArraysWrappers(); break;
                    default: conceptsDemo.demonstrateLambdaStreamsGenerics(); break;
                }
            });
        }
    }

    // =====================================================================
    // 14. Multithreading demonstration
    // =====================================================================

    private void multithreadingMenu() {
        while (true) {
            int choice = showSubMenu("Multithreading Demonstration",
                    "Thread Lifecycle, sleep, join, priority, interrupt", "Race Condition vs synchronized / Atomic",
                    "Visibility with volatile", "Daemon Thread: Background Report Generation",
                    "Producer-Consumer: Concurrent Banking Requests");
            switch (choice) {
                case 1: runSafely(multithreadingDemo::runLifecycleDemo); break;
                case 2: runSafely(multithreadingDemo::runRaceConditionDemo); break;
                case 3: runSafely(multithreadingDemo::runVisibilityDemo); break;
                case 4: runSafely(multithreadingDemo::runBackgroundReportDemo); break;
                case 5: runSafely(this::runProducerConsumer); break;
                default: return;
            }
        }
    }

    private void runProducerConsumer() throws Exception {
        System.out.println("This demo makes REAL transactions: 5 deposits of Rs. 100 and 5 withdrawals of Rs. 50");
        System.out.println("on the account you choose (net change: + Rs. 250 if all succeed).");
        String accountNumber = InputHelper.readLine("Account number to use: ");
        if (InputHelper.readYesNo("Continue?")) {
            multithreadingDemo.runProducerConsumerDemo(accountNumber);
        } else {
            ConsoleUtil.printInfo("Demo cancelled.");
        }
    }
}
