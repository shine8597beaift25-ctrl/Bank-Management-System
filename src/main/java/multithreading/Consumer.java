package multithreading;

import exceptions.AccountNotFoundException;
import exceptions.InsufficientBalanceException;
import exceptions.InvalidAmountException;
import service.AccountService;

import java.sql.SQLException;

/**
 * CONSUMER: takes requests from the queue and performs them on the real database.
 *
 * Extends the Thread class and overrides run(). Calling start() creates a new
 * thread that runs run(); calling run() directly would NOT create a thread.
 *
 * Balance safety: AccountService.deposit/withdraw are synchronized, so even with
 * several consumers working at once only one money operation runs at a time.
 */
public class Consumer extends Thread {

    private final RequestQueue<BankingRequest> queue;
    private final AccountService accountService;
    private final ProcessingStats stats;
    private final long processingDelayMillis;

    public Consumer(String name, RequestQueue<BankingRequest> queue, AccountService accountService,
                    ProcessingStats stats, long processingDelayMillis) {
        super(name);   // Thread(String name) constructor
        this.queue = queue;
        this.accountService = accountService;
        this.stats = stats;
        this.processingDelayMillis = processingDelayMillis;
    }

    @Override
    public void run() {
        try {
            while (true) {
                BankingRequest request = queue.take();   // waits if the queue is empty
                if (request.isStopSignal()) {
                    System.out.println("   [" + getName() + "] received stop signal. Ending.");
                    break;
                }
                process(request);
                Thread.sleep(processingDelayMillis);   // simulates the time a clerk needs per request
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("   [" + getName() + "] was interrupted.");
        }
    }

    private void process(BankingRequest request) {
        String description = "Concurrent demo request #" + request.getRequestId() + " (" + getName() + ")";
        try {
            if (request.getType() == BankingRequest.RequestType.DEPOSIT) {
                accountService.deposit(request.getAccountNumber(), request.getAmount(), description);
                stats.recordDeposit(request.getAmount());
            } else {
                accountService.withdraw(request.getAccountNumber(), request.getAmount(), description);
                stats.recordWithdrawal(request.getAmount());
            }
            System.out.println("   [" + getName() + "] processed " + request);
        } catch (InsufficientBalanceException e) {
            stats.recordFailure();
            System.out.println("   [" + getName() + "] refused " + request + ": " + e.getMessage());
        } catch (AccountNotFoundException | SQLException | InvalidAmountException | IllegalStateException e) {
            stats.recordFailure();
            System.out.println("   [" + getName() + "] could not process " + request + ": " + e.getMessage());
        }
    }
}
