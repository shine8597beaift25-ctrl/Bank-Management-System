package service;

import model.AccountType;
import model.BankAccount;
import model.Customer;
import model.Transaction;
import model.TransactionType;
import repository.AccountRepository;
import repository.CustomerRepository;
import repository.TransactionRepository;
import util.ConsoleUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builds every banking report using LAMBDA EXPRESSIONS and the STREAM API.
 *
 * A stream pipeline has three parts:
 *   source        -> accounts.stream()
 *   intermediate  -> filter(...), map(...), sorted(...), distinct(), skip(...), limit(...)
 *   terminal      -> count(), collect(...), reduce(...), max(...)
 * Intermediate steps are LAZY: nothing runs until the terminal step is called.
 */
public class ReportService {

    /** Predicates (functional interface) built with lambdas and reused in many reports. */
    private static final Predicate<BankAccount> IS_ACTIVE = BankAccount::isActive;
    private static final Predicate<BankAccount> IS_SAVINGS = account -> account.getAccountType() == AccountType.SAVINGS;
    private static final Predicate<BankAccount> IS_CURRENT = account -> account.getAccountType() == AccountType.CURRENT;

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public ReportService(CustomerRepository customerRepository, AccountRepository accountRepository,
                         TransactionRepository transactionRepository) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    // =====================================================================
    // Generic helper methods
    // =====================================================================

    /**
     * BOUNDED generic method: T can be BankAccount or any subclass of it
     * (SavingsAccount, CurrentAccount). Works with a List<SavingsAccount> too.
     */
    public static <T extends BankAccount> BigDecimal sumBalances(Collection<T> accounts) {
        return accounts.stream()
                .map(BankAccount::getBalance)                 // method reference
                .reduce(BigDecimal.ZERO, BigDecimal::add);    // add all BigDecimals exactly
    }

