package multithreading;

import exceptions.AccountNotFoundException;
import filehandling.FileManager;
import model.BankAccount;
import service.AccountService;
import service.ReportService;
import util.ConsoleUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs the multithreading demonstrations shown in menu option 14.
 * Each method is a separate, self-contained demonstration.
 */
public class MultithreadingDemo {

    private static final String BACKGROUND_REPORT_PATH = "data/reports/background_report.txt";

    private final AccountService accountService;
    private final ReportService reportService;
    private final FileManager fileManager;

    public MultithreadingDemo(AccountService accountService, ReportService reportService, FileManager fileManager) {
        this.accountService = accountService;
        this.reportService = reportService;
        this.fileManager = fileManager;
    }

    // =====================================================================
    // 1. Thread lifecycle, Runnable vs Thread, sleep, join, priority, interrupt
    // =====================================================================

    /** A thread made by EXTENDING Thread and overriding run(). */
    private static class CountdownThread extends Thread {
        CountdownThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            for (int count = 3; count >= 1; count--) {
                System.out.println("   [" + getName() + "] countdown " + count);
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    public void runLifecycleDemo() throws InterruptedException {
        ConsoleUtil.printSubHeader("Thread Lifecycle, sleep(), join(), Priority, interrupt()");

        // Way 1: implement Runnable (here with a lambda, because Runnable is a functional interface)
        Runnable printTask = () -> {
            for (int step = 1; step <= 3; step++) {
                System.out.println("   [" + Thread.currentThread().getName() + "] step " + step);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    return;
                }
            }
        };
        Thread runnableThread = new Thread(printTask, "Runnable-Worker");
        // Way 2: extend Thread
        CountdownThread countdownThread = new CountdownThread("Thread-Subclass");

        System.out.println("State before start() : " + runnableThread.getState());          // NEW
        runnableThread.setPriority(Thread.MAX_PRIORITY);
        countdownThread.setPriority(Thread.MIN_PRIORITY);
        System.out.println("Priorities           : " + runnableThread.getName() + "=" + runnableThread.getPriority()
                + ", " + countdownThread.getName() + "=" + countdownThread.getPriority()
                + " (only a hint to the scheduler)");

        runnableThread.start();   // start() creates the new thread, which then calls run()
        countdownThread.start();
        System.out.println("State after start()  : " + runnableThread.getState());          // RUNNABLE (or TIMED_WAITING)
        Thread.sleep(100);
        System.out.println("State while sleeping : " + runnableThread.getState());          // TIMED_WAITING

        runnableThread.join();    // main thread waits here until runnableThread finishes
        countdownThread.join();
        System.out.println("State after join()   : " + runnableThread.getState());          // TERMINATED

        demonstrateBlockedAndWaitingStates();
        demonstrateInterrupt();
    }

    private void demonstrateBlockedAndWaitingStates() throws InterruptedException {
        Object lock = new Object();

        // BLOCKED: a thread wants a lock that another thread (main) is holding
        Thread blockedThread = new Thread(() -> {
            synchronized (lock) {
                System.out.println("   [Blocked-Demo] finally got the lock");
            }
        }, "Blocked-Demo");
        synchronized (lock) {
            blockedThread.start();
            Thread.sleep(100);
            System.out.println("State waiting for a lock : " + blockedThread.getState());   // BLOCKED
        }
        blockedThread.join();

        // WAITING: a thread called wait() and is waiting for notify()
        Thread waitingThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait();
                    System.out.println("   [Waiting-Demo] woken up by notify()");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Waiting-Demo");
        waitingThread.start();
        Thread.sleep(100);
        System.out.println("State after wait()       : " + waitingThread.getState());       // WAITING
        synchronized (lock) {
            lock.notify();
        }
        waitingThread.join();
    }

    private void demonstrateInterrupt() throws InterruptedException {
        Thread sleeper = new Thread(() -> {
            try {
                System.out.println("   [Sleeper] going to sleep for 10 seconds...");
                Thread.sleep(10_000);
                System.out.println("   [Sleeper] woke up normally");
            } catch (InterruptedException e) {
                System.out.println("   [Sleeper] interrupted! InterruptedException caught, stopping early.");
            }
        }, "Sleeper");
        sleeper.start();
        Thread.sleep(300);
        sleeper.interrupt();   // ask the thread to stop
        sleeper.join();
        ConsoleUtil.printSuccess("Lifecycle demo finished: NEW -> RUNNABLE -> TIMED_WAITING / BLOCKED / WAITING -> TERMINATED.");
    }

