package multithreading;

import filehandling.FileManager;
import service.ReportService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * BACKGROUND REPORT GENERATOR - a DAEMON thread.
 *
 * Every few hundred milliseconds it rebuilds the full banking report and saves
 * it to a text file, until it is stopped.
 *
 * Concepts shown:
 *  - extends Thread and overrides run().
 *  - setDaemon(true): the JVM does not wait for daemon threads when the program ends.
 *  - setPriority(Thread.MIN_PRIORITY): a hint that this work is less important.
 *  - volatile boolean running: VISIBILITY - a change made by the main thread is
 *    seen immediately by this thread.
 *  - interrupt(): wakes the thread from sleep() with InterruptedException.
 *  - wait()/notify(): the main thread waits until the first report is ready.
 *  - StringBuffer: like StringBuilder but synchronized (thread-safe), because the
 *    main thread reads the log while this thread writes to it.
 */
public final class ReportThread extends Thread {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final ReportService reportService;
    private final FileManager fileManager;
    private final String outputPath;
    private final long intervalMillis;

    private volatile boolean running = true;
    private volatile int reportsGenerated;
    private final StringBuffer activityLog = new StringBuffer();

    private final Object firstReportLock = new Object();
    private boolean firstReportReady;   // guarded by firstReportLock

    public ReportThread(ReportService reportService, FileManager fileManager, String outputPath, long intervalMillis) {
        super("ReportThread");
        this.reportService = reportService;
        this.fileManager = fileManager;
        this.outputPath = outputPath;
        this.intervalMillis = intervalMillis;
        setDaemon(true);
        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        log("started (daemon=" + isDaemon() + ", priority=" + getPriority() + ")");
        try {
            while (running) {
                fileManager.writeTextFile(outputPath, reportService.buildFullReportText());
                reportsGenerated++;   // only this thread writes the field; volatile makes the new value visible
                log("report #" + reportsGenerated + " saved to " + outputPath);
                signalFirstReport();
                Thread.sleep(intervalMillis);
            }
            log("stopped because running = false");
        } catch (InterruptedException e) {
            log("interrupted while sleeping - stopping");
        } catch (SQLException | IOException e) {
            log("error: " + e.getMessage());
        } finally {
            signalFirstReport();   // never leave the main thread waiting forever
        }
    }

    /** Called by the main thread: waits (up to timeoutMillis) until the first report exists. */
    public boolean waitForFirstReport(long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        synchronized (firstReportLock) {
            while (!firstReportReady) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    return false;
                }
                firstReportLock.wait(remaining);   // releases the lock while waiting
            }
            return true;
        }
    }

    /** Stops the loop: set the volatile flag AND interrupt the sleep. */
    public void stopGracefully() {
        running = false;
        interrupt();
    }

    public int getReportsGenerated() {
        return reportsGenerated;
    }

    public String getActivityLog() {
        return activityLog.toString();
    }

    private void signalFirstReport() {
        synchronized (firstReportLock) {
            firstReportReady = true;
            firstReportLock.notify();   // only the main thread waits here, so notify() is enough
        }
    }

    private void log(String message) {
        activityLog.append(LocalTime.now().format(TIME)).append("  ").append(message).append('\n');
    }
}
