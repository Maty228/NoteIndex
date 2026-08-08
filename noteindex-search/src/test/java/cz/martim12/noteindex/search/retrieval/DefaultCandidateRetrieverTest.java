package cz.martim12.noteindex.search.retrieval;

import cz.martim12.noteindex.search.analysis.TextAnalyzer;
import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.index.SearchIndex;
import cz.martim12.noteindex.search.index.SearchIndexes;
import cz.martim12.noteindex.search.query.DefaultQueryParser;
import cz.martim12.noteindex.search.query.QueryParser;
import cz.martim12.noteindex.search.query.StandaloneTermMatchMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultCandidateRetrieverTest {

    private QueryParser parser;
    private CandidateRetriever retriever;

    private SearchIndex index;

    @BeforeEach
    void setUp() {
        TextAnalyzer analyzer =
                new UnicodeTextAnalyzer();

        index = SearchIndexes.inMemory(analyzer);

        PhraseMatcher phraseMatcher =
                new PositionalPhraseMatcher(index);

        retriever = new DefaultCandidateRetriever(
                index,
                phraseMatcher,
                List.of(
                        FieldName.TITLE,
                        FieldName.BODY
                )
        );

        parser = new DefaultQueryParser(analyzer);

        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Java Virtual Machine",
                        FieldName.BODY,
                        "Automatic garbage collection"
                )
        ));

        index.indexDocument(new IndexDocument(
                2,
                Map.of(
                        FieldName.TITLE,
                        "Java Collections",
                        FieldName.BODY,
                        "Virtual machine internals"
                )
        ));

        index.indexDocument(new IndexDocument(
                3,
                Map.of(
                        FieldName.TITLE,
                        "SQLite Database",
                        FieldName.BODY,
                        "Embedded relational database"
                )
        ));
    }

    @Test
    void returnsUnionForStandaloneTerms() {
        assertEquals(
                List.of(1L, 2L, 3L),
                retriever.retrieveCandidates(
                        parser.parse("java sqlite")
                )
        );
    }

    @Test
    void findsRequiredPhraseInAnyConfiguredField() {
        assertEquals(
                List.of(1L, 2L),
                retriever.retrieveCandidates(
                        parser.parse("\"virtual machine\"")
                )
        );
    }

    @Test
    void requiresEveryQuotedPhrase() {
        assertEquals(
                List.of(1L),
                retriever.retrieveCandidates(
                        parser.parse(
                                "\"virtual machine\" "
                                        + "\"garbage collection\""
                        )
                )
        );
    }

    @Test
    void treatsStandaloneTermsAsOptionalWithRequiredPhrase() {
        assertEquals(
                List.of(1L, 2L),
                retriever.retrieveCandidates(
                        parser.parse(
                                "sqlite \"virtual machine\""
                        )
                )
        );
    }

    @Test
    void returnsEmptyListWhenRequiredPhraseIsMissing() {
        assertTrue(
                retriever.retrieveCandidates(
                        parser.parse(
                                "\"distributed transaction\""
                        )
                ).isEmpty()
        );
    }

    @Test
    void returnsCandidatesOrderedByDocumentId() {
        assertEquals(
                List.of(1L, 2L),
                retriever.retrieveCandidates(
                        parser.parse("java")
                )
        );
    }

    @Test
    void retrievesStandaloneTermsByPrefixWhenEnabled() {
        PhraseMatcher phraseMatcher =
                new PositionalPhraseMatcher(index);

        CandidateRetriever prefixRetriever =
                new DefaultCandidateRetriever(
                        index,
                        phraseMatcher,
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        StandaloneTermMatchMode.PREFIX
                );

        assertEquals(
                List.of(1L, 2L),
                prefixRetriever.retrieveCandidates(
                        parser.parse("jav")
                )
        );
    }

    @Test
    void exactModeDoesNotRetrievePartialTerms() {
        assertTrue(
                retriever.retrieveCandidates(
                        parser.parse("jav")
                ).isEmpty()
        );
    }

    @Test
    void prefixModeCombinesDifferentMatchingVocabularyTerms() {
        index.indexDocument(new IndexDocument(
                4,
                Map.of(
                        FieldName.TITLE,
                        "Neural Networks",
                        FieldName.BODY,
                        "Learning"
                )
        ));

        index.indexDocument(new IndexDocument(
                5,
                Map.of(
                        FieldName.TITLE,
                        "Neuron Models",
                        FieldName.BODY,
                        "Biology"
                )
        ));

        CandidateRetriever prefixRetriever =
                new DefaultCandidateRetriever(
                        index,
                        new PositionalPhraseMatcher(index),
                        List.of(
                                FieldName.TITLE,
                                FieldName.BODY
                        ),
                        StandaloneTermMatchMode.PREFIX
                );

        assertEquals(
                List.of(4L, 5L),
                prefixRetriever.retrieveCandidates(
                        parser.parse("neur")
                )
        );
    }
}