    // =====================================================================
    // 2. Race condition and the three ways to fix it
    // =====================================================================

    /** NOT thread-safe: count++ is really three steps (read, add, write). */
    private static class UnsafeCounter {
        private int count;

        void increment() {
            count++;
        }
    }

    /** Fixed with a synchronized METHOD (locks on "this"). */
    private static class SynchronizedMethodCounter {
        private int count;

        synchronized void increment() {
            count++;
        }
    }

    /** Fixed with a synchronized BLOCK on a private lock object (the critical section is only count++). */
    private static class SynchronizedBlockCounter {
        private final Object lock = new Object();
        private int count;

        void increment() {
            synchronized (lock) {
                count++;
            }
        }
    }

    public void runRaceConditionDemo() throws InterruptedException {
        ConsoleUtil.printSubHeader("Race Condition vs synchronized and AtomicInteger");
        final int threadCount = 4;
        final int incrementsPerThread = 100_000;
        final int expected = threadCount * incrementsPerThread;
        System.out.println(threadCount + " threads each add Rs. 1 to a shared total "
                + incrementsPerThread + " times. Expected total = " + expected);

        UnsafeCounter unsafe = new UnsafeCounter();
        SynchronizedMethodCounter synchronizedMethod = new SynchronizedMethodCounter();
        SynchronizedBlockCounter synchronizedBlock = new SynchronizedBlockCounter();
        AtomicInteger atomic = new AtomicInteger();

        runInParallel(threadCount, incrementsPerThread, unsafe::increment);
        runInParallel(threadCount, incrementsPerThread, synchronizedMethod::increment);
        runInParallel(threadCount, incrementsPerThread, synchronizedBlock::increment);
        runInParallel(threadCount, incrementsPerThread, atomic::incrementAndGet);

        String[][] rows = {
                {"No synchronization (race condition)", String.valueOf(unsafe.count), resultText(unsafe.count, expected)},
                {"synchronized method", String.valueOf(synchronizedMethod.count), resultText(synchronizedMethod.count, expected)},
                {"synchronized block", String.valueOf(synchronizedBlock.count), resultText(synchronizedBlock.count, expected)},
                {"AtomicInteger (atomic operation)", String.valueOf(atomic.get()), resultText(atomic.get(), expected)}
        };
        ConsoleUtil.printTable(new String[] {"Approach", "Final Total", "Result"}, rows);
        System.out.println("The unsafe total is usually LOWER than expected because two threads read the same");
        System.out.println("old value and both write back old+1, so one update is lost. The bank's real");
        System.out.println("deposit/withdraw/transfer methods are synchronized to prevent exactly this.");
    }

