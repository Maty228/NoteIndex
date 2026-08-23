package cz.martim12.noteindex.application.index;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.index.SearchIndex;

import java.util.List;
import java.util.Objects;

/**
 * Keeps the derived search index synchronized with documents
 * stored in the authoritative repository.
 */
public final class SearchIndexSynchronizer {

    private final DocumentRepository documentRepository;
    private final SearchIndex searchIndex;
    private final DocumentIndexMapper documentIndexMapper;

    /**
     * Creates a search index synchronizer.
     *
     * @param documentRepository authoritative document repository
     * @param searchIndex derived search index
     * @param documentIndexMapper mapper from documents to indexed data
     */
    public SearchIndexSynchronizer(
            DocumentRepository documentRepository,
            SearchIndex searchIndex,
            DocumentIndexMapper documentIndexMapper
    ) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "Document repository must not be null");
        this.searchIndex = Objects.requireNonNull(searchIndex, "Search index must not be null");
        this.documentIndexMapper = Objects.requireNonNull(documentIndexMapper, "Document index mapper must not be null");
    }

    /**
     * Adds or replaces a persisted document in the search index.
     *
     * @param document document to synchronize
     */
    public void indexDocument(Document document) {
        searchIndex.indexDocument(documentIndexMapper.map(document));
    }

    /**
     * Removes one document from the index.
     *
     * @param documentId stored document ID
     * @return true when the document was indexed
     */
    public boolean removeDocument(long documentId) {
        return searchIndex.removeDocument(documentId);
    }

    /**
     * Rebuilds the complete in-memory index from SQLite.
     * Documents are loaded and mapped before the current index is
     * cleared. A repository or mapping failure therefore leaves the
     * existing index untouched.
     * If indexing fails midway through rebuilding, the partial index
     * is cleared rather than exposed as a complete index.
     *
     * @return number of indexed documents
     */
    public int rebuild() {
        List<IndexDocument> documents = documentRepository.findAll()
                .stream()
                .map(documentIndexMapper::map)
                .toList();

        searchIndex.clear();

        try {
            for (IndexDocument document : documents) {
                searchIndex.indexDocument(document);
            }

            return documents.size();
        } catch (RuntimeException exception) {
            clearAfterFailure(exception);
            throw exception;
        }
    }

    private void clearAfterFailure(RuntimeException failure) {
        try {
            searchIndex.clear();
        } catch (RuntimeException clearFailure) {
            failure.addSuppressed(clearFailure);
        }
    }


}
