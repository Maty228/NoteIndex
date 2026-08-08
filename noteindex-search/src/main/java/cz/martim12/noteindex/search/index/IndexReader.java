package cz.martim12.noteindex.search.index;

import java.util.List;
import java.util.Optional;

/**
 * Read-only access to an information retrieval index.
 */
public interface IndexReader {

    /**
     * Returns postings for a normalized term in the selected field.
     *
     * The returned list must be ordered by document ID.
     */
    List<Posting> postings(String normalizedTerm, FieldName field);

    /**
     * Returns normalized indexed terms in the selected field that
     * begin with the supplied normalized prefix.
     *
     * The returned terms must be ordered lexicographically.
     */
    List<String> termsWithPrefix(String normalizedPrefix, FieldName field);

    /**
     * Returns statistics for an indexed document.
     */
    Optional<DocumentStatistics> documentStatistics(long documentId);

    /**
     * Returns collection statistics for the selected field.
     */
    FieldStatistics fieldStatistics(FieldName field);

    /**
     * Returns the number of indexed documents.
     */
    long documentCount();

    default boolean containsDocument(long documentId) {
        return documentStatistics(documentId).isPresent();
    }
}
