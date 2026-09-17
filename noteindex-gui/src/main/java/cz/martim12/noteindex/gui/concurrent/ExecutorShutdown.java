package cz.martim12.noteindex.gui.concurrent;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Performs bounded shutdown of GUI-owned background executors.
 */
public final class ExecutorShutdown {

    private static final long TERMINATION_TIMEOUT_MILLIS = 250;

    /** Prevents instantiation. */
    private ExecutorShutdown() {}

    /**
     * Requests immediate shutdown and briefly waits for worker termination.
     *
     * @param executor executor to stop
     */
    public static void shutdownNowAndAwait(ExecutorService executor) {
        Objects.requireNonNull(executor, "Executor must not be null");

        executor.shutdownNow();

        try {
            executor.awaitTermination(
                    TERMINATION_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
