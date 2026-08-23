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
 * The repository remains authoritative. After a delete attempt,
 * the corresponding derived search-index entry is removed as well.
 */
public final class DocumentCatalogWorkflow {

    private final DocumentRepository documentRepository;
    private final SearchIndexSynchronizer indexSynchronizer;

    /**
     * Creates a document catalog workflow.
     *
     * @param documentRepository repository containing stored documents
     * @param indexSynchronizer synchronizer for derived search index data
     */
    public DocumentCatalogWorkflow(DocumentRepository documentRepository, SearchIndexSynchronizer indexSynchronizer) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "Document repository must not be null");
        this.indexSynchronizer = Objects.requireNonNull(indexSynchronizer, "Search index synchronizer must not be null");
    }

    /**
     * Returns lightweight information about all stored documents.
     *
     * @return lightweight document summaries
     */
    public List<DocumentSummary> listDocuments() {
        return List.copyOf(documentRepository.findAllSummaries());
    }

    /**
     * Loads one complete document when it exists.
     *
     * @param documentId stored document ID
     * @return document if it exists
     */
    public Optional<Document> findDocument(long documentId) {
        requirePositiveDocumentId(documentId);

        return documentRepository.findById(documentId);
    }

    /**
     * Deletes a document from the authoritative repository and
     * removes any matching entry from the derived search index.
     * The index cleanup also happens when the repository reports
     * that the document is already missing. This repairs a possible
     * stale index entry.
     *
     * @param documentId stored document ID
     * @return true when a persisted document was deleted
     */
    public boolean deleteDocument(long documentId) {
        requirePositiveDocumentId(documentId);

        boolean deleted = documentRepository.deleteById(documentId);

        indexSynchronizer.removeDocument(documentId);

        return deleted;
    }

    /**
     * Changes the user-visible document title and synchronizes the
     * derived search index.
     * The original source file is not modified.
     *
     * @param documentId stored document ID
     * @param newTitle new user-visible title
     * @return true when the persisted document existed
     */
    public boolean renameDocument(long documentId, String newTitle) {
        requirePositiveDocumentId(documentId);
        requireNonBlankTitle(newTitle);

        boolean renamed = documentRepository.updateDisplayTitle(
                documentId,
                newTitle.trim()
        );

        if (!renamed) {
            indexSynchronizer.removeDocument(documentId);
            return false;
        }

        Optional<Document> updatedDocument =
                documentRepository.findById(documentId);

        if (updatedDocument.isEmpty()) {
            indexSynchronizer.removeDocument(documentId);
            return false;
        }

        indexSynchronizer.indexDocument(updatedDocument.orElseThrow());

        return true;
    }

    private static void requirePositiveDocumentId(long documentId) {
        if (documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be positive");
        }
    }

    private static void requireNonBlankTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Document title must not be blank"
            );
        }
    }
}
