package cz.martim12.noteindex.search.index;

import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}
