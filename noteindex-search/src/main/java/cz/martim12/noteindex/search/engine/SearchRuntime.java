package cz.martim12.noteindex.search.engine;

import cz.martim12.noteindex.search.index.SearchIndex;
import cz.martim12.noteindex.search.query.QueryParser;
import cz.martim12.noteindex.search.snippet.SnippetExtractor;

import java.util.Objects;

/**
 * Fully assembled search components sharing one index and analyzer.
 * The query parser is exposed because the application layer needs
 * the same parsed query when producing result snippets.
 */
public record SearchRuntime (
        SearchIndex index,
        QueryParser queryParser,
        SearchEngine searchEngine,
        SnippetExtractor snippetExtractor
) implements AutoCloseable {

    /**
     * Creates a validated search runtime.
     *
     * @param index search index
     * @param queryParser query parser
     * @param searchEngine search engine
     * @param snippetExtractor snippet extractor
     * @throws NullPointerException if any search component is null
     */
    public SearchRuntime {
        Objects.requireNonNull(
                index,
                "Search index must not be null"
        );

        Objects.requireNonNull(
                queryParser,
                "Query parser must not be null"
        );

        Objects.requireNonNull(
                searchEngine,
                "Search engine must not be null"
        );

        Objects.requireNonNull(
                snippetExtractor,
                "Snippet extractor must not be null"
        );
    }

    /**
     * Releases resources owned by the search index.
     */
    @Override
    public void close() {
        index.close();
    }
}
