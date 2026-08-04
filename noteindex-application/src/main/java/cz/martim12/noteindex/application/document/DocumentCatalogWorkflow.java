package cz.martim12.noteindex.application.document;

import cz.martim12.noteindex.application.index.SearchIndexSynchronizer;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.persistence.api.DocumentRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates document browsing and deletion.
 *
 * The repository remains authoritative. After a delete attempt,
 * the corresponding derived search-index entry is removed as well.
 */
public final class DocumentCatalogWorkflow {

    private final DocumentRepository documentRepository;
    private final SearchIndexSynchronizer indexSynchronizer;

    public DocumentCatalogWorkflow(DocumentRepository documentRepository, SearchIndexSynchronizer indexSynchronizer) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "Document repository must not be null");
        this.indexSynchronizer = Objects.requireNonNull(indexSynchronizer, "Search index synchronizer must not be null");
    }

    /**
     * Returns lightweight information about all stored documents.
     */
    public List<DocumentSummary> listDocuments() {
        return List.copyOf(documentRepository.findAllSummaries());
    }

    /**
     * Loads one complete document when it exists.
     */
    public Optional<Document> findDocument(long documentId) {
        requirePositiveDocumentId(documentId);

        return documentRepository.findById(documentId);
    }

    /**
     * Deletes a document from the authoritative repository and
     * removes any matching entry from the derived search index.
     *
     * The index cleanup also happens when the repository reports
     * that the document is already missing. This repairs a possible
     * stale index entry.
     *
     * @return true when a persisted document was deleted
     */
    public boolean deleteDocument(long documentId) {
        requirePositiveDocumentId(documentId);

        boolean deleted = documentRepository.deleteById(documentId);

        indexSynchronizer.removeDocument(documentId);

        return deleted;
    }

    private static void requirePositiveDocumentId(long documentId) {
        if (documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be positive");
        }
    }
}
