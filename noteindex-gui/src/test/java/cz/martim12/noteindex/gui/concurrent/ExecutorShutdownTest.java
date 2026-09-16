package cz.martim12.noteindex.gui.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutorShutdownTest {

    @Test
    void interruptsWorkerAndWaitsForTermination() throws Exception {
        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        CountDownLatch started = new CountDownLatch(1);

        try {
            executor.submit(() -> {
                started.countDown();

                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });

            assertTrue(started.await(3, TimeUnit.SECONDS));

            ExecutorShutdown.shutdownNowAndAwait(executor);

            assertTrue(executor.isTerminated());
        } finally {
            executor.shutdownNow();
        }
    }
}
