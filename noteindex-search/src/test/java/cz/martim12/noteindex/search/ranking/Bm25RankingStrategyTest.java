package cz.martim12.noteindex.search.ranking;

import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.index.SearchIndex;
import cz.martim12.noteindex.search.index.SearchIndexes;
import cz.martim12.noteindex.search.query.ParsedQuery;
import cz.martim12.noteindex.search.query.QueryPhrase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Bm25RankingStrategyTest {

    private SearchIndex index;

    @BeforeEach
    void setUp() {
        index = SearchIndexes.inMemory(
                new UnicodeTextAnalyzer()
        );
    }

    @Test
    void returnsZeroWhenDocumentDoesNotContainTerm() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(FieldName.BODY, "Java virtual machine")
        ));

        RankingStrategy ranking = bodyRanking();

        double score = ranking.score(
                1,
                query("sqlite")
        );

        assertEquals(0.0, score);
    }

    @Test
    void rewardsHigherTermFrequencyWithDiminishingReturns() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(FieldName.BODY, "java")
        ));

        index.indexDocument(new IndexDocument(
                2,
                Map.of(FieldName.BODY, "java java")
        ));

        index.indexDocument(new IndexDocument(
                3,
                Map.of(
                        FieldName.BODY,
                        "java java java java"
                )
        ));

        RankingStrategy ranking =
                new Bm25RankingStrategy(
                        index,
                        Map.of(FieldName.BODY, 1.0),
                        new Bm25Parameters(1.2, 0.0)
                );

        double once = ranking.score(1, query("java"));
        double twice = ranking.score(2, query("java"));
        double fourTimes = ranking.score(3, query("java"));

        assertTrue(twice > once);
        assertTrue(fourTimes > twice);

        assertTrue(
                twice - once
                        > fourTimes - twice
        );
    }

    @Test
    void normalizesLongerDocuments() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.BODY,
                        "java concise"
                )
        ));

        index.indexDocument(new IndexDocument(
                2,
                Map.of(
                        FieldName.BODY,
                        "java appears inside a considerably "
                                + "longer unrelated document"
                )
        ));

        RankingStrategy ranking = bodyRanking();

        double shortDocument =
                ranking.score(1, query("java"));

        double longDocument =
                ranking.score(2, query("java"));

        assertTrue(shortDocument > longDocument);
    }

    @Test
    void givesRareTermsMoreWeight() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.BODY,
                        "common rare"
                )
        ));

        index.indexDocument(new IndexDocument(
                2,
                Map.of(FieldName.BODY, "common")
        ));

        index.indexDocument(new IndexDocument(
                3,
                Map.of(FieldName.BODY, "common")
        ));

        RankingStrategy ranking = bodyRanking();

        double rareScore =
                ranking.score(1, query("rare"));

        double commonScore =
                ranking.score(1, query("common"));

        assertTrue(rareScore > commonScore);
    }

    @Test
    void appliesConfiguredTitleBoost() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.TITLE,
                        "Java",
                        FieldName.BODY,
                        "Runtime"
                )
        ));

        index.indexDocument(new IndexDocument(
                2,
                Map.of(
                        FieldName.TITLE,
                        "Runtime",
                        FieldName.BODY,
                        "Java"
                )
        ));

        RankingStrategy ranking =
                new Bm25RankingStrategy(
                        index,
                        Map.of(
                                FieldName.TITLE, 3.0,
                                FieldName.BODY, 1.0
                        )
                );

        double titleMatch =
                ranking.score(1, query("java"));

        double bodyMatch =
                ranking.score(2, query("java"));

        assertTrue(titleMatch > bodyMatch);
    }

    @Test
    void scoresTermsContainedInsidePhraseQueries() {
        index.indexDocument(new IndexDocument(
                1,
                Map.of(
                        FieldName.BODY,
                        "binary search tree"
                )
        ));

        RankingStrategy ranking = bodyRanking();

        ParsedQuery phraseQuery = new ParsedQuery(
                List.of(),
                List.of(
                        new QueryPhrase(
                                List.of(
                                        "binary",
                                        "search",
                                        "tree"
                                )
                        )
                )
        );

        assertTrue(ranking.score(1, phraseQuery) > 0.0);
    }

    @Test
    void rejectsUnknownDocument() {
        RankingStrategy ranking = bodyRanking();

        assertThrows(
                IllegalArgumentException.class,
                () -> ranking.score(
                        999,
                        query("java")
                )
        );
    }

    private RankingStrategy bodyRanking() {
        return new Bm25RankingStrategy(
                index,
                Map.of(FieldName.BODY, 1.0)
        );
    }

    private static ParsedQuery query(String term) {
        return new ParsedQuery(
                List.of(term),
                List.of()
        );
    }
}