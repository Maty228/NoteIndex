package cz.martim12.noteindex.gui.search;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
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

/**
 * Coordinates asynchronous document searching for the GUI.
 *
 * <p>Search requests are debounced, executed outside the JavaFX thread,
 * and only the latest request is allowed to update observable state.</p>
 */
public final class SearchCoordinator implements AutoCloseable {

    private static final int DEFAULT_RESULT_LIMIT = 50;
    private static final long DEFAULT_DEBOUNCE_MILLIS = 250;

    private final NoteIndexService service;
    private final ScheduledExecutorService executor;
    private final Consumer<Runnable> uiExecutor;
    private final long debounceMillis;
    private volatile int resultLimit;

    private final ObservableList<SearchResult> results = FXCollections.observableArrayList();

    private final ReadOnlyStringWrapper query = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper searching = new ReadOnlyBooleanWrapper();
    private final ReadOnlyObjectWrapper<Throwable> error = new ReadOnlyObjectWrapper<>();

    private final AtomicLong generation = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<ScheduledFuture<?>> pendingSearch = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<Void>> pendingCompletion = new AtomicReference<>();

    private final ReadOnlyBooleanWrapper unfinishedQuotedPhrase = new ReadOnlyBooleanWrapper(false);

    /**
     * Creates a search coordinator using default background execution settings.
     *
     * @param service application service used to execute searches
     */
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

    /**
     * Returns the observable search results.
     *
     * @return read-only observable result list
     */
    public ObservableList<SearchResult> results() {
        return FXCollections.unmodifiableObservableList(results);
    }

    /**
     * Returns the property indicating whether a search is running.
     *
     * @return searching state property
     */
    public ReadOnlyBooleanProperty searchingProperty() {
        return searching.getReadOnlyProperty();
    }

    /**
     * Returns the property containing the latest search failure.
     *
     * @return error property
     */
    public ReadOnlyObjectProperty<Throwable> errorProperty() {
        return error.getReadOnlyProperty();
    }

    /**
     * Starts a debounced asynchronous search.
     *
     * <p>Blank queries clear results. Requests superseded by newer searches
     * do not update the UI state.</p>
     *
     * @param queryText search text
     * @return completion future for the search operation
     */
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
                unfinishedQuotedPhrase.set(false);

                completion.complete(null);
            });

            return completion;
        }

        boolean unfinishedQuote =  hasUnclosedQuote(queryText);

        if (unfinishedQuote) {
            CompletableFuture<Void> completion =  new CompletableFuture<>();

            uiExecutor.accept(() -> {
                if (closed.get()  || generation.get() != currentGeneration) {
                    completion.complete(null);
                    return;
                }

                results.clear();
                error.set(null);
                searching.set(false);
                unfinishedQuotedPhrase.set(true);

                completion.complete(null);
            });

            return completion;
        }

        CompletableFuture<Void> completion = new CompletableFuture<>();
        pendingCompletion.set(completion);

        uiExecutor.accept(() -> {
            if (closed.get() || generation.get() != currentGeneration) {
                completion.complete(null);
                return;
            }

            query.set(queryText);
            error.set(null);
            searching.set(true);
            unfinishedQuotedPhrase.set(false);
        });


        ScheduledFuture<?> scheduled = executor.schedule(
                () -> executeSearch(queryText, currentGeneration, completion),
                debounceMillis,
                TimeUnit.MILLISECONDS
        );

        pendingSearch.set(scheduled);

        return completion;
    }

    /**
     * Clears the current search query and results.
     */
    public void clear() {
        search("");
    }

    private void executeSearch(String queryText, long currentGeneration, CompletableFuture<Void> completion) {
        try {
            int currentResultLimit = resultLimit;

            List<SearchResult> searchResults = service.search(
                    new SearchQuery(queryText),
                    currentResultLimit
            );

            uiExecutor.accept(() -> {
                if (closed.get() || generation.get() != currentGeneration) {
                    pendingCompletion.compareAndSet(completion, null);
                    completion.complete(null);
                    return;
                }

                results.setAll(searchResults);
                error.set(null);
                searching.set(false);

                pendingCompletion.compareAndSet(completion, null);
                completion.complete(null);
            });

        } catch (RuntimeException exception) {
            uiExecutor.accept(() -> {
                if (closed.get() || generation.get() != currentGeneration) {
                    pendingCompletion.compareAndSet(completion, null);
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
        ScheduledFuture<?> pending = pendingSearch.getAndSet(null);

        if (pending != null) {
            pending.cancel(false);
        }

        CompletableFuture<Void> completion = pendingCompletion.getAndSet(null);

        if (completion != null && !completion.isDone()) {
            completion.cancel(false);
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Search coordinator is closed");
        }
    }

    /**
     * Stops background searching and releases executor resources.
     */
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

    /**
     * Returns the maximum number of search results.
     *
     * @return result limit
     */
    public int resultLimit() {
        return resultLimit;
    }

    /**
     * Updates the maximum number of search results.
     *
     * @param resultLimit maximum number of results
     * @throws IllegalArgumentException if the limit is not positive
     */
    public void setResultLimit(int resultLimit) {
        if (resultLimit <= 0) {
            throw new IllegalArgumentException("Result limit must be positive");
        }

        this.resultLimit = resultLimit;
    }

    private static boolean hasUnclosedQuote(String query) {
        boolean quoted = false;

        for (int index = 0; index < query.length(); index++) {
            if (query.charAt(index) == '"') {
                quoted = !quoted;
            }
        }

        return quoted;
    }

    /**
     * Returns the property indicating whether the current query contains
     * an unfinished quoted phrase.
     *
     * @return unfinished quote state property
     */
    public ReadOnlyBooleanProperty unfinishedQuotedPhraseProperty() {
        return unfinishedQuotedPhrase.getReadOnlyProperty();
    }


}
