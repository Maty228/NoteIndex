package cz.martim12.noteindex.search.engine;

import java.util.List;

/**
 * Executes a complete lexical search over an existing index.
 */
@FunctionalInterface
public interface SearchEngine {

    /**
     * Parses, retrieves and ranks documents matching a query.
     *
     * @param rawQuery user-provided query
     * @param limit maximum number of returned results
     * @return hits ordered by descending relevance
     */
    List<SearchHit> search(CharSequence rawQuery, int limit);
}
