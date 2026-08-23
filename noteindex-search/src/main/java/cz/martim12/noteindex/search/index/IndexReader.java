package cz.martim12.noteindex.search.index;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to an information retrieval index.
 */
public interface IndexReader {

    /**
     * Returns postings for a normalized term in the selected field.
     * The returned list must be ordered by document ID.
     *
     * @param normalizedTerm normalized term to retrieve
     * @param field field to inspect
     * @return postings ordered by document ID
     */
    List<Posting> postings(String normalizedTerm, FieldName field);

    /**
     * Returns normalized indexed terms in the selected field that
     * begin with the supplied normalized prefix.
     * The returned terms must be ordered lexicographically.
     *
     * @param normalizedPrefix normalized term prefix
     * @param field field to inspect
     * @return matching terms ordered lexicographically
     */
    List<String> termsWithPrefix(String normalizedPrefix, FieldName field);

    /**
     * Returns statistics for an indexed document.
     *
     * @param documentId indexed document ID
     * @return document statistics if the document is indexed
     */
    Optional<DocumentStatistics> documentStatistics(long documentId);

    /**
     * Returns collection statistics for the selected field.
     *
     * @param field field to inspect
     * @return collection statistics for the field
     */
    FieldStatistics fieldStatistics(FieldName field);

    /**
     * Returns the number of indexed documents.
     *
     * @return indexed document count
     */
    long documentCount();

    /**
     * Checks whether a document is present in the index.
     *
     * @param documentId document identifier
     * @return true if the document exists
     */
    default boolean containsDocument(long documentId) {
        return documentStatistics(documentId).isPresent();
    }
}
