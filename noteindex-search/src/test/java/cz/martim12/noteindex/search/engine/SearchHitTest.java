package cz.martim12.noteindex.search.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchHitTest {

    @Test
    void combinesLexicalScoreAndPhraseBoost() {
        SearchHit hit = new SearchHit(
                42,
                3.5,
                2.0
        );

        assertEquals(5.5, hit.score());
    }

    @Test
    void rejectsInvalidScores() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchHit(1, -1.0, 0.0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchHit(
                        1,
                        Double.NaN,
                        0.0
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new SearchHit(
                        1,
                        1.0,
                        Double.POSITIVE_INFINITY
                )
        );
    }
}