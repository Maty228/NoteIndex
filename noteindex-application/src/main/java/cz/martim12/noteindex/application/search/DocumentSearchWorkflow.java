package cz.martim12.noteindex.application.search;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.search.engine.SearchEngine;
import cz.martim12.noteindex.search.engine.SearchHit;
import cz.martim12.noteindex.search.query.ParsedQuery;
import cz.martim12.noteindex.search.query.QueryParser;
import cz.martim12.noteindex.search.snippet.Snippet;
import cz.martim12.noteindex.search.snippet.SnippetExtractor;
import cz.martim12.noteindex.core.model.HighlightRange;
import cz.martim12.noteindex.search.snippet.SnippetMatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Coordinates ranked search, document loading and snippet
 * extraction.
 * The search index returns lightweight document IDs. Complete
 * document information is loaded from the authoritative
 * persistence repository.
 */
public final class DocumentSearchWorkflow {

    private final DocumentRepository documentRepository;
    private final SearchEngine searchEngine;
    private final QueryParser queryParser;
    private final SnippetExtractor snippetExtractor;
    private final int maximumSnippetLength;

    /**
     * Creates a document search workflow.
     *
     * @param documentRepository repository used to load complete documents
     * @param searchEngine search engine used for ranking
     * @param queryParser parser used for snippet generation
     * @param snippetExtractor extractor used to create result snippets
     * @param maximumSnippetLength maximum generated snippet length
     */
    public DocumentSearchWorkflow(
            DocumentRepository documentRepository,
            SearchEngine searchEngine,
            QueryParser queryParser,
            SnippetExtractor snippetExtractor,
            int maximumSnippetLength
    ) {
        this.documentRepository = Objects.requireNonNull(documentRepository, "Document repository must not be null");
        this.searchEngine = Objects.requireNonNull(searchEngine, "Search engine must not be null");
        this.queryParser = Objects.requireNonNull(queryParser, "Query parser must not be null");
        this.snippetExtractor = Objects.requireNonNull(snippetExtractor, "Snippet extractor must not be null");

        if (maximumSnippetLength <= 0) {
            throw new IllegalArgumentException(
                    "Maximum snippet length must be positive"
            );
        }
        this.maximumSnippetLength = maximumSnippetLength;
    }

    /**
     * Searches indexed documents and converts search hits into application results.
     *
     * <p>Documents missing from the authoritative repository are skipped.</p>
     *
     * @param query user search query
     * @param limit maximum number of returned results
     * @return search results with snippets and highlights
     */
    public List<SearchResult> search(SearchQuery query, int limit) {
        Objects.requireNonNull(query, "Search query must not be null");

        String rawQuery = Objects.requireNonNull(query.text(), "Search query text must not be null");

        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "Search result limit must be positive"
            );
        }

        List<SearchHit> hits = searchEngine.search(rawQuery, limit);

        if (hits.isEmpty()) {
            return List.of();
        }

        /*
         * SearchEngine parses internally for retrieval. The
         * application parses the same text again because snippets
         * need the normalized ParsedQuery representation.
         */
        ParsedQuery parsedQuery = queryParser.parse(rawQuery);
        List<SearchResult> results = new ArrayList<>(hits.size());

        for (SearchHit hit : hits) {
            documentRepository.findById(hit.documentId())
                    .map(document -> createResult(document, hit, parsedQuery))
                    .ifPresent(results::add);
        }

        return List.copyOf(results);
    }

    private SearchResult createResult(Document document, SearchHit hit, ParsedQuery parsedQuery) {
        Snippet snippet =
                snippetExtractor.extract(
                        document.searchableContent(),
                        parsedQuery,
                        maximumSnippetLength
                );

        return new SearchResult(
                toSummary(document),
                hit.score(),
                snippet.displayText(),
                snippetHighlights(snippet),
                contentHighlights(snippet)
        );
    }

    private static List<HighlightRange> contentHighlights(
            Snippet snippet
    ) {
        return snippet.matches()
                .stream()
                .map(match ->
                        new HighlightRange(
                                match.sourceStartOffset(),
                                match.sourceEndOffset()
                        )
                )
                .distinct()
                .toList();
    }

    private static List<HighlightRange> snippetHighlights(Snippet snippet) {
        int displayPrefixLength =
                snippet.truncatedAtStart() ? 3 : 0;

        return snippet.matches()
                .stream()
                .filter(match ->
                        isFullyInsideSnippet(
                                match,
                                snippet
                        )
                )
                .map(match ->
                        new HighlightRange(
                                displayPrefixLength
                                        + match.sourceStartOffset()
                                        - snippet.sourceStartOffset(),

                                displayPrefixLength
                                        + match.sourceEndOffset()
                                        - snippet.sourceStartOffset()
                        )
                )
                .distinct()
                .toList();
    }

    private static DocumentSummary toSummary(Document document) {
        return new DocumentSummary(
                document.id(),
                document.title(),
                document.format(),
                document.importedAt()
        );
    }

    private static boolean isFullyInsideSnippet(
            SnippetMatch match,
            Snippet snippet
    ) {
        return match.sourceStartOffset()
                >= snippet.sourceStartOffset()
                && match.sourceEndOffset()
                <= snippet.sourceEndOffset();
    }

}
