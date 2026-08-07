package cz.martim12.noteindex.gui.application;

import cz.martim12.noteindex.application.api.NoteIndexService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the application service and the background executor used
 * during GUI startup.
 *
 * Opening SQLite and rebuilding the search index must not block
 * the JavaFX Application Thread.
 */
public final class GuiApplicationContext implements AutoCloseable {

    private final GuiServiceFactory serviceFactory;
    private final ExecutorService executor;

    private final AtomicReference<GuiLifecycleState> state = new AtomicReference<>(GuiLifecycleState.NEW);

    private final AtomicReference<NoteIndexService> service = new AtomicReference<>();

    private final AtomicReference<Path> databaseFile = new AtomicReference<>();

    private final AtomicReference<CompletableFuture<NoteIndexService>> startupFuture = new AtomicReference<>();

    private final AtomicBoolean closed = new AtomicBoolean();

    public GuiApplicationContext(GuiServiceFactory serviceFactory) {
        this(serviceFactory, createDefaultExecutor());
    }

    /*
     * Package-private constructor allows lifecycle tests to own
     * their executor explicitly.
     */
    GuiApplicationContext(GuiServiceFactory serviceFactory, ExecutorService executor) {
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "Service factory must not be null");
        this.executor = Objects.requireNonNull(executor, "Executor must not be null");
    }

    /**
     * Starts the application runtime once.
     */
    public CompletableFuture<NoteIndexService> start(Path databaseFile) {
        Objects.requireNonNull(databaseFile, "Database file must not be null");

        if (!state.compareAndSet(GuiLifecycleState.NEW, GuiLifecycleState.STARTING)) {
            throw new IllegalStateException("GUI application runtime has already been started");
        }

        Path normalizedDatabaseFile = databaseFile.toAbsolutePath().normalize();

        this.databaseFile.set(normalizedDatabaseFile);

        CompletableFuture<NoteIndexService> future = CompletableFuture.supplyAsync(
                () -> openService(normalizedDatabaseFile), executor
        );

        startupFuture.set(future);

        return future;
    }

    public GuiLifecycleState state() {
        return state.get();
    }

    public Optional<NoteIndexService> service() {
        return Optional.ofNullable(service.get());
    }

    public Optional<Path> databaseFile() {
        return Optional.ofNullable(databaseFile.get());
    }

    private NoteIndexService openService(Path databaseFile) {
        try {
            if (closed.get()) {
                throw new CancellationException("GUI application runtime is closed");
            }

            createDatabaseParentDirectory(databaseFile);

            NoteIndexService openedService = Objects.requireNonNull(serviceFactory.open(databaseFile), "Service factory returned null");

            service.set(openedService);

            /*
             * close() may have been called while SQLite was being
             * opened. Do not allow the newly created service to
             * survive that race.
             */

            if (closed.get()) {
                if (service.compareAndSet(openedService, null)) {
                    openedService.close();
                }

                throw new CancellationException("GUI application runtime was closed during startup");

            }
            state.compareAndSet(GuiLifecycleState.STARTING, GuiLifecycleState.READY);

            return openedService;
        } catch (CancellationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            if (!closed.get()) {
                state.set(GuiLifecycleState.FAILED);
            }

            throw exception;
        } catch (IOException exception) {
            if (!closed.get()) {
                state.set(GuiLifecycleState.FAILED);
            }

            throw new IllegalStateException("Could not prepare database directory: " + databaseFile, exception);
        }
    }

    private static void createDatabaseParentDirectory(Path databaseFile) throws IOException {
        Path parent = databaseFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        state.set(GuiLifecycleState.CLOSED);

        CompletableFuture<NoteIndexService> future = startupFuture.get();

        if (future != null && !future.isDone()) {
            future.cancel(true);
        }

        NoteIndexService openedService = service.getAndSet(null);


        try {
            if (openedService != null) {
                openedService.close();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static ExecutorService createDefaultExecutor() {
        return Executors.newSingleThreadExecutor(
                runnable -> {
                    Thread thread = new Thread(runnable, "noteindex-gui-lifecycle");

                    thread.setDaemon(true);

                    return thread;
                }
        );
    }
}
