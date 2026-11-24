package multithreading;

import java.math.BigDecimal;

/**
 * Totals shared by all consumer threads.
 *
 * Updating several fields together is a CRITICAL SECTION: if two threads did it
 * at the same time, one update could be lost. Each update is therefore wrapped in
 * a synchronized BLOCK that locks on a private object.
 */
public class ProcessingStats {

    private final Object lock = new Object();
    private BigDecimal totalDeposited = BigDecimal.ZERO;
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;
    private int successCount;
    private int failureCount;

    public void recordDeposit(BigDecimal amount) {
        synchronized (lock) {
            totalDeposited = totalDeposited.add(amount);
            successCount++;
        }
    }

    public void recordWithdrawal(BigDecimal amount) {
        synchronized (lock) {
            totalWithdrawn = totalWithdrawn.add(amount);
            successCount++;
        }
    }

    public void recordFailure() {
        synchronized (lock) {
            failureCount++;
        }
    }

    public BigDecimal getTotalDeposited() {
        synchronized (lock) {
            return totalDeposited;
        }
    }

    public BigDecimal getTotalWithdrawn() {
        synchronized (lock) {
            return totalWithdrawn;
        }
    }

    public int getSuccessCount() {
        synchronized (lock) {
            return successCount;
        }
    }

    public int getFailureCount() {
        synchronized (lock) {
            return failureCount;
        }
    }
}
