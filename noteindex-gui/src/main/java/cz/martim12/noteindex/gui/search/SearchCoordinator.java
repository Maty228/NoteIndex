package cz.martim12.noteindex.gui.search;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class SearchCoordinator implements AutoCloseable {

    private static final int DEFAULT_RESULT_LIMIT = 50;
    private static final long DEFAULT_DEBOUNCE_MILLIS = 250;

    private final NoteIndexService service;
    private final ScheduledExecutorService executor;
    private final Consumer<Runnable> uiExecutor;
    private final long debounceMillis;
    private final int resultLimit;

    private final ObservableList<SearchResult> results = FXCollections.observableArrayList();

    private final ReadOnlyStringWrapper query = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper searching = new ReadOnlyBooleanWrapper();
    private final ReadOnlyObjectWrapper<Throwable> error = new ReadOnlyObjectWrapper<>();

    private final AtomicLong generation = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<ScheduledFuture<?>> pendingSearch = new AtomicReference<>();

    public SearchCoordinator(NoteIndexService service) {
        this(
                service,
                createDefaultExecutor(),
                Platform::runLater,
                DEFAULT_DEBOUNCE_MILLIS,
                DEFAULT_RESULT_LIMIT
        );
    }

    SearchCoordinator(
            NoteIndexService service,
            ScheduledExecutorService executor,
            Consumer<Runnable> uiExecutor,
            long debounceMillis,
            int resultLimit
    ) {
        this.service = Objects.requireNonNull(service, "Service must not be null");
        this.executor = Objects.requireNonNull(executor, "Executor must not be null");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "UI executor must not be null");

        if (debounceMillis < 0) {
            throw new IllegalArgumentException("Debounce delay must not be negative");
        }

        if (resultLimit <= 0) {
            throw new IllegalArgumentException("Result limit must be positive");
        }

        this.debounceMillis = debounceMillis;
        this.resultLimit = resultLimit;
    }

    public ObservableList<SearchResult> results() {
        return FXCollections.unmodifiableObservableList(results);
    }

    public ReadOnlyStringProperty queryProperty() {
        return query.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty searchingProperty() {
        return searching.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<Throwable> errorProperty() {
        return error.getReadOnlyProperty();
    }

    public CompletableFuture<Void> search(String queryText) {
        ensureOpen();
        Objects.requireNonNull(queryText, "Query text must not be null");

        long currentGeneration = generation.incrementAndGet();

        cancelPendingSearch();

        if (queryText.isBlank()) {
            CompletableFuture<Void> completion = new CompletableFuture<>();

            uiExecutor.accept(() -> {
                if (closed.get() || generation.get() != currentGeneration) {
                    completion.complete(null);
                    return;
                }

                query.set("");
                results.clear();
                error.set(null);
                searching.set(false);

                completion.complete(null);
            });

            return completion;
        }

        CompletableFuture<Void> completion = new CompletableFuture<>();

        uiExecutor.accept(() -> {
            if (closed.get() || generation.get() != currentGeneration) {
                completion.complete(null);
                return;
            }

            query.set(queryText);
            error.set(null);
            searching.set(true);
        });

        ScheduledFuture<?> scheduled = executor.schedule(
                () -> executeSearch(queryText, currentGeneration, completion),
                debounceMillis,
                TimeUnit.MILLISECONDS
        );

        pendingSearch.set(scheduled);

        return completion;
    }

    public void clear() {
        search("");
    }

    private void executeSearch(String queryText, long currentGeneration, CompletableFuture<Void> completion) {

        try {
            List<SearchResult> searchResults = service.search(
                    new SearchQuery(queryText),
                    resultLimit
            );

            System.out.println(
                    "SEARCH QUERY: " + queryText +
                            " RESULTS: " + searchResults.size()
            );

            uiExecutor.accept(() -> {
                if (closed.get() || generation.get() != currentGeneration) {
                    completion.complete(null);
                    return;
                }

                results.setAll(searchResults);
                error.set(null);
                searching.set(false);

                completion.complete(null);
            });

        } catch (RuntimeException exception) {
            uiExecutor.accept(() -> {
                if (closed.get() || generation.get() != currentGeneration) {
                    completion.complete(null);
                    return;
                }

                results.clear();
                error.set(exception);
                searching.set(false);

                completion.completeExceptionally(exception);
            });
        }
    }

    private void cancelPendingSearch() {
        ScheduledFuture<?> scheduled = pendingSearch.getAndSet(null);

        if (scheduled != null && !scheduled.isDone()) {
            scheduled.cancel(false);
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Search coordinator is closed");
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        generation.incrementAndGet();
        cancelPendingSearch();

        executor.shutdownNow();
    }

    private static ScheduledExecutorService createDefaultExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "noteindex-gui-search");
            thread.setDaemon(true);
            return thread;
        });
    }


}