    /** Starts several threads that each call the same action many times, then waits for all of them. */
    private void runInParallel(int threadCount, int repetitions, Runnable action) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                for (int r = 0; r < repetitions; r++) {
                    action.run();
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private String resultText(int actual, int expected) {
        return actual == expected ? "Correct" : "WRONG (lost " + (expected - actual) + " updates)";
    }

    // =====================================================================
    // 3. Visibility with volatile
    // =====================================================================

    private volatile boolean keepWorking;

    public void runVisibilityDemo() throws InterruptedException {
        ConsoleUtil.printSubHeader("Visibility with volatile");
        keepWorking = true;
        AtomicInteger loops = new AtomicInteger();

        Thread worker = new Thread(() -> {
            while (keepWorking) {          // reads the volatile field every time
                loops.incrementAndGet();
            }
            System.out.println("   [Worker] saw keepWorking = false and stopped.");
        }, "Visibility-Worker");

        worker.start();
        Thread.sleep(200);
        System.out.println("Main thread sets keepWorking = false");
        keepWorking = false;               // volatile write: immediately visible to the worker
        worker.join(2000);

        if (worker.isAlive()) {
            ConsoleUtil.printError("Worker did not stop - the change was not visible.");
        } else {
            ConsoleUtil.printSuccess("Worker stopped after " + String.format("%,d", loops.get())
                    + " loops. Without 'volatile' the worker might keep a cached copy and never stop.");
        }
    }

    // =====================================================================
    // 4. Daemon background report thread (wait/notify, interrupt)
    // =====================================================================

    public void runBackgroundReportDemo() throws InterruptedException {
        ConsoleUtil.printSubHeader("Daemon Thread: Background Report Generation");
        ReportThread reportThread = new ReportThread(reportService, fileManager, BACKGROUND_REPORT_PATH, 400);
        reportThread.start();

        System.out.println("Main thread waits (wait/notify) until the first report is ready...");
        boolean ready = reportThread.waitForFirstReport(5000);
        System.out.println(ready ? "First report is ready. Main thread continues its own work."
                : "Report thread did not respond in time.");

        Thread.sleep(1000);        // the daemon keeps working in the background meanwhile
        reportThread.stopGracefully();
        reportThread.join(2000);

        System.out.println("\nReportThread activity log:");
        System.out.print(reportThread.getActivityLog());
        ConsoleUtil.printSuccess(reportThread.getReportsGenerated() + " report(s) generated. Latest saved in "
                + BACKGROUND_REPORT_PATH);
    }

    // =====================================================================
    // 5. Producer-consumer with real banking requests
    // =====================================================================

    /**
     * Two producers (Branch-Counter, ATM) submit deposits and withdrawals for one real account
     * into a small shared queue. Two consumers process them against the database at the same time.
     * Afterwards the final balance is checked against the expected value.
     */
    public void runProducerConsumerDemo(String accountNumber) throws InterruptedException, SQLException,
            AccountNotFoundException {
        ConsoleUtil.printSubHeader("Producer-Consumer: Concurrent Banking Requests");
        BankAccount account = accountService.getAccount(accountNumber);
        if (!account.isActive()) {
            throw new IllegalStateException("Account " + account.getAccountNumber() + " is closed.");
        }
        BigDecimal startBalance = account.getBalance();
        System.out.println("Account " + account.getAccountNumber() + " starting balance: "
                + ConsoleUtil.formatMoney(startBalance));

        // Small capacity + slow consumers (150 ms each) + fast producers = the queue fills up,
        // so you can see producers WAIT until a consumer frees a place.
        RequestQueue<BankingRequest> queue = new RequestQueue<>(3);
        ProcessingStats stats = new ProcessingStats();

        List<BankingRequest> counterRequests = new ArrayList<>();
        List<BankingRequest> atmRequests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            counterRequests.add(new BankingRequest(BankingRequest.RequestType.DEPOSIT, account.getAccountNumber(),
                    new BigDecimal("100.00"), "Branch-Counter"));
            atmRequests.add(new BankingRequest(BankingRequest.RequestType.WITHDRAW, account.getAccountNumber(),
                    new BigDecimal("50.00"), "ATM"));
        }

        Thread branchCounter = new Thread(new Producer(queue, counterRequests, 20), "Branch-Counter");
        Thread atm = new Thread(new Producer(queue, atmRequests, 30), "ATM");
        Consumer consumerOne = new Consumer("Consumer-1", queue, accountService, stats, 150);
        Consumer consumerTwo = new Consumer("Consumer-2", queue, accountService, stats, 150);

        consumerOne.start();
        consumerTwo.start();
        branchCounter.start();
        atm.start();

        branchCounter.join();     // wait until all requests are submitted
        atm.join();
        queue.put(BankingRequest.stopSignal());   // one stop signal per consumer
        queue.put(BankingRequest.stopSignal());
        consumerOne.join();
        consumerTwo.join();

        BigDecimal finalBalance = accountService.getAccount(account.getAccountNumber()).getBalance();
        BigDecimal expectedBalance = startBalance.add(stats.getTotalDeposited()).subtract(stats.getTotalWithdrawn());

        System.out.println();
        System.out.println("Requests processed : " + stats.getSuccessCount() + " successful, "
                + stats.getFailureCount() + " refused");
        System.out.println("Total deposited    : " + ConsoleUtil.formatMoney(stats.getTotalDeposited()));
        System.out.println("Total withdrawn    : " + ConsoleUtil.formatMoney(stats.getTotalWithdrawn()));
        System.out.println("Expected balance   : " + ConsoleUtil.formatMoney(expectedBalance));
        System.out.println("Balance in database: " + ConsoleUtil.formatMoney(finalBalance));
        if (finalBalance.compareTo(expectedBalance) == 0) {
            ConsoleUtil.printSuccess("Balances match. No update was lost even with two consumers working at once.");
        } else {
            ConsoleUtil.printError("Balance mismatch detected!");
        }
    }
}
