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

/**
 * Default implementation of the public NoteIndex application API.
 *
 * The service delegates individual use cases to application
 * workflows and owns the lifetime of the search runtime.
 */
public final class DefaultNoteIndexService implements NoteIndexService {

    private final DocumentImportWorkflow importWorkflow;
    private final DocumentSearchWorkflow searchWorkflow;
    private final DocumentCatalogWorkflow catalogWorkflow;
    private final SearchRuntime searchRuntime;
    private final AtomicBoolean closed = new AtomicBoolean(false);

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

    @Override
    public Document importFile(Path source) {
        ensureOpen();
        return importWorkflow.importFile(source);
    }

    @Override
    public List<SearchResult> search(SearchQuery query, int limit) {
        ensureOpen();
        return searchWorkflow.search(query, limit);
    }

    @Override
    public List<DocumentSummary> listDocuments() {
        ensureOpen();
        return catalogWorkflow.listDocuments();
    }

    @Override
    public Optional<Document> findDocument(long documentId) {
        ensureOpen();
        return catalogWorkflow.findDocument(documentId);
    }

    @Override
    public boolean deleteDocument(long documentId) {
        ensureOpen();
        return catalogWorkflow.deleteDocument(documentId);
    }

    @Override
    public Set<String> supportedImportExtensions() {
        ensureOpen();
        return importWorkflow.supportedExtensions();
    }

    @Override
    public boolean renameDocument(long documentId, String newTitle) {
        ensureOpen();
        return catalogWorkflow.renameDocument(documentId, newTitle);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            searchRuntime.close();
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("NoteIndex service is closed");
        }
    }
}
