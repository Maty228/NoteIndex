package cz.martim12.noteindex.search.index;

/**
 * Complete read/write search index abstraction.
 */
public interface SearchIndex extends IndexReader, IndexWriter, AutoCloseable {

    /**
     * In-memory implementations own no external resources.
     * Persistent implementations may override this method.
     */
    @Override
    default void close() {
        // No resources to release by default.
    }
}
