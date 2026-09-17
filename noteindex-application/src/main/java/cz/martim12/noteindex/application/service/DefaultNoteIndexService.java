package cz.martim12.noteindex.application.service;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.application.document.DocumentCatalogWorkflow;
import cz.martim12.noteindex.application.importing.DocumentImportWorkflow;
import cz.martim12.noteindex.application.search.DocumentSearchWorkflow;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import cz.martim12.noteindex.search.engine.SearchRuntime;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Default implementation of the public NoteIndex application API.
 * The service delegates individual use cases to application
 * workflows and owns the lifetime of the search runtime.
 */
public final class DefaultNoteIndexService implements NoteIndexService {

    private final DocumentImportWorkflow importWorkflow;
    private final DocumentSearchWorkflow searchWorkflow;
    private final DocumentCatalogWorkflow catalogWorkflow;
    private final SearchRuntime searchRuntime;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ReadWriteLock lifecycleLock = new ReentrantReadWriteLock(true);
    private final Lock readLock = lifecycleLock.readLock();
    private final Lock writeLock = lifecycleLock.writeLock();

    /**
     * Creates the default application service implementation.
     *
     * @param importWorkflow workflow responsible for importing documents
     * @param searchWorkflow workflow responsible for searching documents
     * @param catalogWorkflow workflow responsible for document management
     * @param searchRuntime runtime resources owned by the service
     */
    public DefaultNoteIndexService(
            DocumentImportWorkflow importWorkflow,
            DocumentSearchWorkflow searchWorkflow,
            DocumentCatalogWorkflow catalogWorkflow,
            SearchRuntime searchRuntime
    ) {
        this.importWorkflow = Objects.requireNonNull(importWorkflow, "Import workflow must not be null");
        this.searchWorkflow = Objects.requireNonNull(searchWorkflow, "Search workflow must not be null");
        this.catalogWorkflow = Objects.requireNonNull(catalogWorkflow, "Catalog workflow must not be null");
        this.searchRuntime = Objects.requireNonNull(searchRuntime, "Search runtime must not be null");
    }

    /** {@inheritDoc} */
    @Override
    public Document importFile(Path source) {
        return withWriteLock(
                () -> importWorkflow.importFile(source)
        );
    }

    /** {@inheritDoc} */
    @Override
    public List<SearchResult> search(SearchQuery query, int limit) {
        return withReadLock(
                () -> searchWorkflow.search(query, limit)
        );
    }

    /** {@inheritDoc} */
    @Override
    public List<DocumentSummary> listDocuments() {
        return withReadLock(
                catalogWorkflow::listDocuments
        );
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Document> findDocument(long documentId) {
        return withReadLock(
                () -> catalogWorkflow.findDocument(documentId)
        );
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteDocument(long documentId) {
        return withWriteLock(
                () -> catalogWorkflow.deleteDocument(documentId)
        );
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> supportedImportExtensions() {
        return withReadLock(
                importWorkflow::supportedExtensions
        );
    }

    /** {@inheritDoc} */
    @Override
    public boolean renameDocument(long documentId, String newTitle) {
        return withWriteLock(
                () -> catalogWorkflow.renameDocument(documentId, newTitle)
        );
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        writeLock.lock();

        try {
            if (closed.compareAndSet(false, true)) {
                searchRuntime.close();
            }
        } finally {
            writeLock.unlock();
        }
    }

    /** Executes a complete read-only service operation while holding the read lock. */
    private <T> T withReadLock(Supplier<T> operation) {
        readLock.lock();

        try {
            ensureOpen();
            return operation.get();
        } finally {
            readLock.unlock();
        }
    }

    /** Executes a complete mutating service operation while holding the write lock. */
    private <T> T withWriteLock(Supplier<T> operation) {
        writeLock.lock();

        try {
            ensureOpen();
            return operation.get();
        } finally {
            writeLock.unlock();
        }
    }

    /** Rejects operations after the service has been closed. */
    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("NoteIndex service is closed");
        }
    }
}
