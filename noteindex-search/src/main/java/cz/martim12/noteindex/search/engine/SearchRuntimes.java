package cz.martim12.noteindex.search.engine;

import cz.martim12.noteindex.search.analysis.TextAnalyzer;
import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.index.SearchIndex;
import cz.martim12.noteindex.search.index.SearchIndexes;
import cz.martim12.noteindex.search.query.DefaultQueryParser;
import cz.martim12.noteindex.search.query.QueryParser;
import cz.martim12.noteindex.search.query.StandaloneTermMatchMode;
import cz.martim12.noteindex.search.ranking.Bm25RankingStrategy;
import cz.martim12.noteindex.search.ranking.RankingStrategy;
import cz.martim12.noteindex.search.retrieval.CandidateRetriever;
import cz.martim12.noteindex.search.retrieval.DefaultCandidateRetriever;
import cz.martim12.noteindex.search.retrieval.PhraseMatcher;
import cz.martim12.noteindex.search.retrieval.PositionalPhraseMatcher;
import cz.martim12.noteindex.search.snippet.ContextAwareSnippetExtractor;
import cz.martim12.noteindex.search.snippet.SnippetExtractor;

import java.util.Objects;

/**
 * Factory for complete search runtime configurations.
 */
public final class SearchRuntimes {

    private SearchRuntimes() {}

    public static SearchRuntime inMemory() {
        return inMemory(
                new UnicodeTextAnalyzer(),
                SearchConfiguration.defaults(),
                StandaloneTermMatchMode.PREFIX
        );
    }

    public static SearchRuntime inMemory(
            SearchConfiguration configuration
    ) {
        return inMemory(
                new UnicodeTextAnalyzer(),
                configuration
        );
    }

    public static SearchRuntime inMemory(
            TextAnalyzer analyzer,
            SearchConfiguration configuration
    ) {
        return inMemory(
                analyzer,
                configuration,
                StandaloneTermMatchMode.PREFIX
        );
    }

    public static SearchRuntime inMemory(
            SearchConfiguration configuration,
            StandaloneTermMatchMode standaloneTermMatchMode
    ) {
        return inMemory(
                new UnicodeTextAnalyzer(),
                configuration,
                standaloneTermMatchMode
        );
    }

    public static SearchRuntime inMemory(
            TextAnalyzer analyzer,
            SearchConfiguration configuration,
            StandaloneTermMatchMode standaloneTermMatchMode
    ) {
        Objects.requireNonNull(
                analyzer,
                "Text analyzer must not be null"
        );

        Objects.requireNonNull(
                configuration,
                "Search configuration must not be null"
        );

        Objects.requireNonNull(
                standaloneTermMatchMode,
                "Standalone term match mode must not be null"
        );

        SearchIndex index =
                SearchIndexes.inMemory(analyzer);

        try {
            QueryParser queryParser =
                    new DefaultQueryParser(analyzer);

            PhraseMatcher phraseMatcher =
                    new PositionalPhraseMatcher(index);

            CandidateRetriever candidateRetriever =
                    new DefaultCandidateRetriever(
                            index,
                            phraseMatcher,
                            configuration.fields(),
                            standaloneTermMatchMode
                    );

            RankingStrategy rankingStrategy =
                    new Bm25RankingStrategy(
                            index,
                            configuration.fieldWeights(),
                            configuration.bm25Parameters(),
                            standaloneTermMatchMode
                    );

            SearchEngine searchEngine =
                    new DefaultSearchEngine(
                            queryParser,
                            candidateRetriever,
                            rankingStrategy,
                            phraseMatcher,
                            configuration.fields(),
                            configuration.phraseOccurrenceBonus()
                    );

            SnippetExtractor snippetExtractor =
                    new ContextAwareSnippetExtractor(
                            analyzer
                    );

            return new SearchRuntime(
                    index,
                    queryParser,
                    searchEngine,
                    snippetExtractor
            );

        } catch (RuntimeException exception) {
            index.close();
            throw exception;
        }
    }
}
