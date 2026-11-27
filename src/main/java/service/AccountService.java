package service;

import database.DatabaseManager;
import exceptions.AccountNotFoundException;
import exceptions.CustomerNotFoundException;
import exceptions.DuplicateAccountException;
import exceptions.InsufficientBalanceException;
import exceptions.InvalidAmountException;
import filehandling.ImportResult;
import model.AccountStatus;
import model.AccountType;
import model.BankAccount;
import model.Customer;
import model.TransactionType;
import repository.AccountRepository;
import repository.CustomerRepository;
import util.ConsoleUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for bank accounts and every money movement.
 *
 * DATABASE TRANSACTIONS
 *   Each deposit, withdrawal and transfer runs on ONE connection with
 *   setAutoCommit(false). Either everything is saved with commit(), or
 *   nothing is saved with rollback(). For a transfer this means the
 *   sender's debit and the receiver's credit succeed together or fail together.
 *
 * THREAD AND CONNECTION SAFETY (two layers)
 *   1. deposit, withdraw and transfer are "synchronized" methods. Only one thread
 *      at a time can run any of them on this service object (they share the
 *      object's intrinsic lock). This stops two threads in this program from
 *      reading the same old balance and overwriting each other (a lost update).
 *   2. Inside the database transaction the account row is read with
 *      SELECT ... FOR UPDATE, which makes MySQL lock that row until commit or
 *      rollback. This protects the balance even against a second copy of the
 *      program (or another tool) connected to the same MySQL database.
 */
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionService transactionService;

    public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository,
                          TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionService = transactionService;
    }

    /** Makes new account numbers continue after the highest number already in the database. */
    public void loadAccountSequence() throws SQLException {
        BankAccount.updateSequence(accountRepository.findHighestSequence());
    }

    // =====================================================================
    // Account management
    // =====================================================================

    /**
     * Opens a new savings or current account with an opening deposit.
     * Rule: a customer may have only ONE active account of each type.
     */
    public BankAccount openAccount(String customerId, AccountType type, BigDecimal openingDeposit)
            throws CustomerNotFoundException, DuplicateAccountException, SQLException {
        String cleanedId = customerId.trim().toUpperCase();
        Customer customer = customerRepository.findById(cleanedId)
                .orElseThrow(() -> new CustomerNotFoundException(cleanedId));

        Optional<BankAccount> existing = accountRepository.findActiveByCustomerAndType(cleanedId, type);
        if (existing.isPresent()) {
            throw new DuplicateAccountException("Customer " + cleanedId + " already has an active "
                    + type.getLabel() + " account (" + existing.get().getAccountNumber()
                    + "). Only one active account of each type is allowed.");
        }

        // The parent reference holds a SavingsAccount or CurrentAccount object (runtime polymorphism).
        // The number is null for now and is assigned only after the checks pass.
        BankAccount account = BankAccount.createAccount(type, null,
                cleanedId, customer.getName(), BigDecimal.ZERO, LocalDate.now(), AccountStatus.ACTIVE);

        if (openingDeposit.compareTo(account.getMinimumOpeningBalance()) < 0) {
            throw new InvalidAmountException("Minimum opening deposit for a " + type.getLabel() + " account is "
                    + ConsoleUtil.formatMoney(account.getMinimumOpeningBalance()) + ".");
        }
        account.deposit(openingDeposit);
        account.assignAccountNumber(BankAccount.generateAccountNumber(type));

        String referenceId = transactionService.generateReferenceId("OPN");
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                accountRepository.save(connection, account);
                transactionService.recordSuccess(connection, account.getAccountNumber(), TransactionType.DEPOSIT,
                        openingDeposit, "Opening deposit", referenceId);
                connection.commit();      // account and its first deposit are saved together
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
        return account;
    }

    public List<BankAccount> getAllAccounts() throws SQLException {
        return accountRepository.findAll();
    }

    public List<BankAccount> getAccountsForCustomer(String customerId) throws SQLException {
        return accountRepository.findByCustomerId(customerId.trim().toUpperCase());
    }

    public BankAccount getAccount(String accountNumber) throws AccountNotFoundException, SQLException {
        String cleaned = accountNumber.trim().toUpperCase();
        return accountRepository.findById(cleaned).orElseThrow(() -> new AccountNotFoundException(cleaned));
    }

    /** Closes an account. The balance must be zero (rule checked inside BankAccount.close()). */
    public BankAccount closeAccount(String accountNumber) throws AccountNotFoundException, SQLException {
        BankAccount account = getAccount(accountNumber);
        account.close();   // throws IllegalStateException if already closed or balance > 0
        accountRepository.updateStatus(account.getAccountNumber(), account.getStatus());
        return account;
    }

    // =====================================================================
    // Money movements
    // =====================================================================

    /** Deposit with the standard description. */
    public BankAccount deposit(String accountNumber, BigDecimal amount) throws AccountNotFoundException, SQLException {
        return deposit(accountNumber, amount, "Cash deposit");
    }

    /** Overloaded deposit that lets the caller choose the description. */
    public synchronized BankAccount deposit(String accountNumber, BigDecimal amount, String description)
            throws AccountNotFoundException, SQLException {
        String cleaned = accountNumber.trim().toUpperCase();
        String referenceId = transactionService.generateReferenceId("DEP");

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BankAccount account = lockAccount(connection, cleaned);
                account.deposit(amount);   // checks: account active, amount > 0
                requireOneRowChanged(accountRepository.updateBalance(connection, cleaned, account.getBalance()), cleaned);
                transactionService.recordSuccess(connection, cleaned, TransactionType.DEPOSIT, amount,
                        description, referenceId);
                connection.commit();
                return account;
            } catch (SQLException | RuntimeException e) {   // multi-catch, then re-throw
                connection.rollback();
                throw e;
            }
        }
    }

    public BankAccount withdraw(String accountNumber, BigDecimal amount)
            throws AccountNotFoundException, InsufficientBalanceException, SQLException {
        return withdraw(accountNumber, amount, "Cash withdrawal");
    }

    /** Withdraws money. A refused withdrawal is stored as a FAILED record and the balance stays the same. */
    public synchronized BankAccount withdraw(String accountNumber, BigDecimal amount, String description)
            throws AccountNotFoundException, InsufficientBalanceException, SQLException {
        String cleaned = accountNumber.trim().toUpperCase();
        String referenceId = transactionService.generateReferenceId("WDL");

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BankAccount account = lockAccount(connection, cleaned);
                account.withdraw(amount);  // checks: active, amount > 0, limit, enough balance
                requireOneRowChanged(accountRepository.updateBalance(connection, cleaned, account.getBalance()), cleaned);
                transactionService.recordSuccess(connection, cleaned, TransactionType.WITHDRAWAL, amount,
                        description, referenceId);
                connection.commit();
                return account;
            } catch (InsufficientBalanceException e) {
                connection.rollback();
                transactionService.recordFailure(cleaned, TransactionType.WITHDRAWAL, amount,
                        "Refused: insufficient balance", referenceId);
                throw e;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Moves money from one account to another as ONE database transaction.
     *
     * Steps: validate -> debit sender -> record TRANSFER_OUT -> credit receiver -> record TRANSFER_IN -> commit.
     * If ANY step fails, rollback() undoes all earlier steps, so money is never lost or created.
     *
     * @return the reference ID shared by both transaction rows
     */
    public synchronized String transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount)
            throws AccountNotFoundException, InsufficientBalanceException, SQLException {
        String fromAccount = fromAccountNumber.trim().toUpperCase();
        String toAccount = toAccountNumber.trim().toUpperCase();
        if (fromAccount.equals(toAccount)) {   // equals() compares the text, == would compare object references
            throw new IllegalArgumentException("Sender and receiver accounts must be different.");
        }
        String referenceId = transactionService.generateReferenceId("TRF");

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);   // START of the database transaction
            try {
                // Lock both rows, always in alphabetical order. If two opposite transfers
                // (A->B and B->A) run at the same time, both try to lock the same row first,
                // so they can never wait for each other forever (no deadlock).
                boolean senderFirst = fromAccount.compareTo(toAccount) < 0;
                BankAccount firstLocked = lockAccount(connection, senderFirst ? fromAccount : toAccount);
                BankAccount secondLocked = lockAccount(connection, senderFirst ? toAccount : fromAccount);
                BankAccount sender = senderFirst ? firstLocked : secondLocked;
                BankAccount receiver = senderFirst ? secondLocked : firstLocked;
                if (!receiver.isActive()) {
                    throw new IllegalStateException("Receiver account " + toAccount + " is closed.");
                }

                sender.withdraw(amount);       // may throw InsufficientBalanceException
                receiver.deposit(amount);

                // Step 1: take money from the sender
                requireOneRowChanged(accountRepository.updateBalance(connection, fromAccount, sender.getBalance()), fromAccount);
                transactionService.recordSuccess(connection, fromAccount, TransactionType.TRANSFER_OUT, amount,
                        "Transfer to " + toAccount, referenceId);

                // Step 2: give money to the receiver
                requireOneRowChanged(accountRepository.updateBalance(connection, toAccount, receiver.getBalance()), toAccount);
                transactionService.recordSuccess(connection, toAccount, TransactionType.TRANSFER_IN, amount,
                        "Transfer from " + fromAccount, referenceId);

                connection.commit();           // END: both sides saved together
                return referenceId;
            } catch (InsufficientBalanceException e) {
                connection.rollback();
                transactionService.recordFailure(fromAccount, TransactionType.TRANSFER_OUT, amount,
                        "Refused transfer to " + toAccount + ": insufficient balance", referenceId);
                throw e;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();         // undo the debit if the credit (or anything else) failed
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    // =====================================================================
    // CSV import
    // =====================================================================

    /** Saves valid accounts read from accounts.csv. Each one gets an opening-balance transaction. */
    public void importAccounts(ImportResult<BankAccount> result) throws SQLException {
        for (BankAccount account : result.getValidRecords()) {
            try {
                importOneAccount(account);
                result.recordImported();
            } catch (CustomerNotFoundException | DuplicateAccountException e) {
                result.addError("Account " + account.getAccountNumber() + " skipped: " + e.getMessage());
            }
        }
        loadAccountSequence();   // new account numbers must continue after imported ones
    }

    private void importOneAccount(BankAccount account)
            throws CustomerNotFoundException, DuplicateAccountException, SQLException {
        Customer customer = customerRepository.findById(account.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(account.getCustomerId()));
        if (accountRepository.existsById(account.getAccountNumber())) {
            throw new DuplicateAccountException("Account number " + account.getAccountNumber() + " already exists.");
        }
        if (account.isActive()
                && accountRepository.findActiveByCustomerAndType(customer.getCustomerId(), account.getAccountType()).isPresent()) {
            throw new DuplicateAccountException("Customer " + customer.getCustomerId() + " already has an active "
                    + account.getAccountType().getLabel() + " account.");
        }
        account.setAccountHolderName(customer.getName());

        String referenceId = transactionService.generateReferenceId("IMP");
        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                accountRepository.save(connection, account);
                if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                    transactionService.recordSuccess(connection, account.getAccountNumber(), TransactionType.DEPOSIT,
                            account.getBalance(), "Opening balance (CSV import)", referenceId);
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // =====================================================================
    // Private helpers
    // =====================================================================

    /** Reads the account and locks its row (SELECT ... FOR UPDATE) for the current database transaction. */
    private BankAccount lockAccount(Connection connection, String accountNumber)
            throws AccountNotFoundException, SQLException {
        return accountRepository.findByIdForUpdate(connection, accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    /** An UPDATE that changes 0 rows means something went wrong: throw so the caller rolls back. */
    private void requireOneRowChanged(int rowsChanged, String accountNumber) throws SQLException {
        if (rowsChanged != 1) {
            throw new SQLException("Balance update failed for account " + accountNumber + ".");
        }
    }
}
