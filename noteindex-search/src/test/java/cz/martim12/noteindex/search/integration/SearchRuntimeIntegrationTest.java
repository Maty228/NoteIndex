package cz.martim12.noteindex.search.integration;

import cz.martim12.noteindex.search.engine.SearchHit;
import cz.martim12.noteindex.search.engine.SearchRuntime;
import cz.martim12.noteindex.search.engine.SearchRuntimes;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.query.ParsedQuery;
import cz.martim12.noteindex.search.query.QueryParseException;
import cz.martim12.noteindex.search.snippet.Snippet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchRuntimeIntegrationTest {

    @Test
    void executesCompleteRankedPhraseSearch() {
        try (SearchRuntime runtime =
                     SearchRuntimes.inMemory()) {

            String firstBody =
                    "The virtual machine executes Java bytecode.";

            runtime.index().indexDocument(
                    document(
                            1,
                            "Java Virtual Machine",
                            firstBody
                    )
            );

            runtime.index().indexDocument(
                    document(
                            2,
                            "Runtime Internals",
                            "Java virtual machine internals "
                                    + "and memory management."
                    )
            );

            runtime.index().indexDocument(
                    document(
                            3,
                            "SQLite Database",
                            "An embedded relational database."
                    )
            );

            String rawQuery =
                    "java \"virtual machine\"";

            List<SearchHit> hits =
                    runtime.searchEngine()
                            .search(rawQuery, 10);

            assertEquals(
                    List.of(1L, 2L),
                    hits.stream()
                            .map(SearchHit::documentId)
                            .toList()
            );

            assertTrue(
                    hits.getFirst().score()
                            > hits.getLast().score()
            );

            assertEquals(
                    4.0,
                    hits.getFirst().phraseBoost()
            );

            assertEquals(
                    2.0,
                    hits.getLast().phraseBoost()
            );

            ParsedQuery parsedQuery =
                    runtime.queryParser()
                            .parse(rawQuery);

            Snippet snippet =
                    runtime.snippetExtractor()
                            .extract(
                                    firstBody,
                                    parsedQuery,
                                    35
                            );

            assertTrue(
                    snippet.text()
                            .contains("virtual machine")
            );
        }
    }

    @Test
    void reflectsDocumentReplacementImmediately() {
        try (SearchRuntime runtime =
                     SearchRuntimes.inMemory()) {

            runtime.index().indexDocument(
                    document(
                            1,
                            "Java Notes",
                            "Java virtual machine"
                    )
            );

            assertEquals(
                    List.of(1L),
                    runtime.searchEngine()
                            .search("java", 10)
                            .stream()
                            .map(SearchHit::documentId)
                            .toList()
            );

            runtime.index().indexDocument(
                    document(
                            1,
                            "SQLite Notes",
                            "Embedded relational database"
                    )
            );

            assertTrue(
                    runtime.searchEngine()
                            .search("java", 10)
                            .isEmpty()
            );

            assertEquals(
                    List.of(1L),
                    runtime.searchEngine()
                            .search("sqlite", 10)
                            .stream()
                            .map(SearchHit::documentId)
                            .toList()
            );
        }
    }

    @Test
    void supportsCzechTextEndToEnd() {
        try (SearchRuntime runtime =
                     SearchRuntimes.inMemory()) {

            String body =
                    "Studijní poznámky lze prohledávat lokálně.";

            runtime.index().indexDocument(
                    document(
                            1,
                            "Vyhledávání poznámek",
                            body
                    )
            );

            String rawQuery =
                    "vyhledávání \"studijní poznámky\"";

            List<SearchHit> hits =
                    runtime.searchEngine()
                            .search(rawQuery, 10);

            assertEquals(1, hits.size());
            assertEquals(1, hits.getFirst().documentId());

            Snippet snippet =
                    runtime.snippetExtractor()
                            .extract(
                                    body,
                                    runtime.queryParser()
                                            .parse(rawQuery),
                                    100
                            );

            assertTrue(
                    snippet.text()
                            .contains("Studijní poznámky")
            );
        }
    }

    @Test
    void handlesEmptyIndexAndMalformedQueries() {
        try (SearchRuntime runtime =
                     SearchRuntimes.inMemory()) {

            assertTrue(
                    runtime.searchEngine()
                            .search("java", 10)
                            .isEmpty()
            );

            assertThrows(
                    QueryParseException.class,
                    () -> runtime.searchEngine()
                            .search(
                                    "\"unclosed phrase",
                                    10
                            )
            );
        }
    }

    private static IndexDocument document(
            long documentId,
            String title,
            String body
    ) {
        return new IndexDocument(
                documentId,
                Map.of(
                        FieldName.TITLE,
                        title,
                        FieldName.BODY,
                        body
                )
        );
    }
}