    /** Adds the amounts of the transactions that match the condition. */
    private static BigDecimal sumAmounts(List<Transaction> transactions, Predicate<Transaction> condition) {
        return transactions.stream()
                .filter(condition)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Predicate<Transaction> successful(TransactionType type) {
        return transaction -> transaction.isSuccessful() && transaction.getType() == type;
    }

    // =====================================================================
    // Reports
    // =====================================================================

    /** All headline figures in display order (LinkedHashMap keeps insertion order). */
    public Map<String, String> getBankSummary() throws SQLException {
        List<Customer> customers = customerRepository.findAll();
        List<BankAccount> accounts = accountRepository.findAll();
        List<Transaction> transactions = transactionRepository.findAll();

        List<BankAccount> activeAccounts = accounts.stream().filter(IS_ACTIVE).collect(Collectors.toList());

        long depositCount = transactions.stream().filter(successful(TransactionType.DEPOSIT)).count();
        long withdrawalCount = transactions.stream().filter(successful(TransactionType.WITHDRAWAL)).count();
        long transferCount = transactions.stream().filter(successful(TransactionType.TRANSFER_OUT)).count();
        long failedCount = transactions.stream().filter(transaction -> !transaction.isSuccessful()).count();

        // distinct(): how many different accounts appear in the transaction history
        long accountsWithTransactions = transactions.stream()
                .map(Transaction::getAccountNumber)
                .distinct()
                .count();

        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("Total registered customers", String.valueOf(customers.size()));
        summary.put("Total accounts (active + closed)", String.valueOf(accounts.size()));
        summary.put("Total active accounts", String.valueOf(activeAccounts.size()));
        summary.put("Total savings accounts (active)", String.valueOf(activeAccounts.stream().filter(IS_SAVINGS).count()));
        summary.put("Total current accounts (active)", String.valueOf(activeAccounts.stream().filter(IS_CURRENT).count()));
        summary.put("Total money held in active accounts", ConsoleUtil.formatMoney(sumBalances(activeAccounts)));
        summary.put("Total deposits",
                ConsoleUtil.formatMoney(sumAmounts(transactions, successful(TransactionType.DEPOSIT))) + " (" + depositCount + " deposits)");
        summary.put("Total withdrawals",
                ConsoleUtil.formatMoney(sumAmounts(transactions, successful(TransactionType.WITHDRAWAL))) + " (" + withdrawalCount + " withdrawals)");
        summary.put("Total transfers",
                ConsoleUtil.formatMoney(sumAmounts(transactions, successful(TransactionType.TRANSFER_OUT))) + " (" + transferCount + " transfers)");
        summary.put("Refused (failed) attempts", String.valueOf(failedCount));
        summary.put("Highest account balance", describeAccount(getHighestBalanceAccount(activeAccounts)));
        summary.put("Lowest account balance", describeAccount(getLowestBalanceAccount(activeAccounts)));
        summary.put("Accounts with at least one transaction", String.valueOf(accountsWithTransactions));
        return summary;
    }

    public Optional<BankAccount> getHighestBalanceAccount(List<BankAccount> accounts) {
        return accounts.stream().max(Comparator.comparing(BankAccount::getBalance));
    }

    public Optional<BankAccount> getLowestBalanceAccount(List<BankAccount> accounts) {
        return accounts.stream().min(Comparator.comparing(BankAccount::getBalance));
    }

    /** Top N active accounts by balance: sorted (highest first) then limit(N). */
    public List<BankAccount> getTopAccountsByBalance(int count) throws SQLException {
        return accountRepository.findAll().stream()
                .filter(IS_ACTIVE)
                .sorted(Comparator.comparing(BankAccount::getBalance).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    /** Customers sorted A-Z by name (case does not matter). */
    public List<Customer> getCustomersAlphabetical() throws SQLException {
        return customerRepository.findAll().stream()
                .sorted(Comparator.comparing(Customer::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    /**
     * Groups active accounts by type. TreeMap keeps the types in sorted order.
     * Returns a 2-D array (rows x columns) ready for ConsoleUtil.printTable.
     */
    public String[][] getAccountSummaryByType() throws SQLException {
        Map<AccountType, List<BankAccount>> byType = accountRepository.findAll().stream()
                .filter(IS_ACTIVE)
                .collect(Collectors.groupingBy(BankAccount::getAccountType, TreeMap::new, Collectors.toList()));

        String[][] rows = new String[byType.size()][];
        int rowIndex = 0;
        for (Map.Entry<AccountType, List<BankAccount>> entry : byType.entrySet()) {
            List<BankAccount> accounts = entry.getValue();
            BigDecimal total = sumBalances(accounts);
            BigDecimal average = total.divide(BigDecimal.valueOf(accounts.size()), 2, RoundingMode.HALF_UP);
            rows[rowIndex++] = new String[] {entry.getKey().getLabel(), String.valueOf(accounts.size()),
                    ConsoleUtil.formatMoney(total), ConsoleUtil.formatMoney(average)};
        }
        return rows;
    }

    /** Count and total amount for each transaction type (successful ones only). */
    public String[][] getTransactionSummaryByType() throws SQLException {
        Map<TransactionType, List<Transaction>> byType = transactionRepository.findAll().stream()
                .filter(Transaction::isSuccessful)
                .collect(Collectors.groupingBy(Transaction::getType, TreeMap::new, Collectors.toList()));

        String[][] rows = new String[byType.size()][];
        int rowIndex = 0;
        for (Map.Entry<TransactionType, List<Transaction>> entry : byType.entrySet()) {
            rows[rowIndex++] = new String[] {entry.getKey().getLabel(), String.valueOf(entry.getValue().size()),
                    ConsoleUtil.formatMoney(sumAmounts(entry.getValue(), transaction -> true))};
        }
        return rows;
    }

    /** One page of accounts using skip() and limit(). Pages start at 1. */
    public List<BankAccount> getAccountsPage(int pageNumber, int pageSize) throws SQLException {
        return accountRepository.findAll().stream()
                .sorted()                                   // natural order (Comparable): by account number
                .skip((long) (pageNumber - 1) * pageSize)   // jump over earlier pages
                .limit(pageSize)                            // take one page
                .collect(Collectors.toList());
    }

    public int getTotalPages(int pageSize) throws SQLException {
        long total = accountRepository.count();
        return (int) Math.max(1, (total + pageSize - 1) / pageSize);
    }

    /**
     * Full plain-text report. Used by "Save report to file" and by the
     * background ReportThread. StringBuilder joins all the lines efficiently.
     */
    public String buildFullReportText() throws SQLException {
        StringBuilder report = new StringBuilder();
        report.append(ConsoleUtil.line('=', 60)).append('\n');
        report.append(ConsoleUtil.center(BankAccount.BANK_NAME + " - BANKING REPORT", 60)).append('\n');
        report.append(ConsoleUtil.center("Generated: " + LocalDateTime.now().format(Transaction.DATE_TIME_FORMAT), 60)).append('\n');
        report.append(ConsoleUtil.line('=', 60)).append('\n');

        for (Map.Entry<String, String> entry : getBankSummary().entrySet()) {
            report.append(String.format("%-40s : %s%n", entry.getKey(), entry.getValue()));
        }

        report.append('\n').append("TOP 5 ACCOUNT HOLDERS BY BALANCE").append('\n');
        int rank = 1;
        for (BankAccount account : getTopAccountsByBalance(5)) {
            report.append(String.format("%d. %-8s %-25s %s%n", rank++, account.getAccountNumber(),
                    account.getAccountHolderName(), ConsoleUtil.formatMoney(account.getBalance())));
        }

        report.append('\n').append("CUSTOMERS (ALPHABETICAL)").append('\n');
        for (Customer customer : getCustomersAlphabetical()) {
            report.append(String.format("- %-25s %s%n", customer.getName(), customer.getCustomerId()));
        }
        return report.toString();
    }

    private String describeAccount(Optional<BankAccount> account) {
        // map() runs only if a value is present; orElse gives the text for "no accounts"
        return account.map(found -> ConsoleUtil.formatMoney(found.getBalance()) + " (" + found.getAccountNumber()
                + " - " + found.getAccountHolderName() + ")").orElse("No active accounts");
    }
}
