package multithreading;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One request waiting in the queue, for example "deposit Rs. 100 into SB1001".
 * Objects are IMMUTABLE (all fields final, no setters), so they are safe to
 * pass between threads without extra locking.
 */
public class BankingRequest {

    public enum RequestType { DEPOSIT, WITHDRAW, STOP }

    /** AtomicInteger gives each request a unique ID even when many threads create requests at once. */
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int requestId;
    private final RequestType type;
    private final String accountNumber;
    private final BigDecimal amount;
    private final String createdBy;

    public BankingRequest(RequestType type, String accountNumber, BigDecimal amount, String createdBy) {
        this.requestId = NEXT_ID.getAndIncrement();
        this.type = type;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.createdBy = createdBy;
    }

    /** A special "poison pill" request that tells a consumer to stop. */
    public static BankingRequest stopSignal() {
        return new BankingRequest(RequestType.STOP, "", BigDecimal.ZERO, "system");
    }

    public boolean isStopSignal() {
        return type == RequestType.STOP;
    }

    public int getRequestId() {
        return requestId;
    }

    public RequestType getType() {
        return type;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return "Request #" + requestId + " [" + type + " Rs. " + amount + " on " + accountNumber + " from " + createdBy + "]";
    }
}
