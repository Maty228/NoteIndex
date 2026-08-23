package cz.martim12.noteindex.gui.main;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Presentation state for the main NoteIndex window.
 */
public final class MainViewModel implements AutoCloseable {

    /**
     * Available document library filters.
     */
    public enum LibraryView {
        /**
         * Displays all available documents.
         */
        ALL,
        /**
         * Displays only recently imported documents.
         */
        RECENT,
        /**
         * Displays only plain text documents.
         */
        TXT,
        /**
         * Displays only Markdown documents.
         */
        MARKDOWN
    }

    /**
     * Available document sorting modes.
     */
    public enum DocumentSort {
        /**
         * Sorts documents by import date with newest documents first.
         */
        NEWEST,
        /**
         * Sorts documents by import date with oldest documents first.
         */
        OLDEST,
        /**
         * Sorts documents alphabetically by title in ascending order.
         */
        TITLE_ASCENDING,
        /**
         * Sorts documents alphabetically by title in descending order.
         */
        TITLE_DESCENDING
    }

    private static final int RECENT_DOCUMENT_LIMIT = 20;

    private final NoteIndexService service;
    private final ExecutorService executor;
    private final Consumer<Runnable> uiExecutor;

    private final List<DocumentSummary> allDocuments = new ArrayList<>();
    private final ObservableList<DocumentSummary> visibleDocuments = FXCollections.observableArrayList();

    private final ReadOnlyIntegerWrapper totalDocumentCount = new ReadOnlyIntegerWrapper();
    private final ReadOnlyObjectWrapper<Document> selectedDocument = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper libraryLoading = new ReadOnlyBooleanWrapper();
    private final ReadOnlyObjectWrapper<Throwable> error = new ReadOnlyObjectWrapper<>();

    private final AtomicLong selectionGeneration = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();

    private LibraryView libraryView = LibraryView.ALL;
    private DocumentSort documentSort = DocumentSort.NEWEST;

    /**
     * Creates the main window view model.
     *
     * @param service application service used for document operations
     */
    public MainViewModel(NoteIndexService service) {
        this(service, createDefaultExecutor(), Platform::runLater);
    }

    MainViewModel(NoteIndexService service, ExecutorService executor, Consumer<Runnable> uiExecutor) {
        this.service = Objects.requireNonNull(service, "Service must not be null");
        this.executor = Objects.requireNonNull(executor, "Executor must not be null");
        this.uiExecutor = Objects.requireNonNull(uiExecutor, "UI executor must not be null");
    }

    /**
     * Returns the currently visible documents.
     *
     * @return read-only observable document list
     */
    public ObservableList<DocumentSummary> visibleDocuments() {
        return FXCollections.unmodifiableObservableList(visibleDocuments);
    }

    /**
     * Returns the property containing the total number of documents.
     *
     * @return document count property
     */
    public ReadOnlyIntegerProperty totalDocumentCountProperty() {
        return totalDocumentCount.getReadOnlyProperty();
    }

    /**
     * Returns the property containing the currently selected document.
     *
     * @return selected document property
     */
    public ReadOnlyObjectProperty<Document> selectedDocumentProperty() {
        return selectedDocument.getReadOnlyProperty();
    }

    /**
     * Returns the property indicating whether the document library is loading.
     *
     * @return library loading state property
     */
    public ReadOnlyBooleanProperty libraryLoadingProperty() {
        return libraryLoading.getReadOnlyProperty();
    }

    /**
     * Returns the property containing the latest operation error.
     *
     * @return error property
     */
    public ReadOnlyObjectProperty<Throwable> errorProperty() {
        return error.getReadOnlyProperty();
    }

    /**
     * Reloads document summaries asynchronously.
     *
     * @return completion future for the refresh operation
     */
    public CompletableFuture<Void> refresh() {
        ensureOpen();

        libraryLoading.set(true);
        error.set(null);

        CompletableFuture<Void> result = new CompletableFuture<>();

        CompletableFuture
                .supplyAsync(service::listDocuments, executor)
                .whenComplete((documents, failure) ->
                        uiExecutor.accept(() -> {
                            if (closed.get()) {
                                result.complete(null);
                                return;
                            }
                            libraryLoading.set(false);

                            if (failure != null) {
                                Throwable actualFailure = unwrap(failure);
                                error.set(actualFailure);
                                result.completeExceptionally(actualFailure);
                                return;
                            }

                            allDocuments.clear();
                            allDocuments.addAll(documents);

                            totalDocumentCount.set(allDocuments.size());

                            recomputeVisibleDocuments();

                            result.complete(null);
                        }));
        return result;
    }

