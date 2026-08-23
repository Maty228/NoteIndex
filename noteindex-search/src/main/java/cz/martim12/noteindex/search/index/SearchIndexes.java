package cz.martim12.noteindex.search.index;

import cz.martim12.noteindex.search.analysis.TextAnalyzer;
import cz.martim12.noteindex.search.index.memory.InMemorySearchIndex;

import java.util.Objects;

/**
 * Factory methods for creating search index implementations.
 */
public final class SearchIndexes {

    private SearchIndexes() {
        throw new AssertionError("Utility class must not be instantiated");
    }

    /**
     * Creates a non-persistent in-memory search index.
     *
     * @param analyzer analyzer used for tokenizing indexed content
     * @return new search index
     */
    public static SearchIndex inMemory(TextAnalyzer analyzer) {
        Objects.requireNonNull(analyzer, "Analyzer must not be null");

        return new InMemorySearchIndex(analyzer);
    }
}
