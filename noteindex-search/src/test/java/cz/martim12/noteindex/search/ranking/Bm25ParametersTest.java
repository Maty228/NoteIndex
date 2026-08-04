package cz.martim12.noteindex.search.ranking;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Bm25ParametersTest {

    @Test
    void providesStandardDefaultValues() {
        assertEquals(1.2, Bm25Parameters.DEFAULT.k1());
        assertEquals(0.75, Bm25Parameters.DEFAULT.b());
    }

    @Test
    void rejectsNonPositiveK1() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Bm25Parameters(0.0, 0.75)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Bm25Parameters(-1.0, 0.75)
        );
    }

    @Test
    void rejectsInvalidLengthNormalization() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Bm25Parameters(1.2, -0.1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Bm25Parameters(1.2, 1.1)
        );
    }
}