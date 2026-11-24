package multithreading;

import java.util.List;

/**
 * PRODUCER: puts banking requests into the shared queue.
 *
 * Implements the Runnable interface (the other way to create a thread is
 * to extend Thread, as Consumer does). A Runnable is passed to a Thread:
 *     new Thread(new Producer(...), "ATM").start();
 */
public class Producer implements Runnable {

    private final RequestQueue<BankingRequest> queue;
    private final List<BankingRequest> requests;
    private final long delayMillis;

    public Producer(RequestQueue<BankingRequest> queue, List<BankingRequest> requests, long delayMillis) {
        this.queue = queue;
        this.requests = requests;
        this.delayMillis = delayMillis;
    }

    @Override
    public void run() {
        String name = Thread.currentThread().getName();
        try {
            for (BankingRequest request : requests) {
                queue.put(request);
                System.out.println("   [" + name + "] submitted " + request);
                Thread.sleep(delayMillis);   // TIMED_WAITING: simulates customers arriving over time
            }
            System.out.println("   [" + name + "] finished submitting " + requests.size() + " requests.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // keep the interrupt flag set for the caller
            System.out.println("   [" + name + "] was interrupted.");
        }
    }
}
