package multithreading;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A GENERIC, fixed-size, thread-safe queue built with wait() and notifyAll().
 * This is the classic PRODUCER-CONSUMER buffer.
 *
 *  - put():  if the queue is full, the producer thread WAITS until a consumer takes something.
 *  - take(): if the queue is empty, the consumer thread WAITS until a producer adds something.
 *
 * Both methods are synchronized, so they use this object's INTRINSIC LOCK (monitor).
 * wait() releases the lock while waiting; notifyAll() wakes the waiting threads.
 * We check the condition in a while loop (not an if) because a thread can wake up
 * and find the condition is still not true (another thread got there first).
 */
public class RequestQueue<T> {

    private final Queue<T> items = new LinkedList<>();
    private final int capacity;

    public RequestQueue(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (items.size() == capacity) {
            System.out.println("   [Queue] Full (" + capacity + " items). "
                    + Thread.currentThread().getName() + " waits...");
            wait();          // releases the lock and sleeps until notified
        }
        items.add(item);
        notifyAll();         // wake consumers that are waiting for an item
    }

    public synchronized T take() throws InterruptedException {
        while (items.isEmpty()) {
            wait();
        }
        T item = items.poll();
        notifyAll();         // wake producers that are waiting for free space
        return item;
    }

    public synchronized int size() {
        return items.size();
    }
}
