package cz.martim12.noteindex.persistence.api;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.ImportedDocument;

import java.util.List;
import java.util.Optional;

/**
 * Provides persistent storage operations for NoteIndex documents.
 *
 * <p>Implementations are responsible for storing and retrieving documents
 * from the underlying persistence layer.</p>
 */
public interface DocumentRepository {

    /**
     * Stores a new imported document.
     *
     * @param document document data to store
     * @return persisted document with assigned metadata
     */
    Document save(ImportedDocument document);

    /**
     * Finds a document by its identifier.
     *
     * @param id document identifier
     * @return document if found, otherwise empty
     */
    Optional<Document> findById(long id);

    /**
     * Returns all stored documents.
     *
     * @return list of documents
     */
    List<Document> findAll();

    /**
     * Returns lightweight information about all stored documents.
     *
     * @return document summaries
     */
    List<DocumentSummary> findAllSummaries();

    /**
     * Checks whether a document with the given source URI exists.
     *
     * @param sourceUri document source URI
     * @return true if a document with this source exists
     */
    boolean existsBySourceUri(String sourceUri);

    /**
     * Deletes a document by its identifier.
     *
     * @param id document identifier
     * @return true if a document was deleted
     */
    boolean deleteById(long id);

    /**
     * Updates the user-visible title of a document.
     *
     * @param id document identifier
     * @param displayTitle new display title
     * @return true if the document was updated
     */
    boolean updateDisplayTitle(long id, String displayTitle);
}
