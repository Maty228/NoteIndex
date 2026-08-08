package cz.martim12.noteindex.application.search;

import cz.martim12.noteindex.core.model.*;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.engine.SearchEngine;
import cz.martim12.noteindex.search.engine.SearchHit;
import cz.martim12.noteindex.search.query.DefaultQueryParser;
import cz.martim12.noteindex.search.query.QueryParser;
import cz.martim12.noteindex.search.snippet.ContextAwareSnippetExtractor;
import cz.martim12.noteindex.search.snippet.SnippetExtractor;
import cz.martim12.noteindex.search.query.StandaloneTermMatchMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DocumentSearchWorkflowTest {

    private StubDocumentRepository repository;
    private StubSearchEngine searchEngine;
    private DocumentSearchWorkflow workflow;

    @BeforeEach
    void setUp() {
        UnicodeTextAnalyzer analyzer =
                new UnicodeTextAnalyzer();

        QueryParser queryParser =
                new DefaultQueryParser(analyzer);

        SnippetExtractor snippetExtractor =
                new ContextAwareSnippetExtractor(
                        analyzer,
                        StandaloneTermMatchMode.PREFIX
                );

        repository = new StubDocumentRepository(
                List.of(
                        document(
                                1,
                                "Java Virtual Machine",
                                "The virtual machine executes "
                                        + "Java bytecode."
                        ),
                        document(
                                2,
                                "Runtime Memory",
                                "Garbage collection in the "
                                        + "virtual machine manages "
                                        + "runtime memory."
                        )
                )
        );

        searchEngine = new StubSearchEngine();

        workflow = new DocumentSearchWorkflow(
                repository,
                searchEngine,
                queryParser,
                snippetExtractor,
                45
        );
    }

    @Test
    void convertsRankedHitsIntoSearchResults() {
        searchEngine.hits = List.of(
                new SearchHit(
                        2,
                        4.5,
                        2.0
                ),
                new SearchHit(
                        1,
                        3.0,
                        2.0
                )
        );

        List<SearchResult> results =
                workflow.search(
                        new SearchQuery(
                                "\"virtual machine\""
                        ),
                        10
                );

        assertEquals(2, results.size());

        SearchResult first = results.getFirst();

        assertEquals(2, first.document().id());
        assertEquals("Runtime Memory", first.document().title());
        assertEquals("txt", first.document().format());
        assertEquals(6.5, first.score());

        assertTrue(
                first.snippet()
                        .contains("virtual machine")
        );

        SearchResult second = results.getLast();

        assertEquals(1, second.document().id());
        assertEquals(5.0, second.score());

        assertTrue(
                second.snippet()
                        .contains("virtual machine")
        );
    }

    @Test
    void preservesSearchHitOrderWhenLoadingDocuments() {
        searchEngine.hits = List.of(
                new SearchHit(2, 5.0, 0.0),
                new SearchHit(1, 4.0, 0.0)
        );

        List<SearchResult> results =
                workflow.search(
                        new SearchQuery("virtual"),
                        10
                );

        assertEquals(
                List.of(2L, 1L),
                results.stream()
                        .map(result ->
                                result.document().id()
                        )
                        .toList()
        );

        assertEquals(
                List.of(2L, 1L),
                repository.requestedDocumentIds
        );
    }

    @Test
    void skipsHitsMissingFromRepository() {
        searchEngine.hits = List.of(
                new SearchHit(99, 10.0, 0.0),
                new SearchHit(1, 5.0, 0.0)
        );

        List<SearchResult> results =
                workflow.search(
                        new SearchQuery("virtual"),
                        10
                );

        assertEquals(1, results.size());
        assertEquals(1, results.getFirst().document().id());

        assertEquals(
                List.of(99L, 1L),
                repository.requestedDocumentIds
        );
    }

    @Test
    void passesRawQueryAndLimitToSearchEngine() {
        searchEngine.hits = List.of();

        List<SearchResult> results =
                workflow.search(
                        new SearchQuery("java collections"),
                        7
                );

        assertTrue(results.isEmpty());
        assertEquals(
                "java collections",
                searchEngine.receivedQuery
        );
        assertEquals(7, searchEngine.receivedLimit);
    }

    @Test
    void returnsImmutableResultList() {
        searchEngine.hits = List.of(
                new SearchHit(1, 1.0, 0.0)
        );

        List<SearchResult> results =
                workflow.search(
                        new SearchQuery("virtual"),
                        10
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> results.add(
                        new SearchResult(
                                new DocumentSummary(
                                        20,
                                        "Other",
                                        "txt",
                                        Instant.EPOCH
                                ),
                                1.0,
                                "snippet"
                        )
                )
        );
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(
                NullPointerException.class,
                () -> workflow.search(null, 10)
        );

        assertThrows(
                NullPointerException.class,
                () -> workflow.search(
                        new SearchQuery(null),
                        10
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> workflow.search(
                        new SearchQuery("java"),
                        0
                )
        );
    }

    @Test
    void rejectsInvalidSnippetLength() {
        UnicodeTextAnalyzer analyzer =
                new UnicodeTextAnalyzer();

        assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentSearchWorkflow(
                        repository,
                        searchEngine,
                        new DefaultQueryParser(analyzer),
                        new ContextAwareSnippetExtractor(
                                analyzer
                        ),
                        0
                )
        );
    }

    @Test
    void exposesCompleteMatchedTokenForHighlighting() {
        searchEngine.hits = List.of(
                new SearchHit(
                        1,
                        3.0,
                        0.0
                )
        );

        List<SearchResult> results =
                workflow.search(
                        new SearchQuery("virt"),
                        10
                );

        SearchResult result =
                results.getFirst();

        assertFalse(
                result.contentHighlights()
                        .isEmpty()
        );

        HighlightRange range =
                result.contentHighlights()
                        .getFirst();

        assertEquals(
                new HighlightRange(4, 11),
                range
        );
    }

    private static Document document(
            long id,
            String title,
            String searchableContent
    ) {
        return new Document(
                id,
                title,
                "file:///notes/" + id + ".txt",
                "txt",
                searchableContent,
                searchableContent,
                Instant.parse(
                        "2026-08-04T20:00:00Z"
                )
        );
    }

    private static final class StubSearchEngine
            implements SearchEngine {

        private List<SearchHit> hits = List.of();
        private String receivedQuery;
        private int receivedLimit;

        @Override
        public List<SearchHit> search(
                CharSequence rawQuery,
                int limit
        ) {
            receivedQuery = rawQuery.toString();
            receivedLimit = limit;

            return hits;
        }
    }

    private static final class StubDocumentRepository
            implements DocumentRepository {

        private final Map<Long, Document> documents;
        private final List<Long> requestedDocumentIds =
                new ArrayList<>();

        private StubDocumentRepository(
                List<Document> documents
        ) {
            Map<Long, Document> mappedDocuments =
                    new LinkedHashMap<>();

            for (Document document : documents) {
                mappedDocuments.put(
                        document.id(),
                        document
                );
            }

            this.documents =
                    Map.copyOf(mappedDocuments);
        }

        @Override
        public Optional<Document> findById(long id) {
            requestedDocumentIds.add(id);

            return Optional.ofNullable(
                    documents.get(id)
            );
        }

        @Override
        public Document save(
                ImportedDocument document
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Document> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DocumentSummary> findAllSummaries() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsBySourceUri(
                String sourceUri
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean updateDisplayTitle(long id, String displayTitle) {
            throw new UnsupportedOperationException();
        }
    }
}