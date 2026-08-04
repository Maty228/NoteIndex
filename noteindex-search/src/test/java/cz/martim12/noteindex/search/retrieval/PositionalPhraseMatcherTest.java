package cz.martim12.noteindex.search.retrieval;

import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.index.SearchIndex;
import cz.martim12.noteindex.search.index.SearchIndexes;
import cz.martim12.noteindex.search.query.QueryPhrase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionalPhraseMatcherTest {

    private SearchIndex index;
    private PhraseMatcher matcher;

    @BeforeEach
    void setUp() {
        index = SearchIndexes.inMemory(
                new UnicodeTextAnalyzer()
        );

        matcher = new PositionalPhraseMatcher(index);
    }

    @Test
    void findsExactPhraseInMultipleFields() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Binary Search Tree",
                        FieldName.BODY,
                        "A binary search tree stores values"
                )
        ));

        List<PhraseMatch> matches = matcher.findMatches(
                new QueryPhrase(
                        List.of("binary", "search", "tree")
                ),
                List.of(FieldName.TITLE, FieldName.BODY)
        );

        assertEquals(
                List.of(
                        new PhraseMatch(
                                1,
                                FieldName.BODY,
                                List.of(1)
                        ),
                        new PhraseMatch(
                                1,
                                FieldName.TITLE,
                                List.of(0)
                        )
                ),
                matches
        );
    }

    @Test
    void doesNotMatchSeparatedOrReorderedTerms() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.BODY,
                        "Binary balanced search tree"
                )
        ));

        index.indexDocument(new IndexDocument(
                2,
                Map.of(
                        FieldName.BODY,
                        "Tree search binary"
                )
        ));

        List<PhraseMatch> matches = matcher.findMatches(
                new QueryPhrase(
                        List.of("binary", "search", "tree")
                ),
                List.of(FieldName.BODY)
        );

        assertTrue(matches.isEmpty());
    }

    @Test
    void doesNotMatchAcrossDifferentFields() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Binary",
                        FieldName.BODY,
                        "Search tree"
                )
        ));

        List<PhraseMatch> matches = matcher.findMatches(
                new QueryPhrase(
                        List.of("binary", "search", "tree")
                ),
                List.of(FieldName.TITLE, FieldName.BODY)
        );

        assertTrue(matches.isEmpty());
    }

    @Test
    void findsMultiplePhraseOccurrences() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.BODY,
                        "Binary tree and another binary tree"
                )
        ));

        List<PhraseMatch> matches = matcher.findMatches(
                new QueryPhrase(
                        List.of("binary", "tree")
                ),
                List.of(FieldName.BODY)
        );

        assertEquals(
                List.of(
                        new PhraseMatch(
                                1,
                                FieldName.BODY,
                                List.of(0, 4)
                        )
                ),
                matches
        );
    }

    @Test
    void supportsRepeatedTermsInsidePhrase() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.BODY,
                        "Very very useful and very interesting"
                )
        ));

        List<PhraseMatch> matches = matcher.findMatches(
                new QueryPhrase(
                        List.of("very", "very")
                ),
                List.of(FieldName.BODY)
        );

        assertEquals(
                List.of(
                        new PhraseMatch(
                                1,
                                FieldName.BODY,
                                List.of(0)
                        )
                ),
                matches
        );
    }
}