package cz.martim12.noteindex.search.index;

import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemorySearchIndexTest {

    private SearchIndex index;

    @BeforeEach
    void setUp() {
        index = SearchIndexes.inMemory(
                new UnicodeTextAnalyzer()
        );
    }


    @Test
    void indexesTermsAndTokenPositions() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Binary Trees",
                        FieldName.BODY,
                        "Binary trees are useful. Binary search."
                )
        ));

        List<Posting> titlePostings =
                index.postings("binary", FieldName.TITLE);

        List<Posting> bodyPostings =
                index.postings("binary", FieldName.BODY);

        assertEquals(
                List.of(new Posting(1, List.of(0))),
                titlePostings
        );

        assertEquals(
                List.of(new Posting(1, List.of(0, 4))),
                bodyPostings
        );
    }

    @Test
    void keepsFieldsSeparated() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Databases",
                        FieldName.BODY,
                        "Relational databases use tables."
                )
        ));

        assertTrue(
                index.postings(
                        "relational",
                        FieldName.TITLE
                ).isEmpty()
        );

        assertEquals(
                List.of(new Posting(1, List.of(0))),
                index.postings(
                        "relational",
                        FieldName.BODY
                )
        );
    }

    @Test
    void calculatesDocumentStatistics() {
        index.indexDocument(new IndexDocument(
                42,
                Map.of(
                        FieldName.TITLE,
                        "Binary Trees",
                        FieldName.BODY,
                        "AVL trees remain balanced"
                )
        ));

        DocumentStatistics statistics =
                index.documentStatistics(42).orElseThrow();

        assertEquals(2, statistics.fieldLength(FieldName.TITLE));
        assertEquals(4, statistics.fieldLength(FieldName.BODY));
        assertEquals(6, statistics.totalLength());
        assertEquals(1, index.documentCount());
    }

    @Test
    void calculatesCollectionFieldStatistics() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Java",
                        FieldName.BODY,
                        "Java virtual machine"
                )
        ));

        index.indexDocument(new IndexDocument(
                2,
                Map.of(
                        FieldName.TITLE,
                        "SQLite Database",
                        FieldName.BODY,
                        "Embedded relational database system"
                )
        ));

        FieldStatistics titleStatistics =
                index.fieldStatistics(FieldName.TITLE);

        FieldStatistics bodyStatistics =
                index.fieldStatistics(FieldName.BODY);

        assertEquals(2, titleStatistics.documentsWithField());
        assertEquals(3, titleStatistics.totalTokenCount());
        assertEquals(1.5, titleStatistics.averageFieldLength());

        assertEquals(2, bodyStatistics.documentsWithField());
        assertEquals(7, bodyStatistics.totalTokenCount());
        assertEquals(3.5, bodyStatistics.averageFieldLength());
    }

    @Test
    void returnsPostingsOrderedByDocumentId() {
        index.indexDocument(new IndexDocument(
                20,
                Map.of(FieldName.BODY, "java")
        ));

        index.indexDocument(new IndexDocument(
                5,
                Map.of(FieldName.BODY, "java")
        ));

        index.indexDocument(new IndexDocument(
                12,
                Map.of(FieldName.BODY, "java")
        ));

        List<Long> documentIds =
                index.postings("java", FieldName.BODY)
                        .stream()
                        .map(Posting::documentId)
                        .toList();

        assertEquals(List.of(5L, 12L, 20L), documentIds);
    }

    @Test
    void countsAnEmptyFieldInCollectionStatistics() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "",
                        FieldName.BODY,
                        "Some searchable content"
                )
        ));

        FieldStatistics statistics =
                index.fieldStatistics(FieldName.TITLE);

        assertEquals(1, statistics.documentsWithField());
        assertEquals(0, statistics.totalTokenCount());
        assertEquals(0.0, statistics.averageFieldLength());
    }

    @Test
    void returnsEmptyPostingsForUnknownTerm() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(FieldName.BODY, "Java and SQLite")
        ));

        assertTrue(
                index.postings(
                        "python",
                        FieldName.BODY
                ).isEmpty()
        );
    }

    @Test
    void replacesExistingDocument() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Binary Trees",
                        FieldName.BODY,
                        "Trees are useful"
                )
        ));

        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Graph Algorithms",
                        FieldName.BODY,
                        "Graphs contain vertices and edges"
                )
        ));

        assertEquals(1, index.documentCount());

        assertTrue(
                index.postings(
                        "binary",
                        FieldName.TITLE
                ).isEmpty()
        );

        assertTrue(
                index.postings(
                        "trees",
                        FieldName.BODY
                ).isEmpty()
        );

        assertEquals(
                List.of(new Posting(1, List.of(0))),
                index.postings(
                        "graphs",
                        FieldName.BODY
                )
        );

        DocumentStatistics statistics =
                index.documentStatistics(1).orElseThrow();

        assertEquals(2, statistics.fieldLength(FieldName.TITLE));
        assertEquals(5, statistics.fieldLength(FieldName.BODY));
    }

    @Test
    void removesDocumentAndUpdatesStatistics() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(FieldName.BODY, "java java virtual machine")
        ));

        index.indexDocument(new IndexDocument(
                2,
                Map.of(FieldName.BODY, "java collections")
        ));

        assertTrue(index.removeDocument(1));

        assertEquals(1, index.documentCount());
        assertTrue(index.documentStatistics(1).isEmpty());

        assertEquals(
                List.of(new Posting(2, List.of(0))),
                index.postings("java", FieldName.BODY)
        );

        FieldStatistics statistics =
                index.fieldStatistics(FieldName.BODY);

        assertEquals(1, statistics.documentsWithField());
        assertEquals(2, statistics.totalTokenCount());
    }

    @Test
    void returnsFalseWhenRemovingUnknownDocument() {
        assertFalse(index.removeDocument(999));
    }

    @Test
    void clearsAllIndexedData() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(FieldName.BODY, "java virtual machine")
        ));

        index.indexDocument(new IndexDocument(
                2,
                Map.of(FieldName.BODY, "sqlite database")
        ));

        index.clear();

        assertEquals(0, index.documentCount());
        assertTrue(index.documentStatistics(1).isEmpty());
        assertTrue(index.documentStatistics(2).isEmpty());

        assertTrue(
                index.postings("java", FieldName.BODY).isEmpty()
        );

        assertEquals(
                new FieldStatistics(0, 0),
                index.fieldStatistics(FieldName.BODY)
        );
    }

    @Test
    void returnsTermsMatchingPrefixInLexicographicOrder() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.BODY,
                        "neuron neural network neuroscience"
                )
        ));

        assertEquals(
                List.of(
                        "neural",
                        "neuron",
                        "neuroscience"
                ),
                index.termsWithPrefix(
                        "neur",
                        FieldName.BODY
                )
        );
    }

    @Test
    void prefixVocabularyRemainsSeparatedByField() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Neural Models",
                        FieldName.BODY,
                        "Network architecture"
                )
        ));

        assertEquals(
                List.of("neural"),
                index.termsWithPrefix(
                        "neur",
                        FieldName.TITLE
                )
        );

        assertTrue(
                index.termsWithPrefix(
                        "neur",
                        FieldName.BODY
                ).isEmpty()
        );
    }

    @Test
    void removesTermsFromPrefixVocabularyWithDocument() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.BODY,
                        "neural network"
                )
        ));

        assertEquals(
                List.of("neural"),
                index.termsWithPrefix(
                        "neur",
                        FieldName.BODY
                )
        );

        assertTrue(index.removeDocument(1));

        assertTrue(
                index.termsWithPrefix(
                        "neur",
                        FieldName.BODY
                ).isEmpty()
        );
    }
}
