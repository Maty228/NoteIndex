package cz.martim12.noteindex.search.index;

/**
 * Mutation operations supported by a search index.
 */
public interface IndexWriter {

    /**
     * Adds a document or replaces the document with the same ID.
     *
     * @param document document to index
     */
    void indexDocument(IndexDocument document);

    /**
     * Removes a document from the index.
     *
     * @param documentId indexed document ID
     * @return true when a document was removed
     */
    boolean removeDocument(long documentId);

    /**
     * Removes all indexed data.
     */
    void clear();
}