    /**
     * Loads and selects a document asynchronously.
     *
     * @param summary document summary to select
     * @return completion future for the selection operation
     */
    public CompletableFuture<Void> selectDocument(DocumentSummary summary) {
        ensureOpen();

        long generation = selectionGeneration.incrementAndGet();

        if (summary == null) {
            selectedDocument.set(null);
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> result = new CompletableFuture<>();

        CompletableFuture
                .supplyAsync(() -> service.findDocument(summary.id()), executor)
                .whenComplete((document, failure) ->
                        uiExecutor.accept(() -> {

                            if (closed.get() || generation != selectionGeneration.get()) {
                                result.complete(null);
                                return;
                            }

                            if (failure != null) {
                                Throwable actualFailure = unwrap(failure);
                                error.set(actualFailure);
                                result.completeExceptionally(actualFailure);
                                return;
                            }

                            if (document.isEmpty()) {
                                IllegalStateException exception = new IllegalStateException(
                                        "Document " + summary.id() + " no longer exists"
                                );

                                error.set(exception);
                                selectedDocument.set(null);
                                result.completeExceptionally(exception);
                                return;
                            }

                            selectedDocument.set(document.orElseThrow());
                            result.complete(null);

                        }));
        return result;
    }

    /**
     * Deletes a document asynchronously.
     *
     * @param documentId identifier of the document to delete
     * @return future containing whether deletion succeeded
     */
    public CompletableFuture<Boolean> deleteDocument(long documentId) {
        ensureOpen();

        if (documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be positive");
        }

        /*
         * Any document load already running must no longer be allowed
         * to select the document after it has been deleted.
         */
        selectionGeneration.incrementAndGet();

        CompletableFuture<Boolean> result = new CompletableFuture<>();

        CompletableFuture
                .supplyAsync(() -> service.deleteDocument(documentId), executor)
                .whenComplete((deleted, failure) ->
                        uiExecutor.accept(() -> {
                            if (closed.get()) {
                                result.complete(false);
                                return;
                            }

                            if (failure != null) {
                                Throwable actualFailure = unwrap(failure);

                                error.set(actualFailure);
                                result.completeExceptionally(actualFailure);
                                return;
                            }

                            Document selected = selectedDocument.get();

                            if (deleted && selected != null && selected.id() == documentId) {
                                selectedDocument.set(null);
                            }

                            result.complete(deleted);
                        }));

        return result;
    }

    /**
     * Renames a document asynchronously.
     *
     * @param documentId document identifier
     * @param newTitle new document title
     * @return future containing whether rename succeeded
     */
    public CompletableFuture<Boolean> renameDocument(
            long documentId,
            String newTitle
    ) {
        ensureOpen();

        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }

        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException(
                    "Document title must not be blank"
            );
        }

        selectionGeneration.incrementAndGet();

        String normalizedTitle = newTitle.trim();

        CompletableFuture<Boolean> result = new CompletableFuture<>();

        CompletableFuture
                .supplyAsync(
                        () -> service.renameDocument(
                                documentId,
                                normalizedTitle
                        ),
                        executor
                )
                .whenComplete((renamed, failure) ->
                        uiExecutor.accept(() -> {
                            if (closed.get()) {
                                result.complete(false);
                                return;
                            }

                            if (failure != null) {
                                Throwable actualFailure = unwrap(failure);

                                error.set(actualFailure);
                                result.completeExceptionally(actualFailure);
                                return;
                            }

                            result.complete(renamed);
                        }));

        return result;
    }

    /**
     * Changes the active library filter.
     *
     * @param view selected library view
     */
    public void setLibraryView(LibraryView view) {
        ensureOpen();

        this.libraryView = Objects.requireNonNull(view, "Library view must not be null");
        recomputeVisibleDocuments();
    }

    /**
     * Changes document sorting.
     *
     * @param documentSort selected sorting mode
     */
    public void setDocumentSort(DocumentSort documentSort) {
        ensureOpen();

        this.documentSort = Objects.requireNonNull(documentSort, "Document sort must not be null");
        recomputeVisibleDocuments();
    }

    /**
     * Returns the active library filter.
     *
     * @return current library view
     */
    public LibraryView libraryView() {
        return libraryView;
    }

    /**
     * Returns the active document sorting mode.
     *
     * @return current sorting mode
     */
    public DocumentSort documentSort() {
        return documentSort;
    }


    private void recomputeVisibleDocuments() {
        List<DocumentSummary> documents = switch (libraryView) {
            case ALL -> new ArrayList<>(allDocuments);

            case TXT -> allDocuments.stream()
                    .filter(document -> document.format().equalsIgnoreCase("text/plain"))
                    .toList();

            case MARKDOWN -> allDocuments.stream()
                    .filter(document -> document.format().equalsIgnoreCase("text/markdown"))
                    .toList();

            case RECENT -> allDocuments.stream()
                    .sorted(Comparator.comparing(DocumentSummary::importedAt).reversed())
                    .limit(RECENT_DOCUMENT_LIMIT)
                    .toList();

        };

        Comparator<DocumentSummary> comparator = switch (documentSort) {
            case NEWEST -> Comparator.comparing(DocumentSummary::importedAt).reversed();
            case OLDEST -> Comparator.comparing(DocumentSummary::importedAt);
            case TITLE_ASCENDING -> Comparator.comparing(DocumentSummary::title, String.CASE_INSENSITIVE_ORDER);
            case TITLE_DESCENDING -> Comparator.comparing(DocumentSummary::title, String.CASE_INSENSITIVE_ORDER).reversed();
        };

        visibleDocuments.setAll(documents.stream()
                .sorted(comparator)
                .toList());
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;

        while(current.getCause() != null && current.getCause() != current) {
            if (!(current instanceof CompletionException)) {
                break;
            }
            current = current.getCause();
        }

        return current;
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Main view model is closed");
        }
    }

    /**
     * Releases background resources used by this view model.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        selectionGeneration.incrementAndGet();
        executor.shutdownNow();
    }

    private static ExecutorService createDefaultExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "noteindex-gui-main");
            thread.setDaemon(true);
            return thread;
        });
    }
}
