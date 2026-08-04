package cz.martim12.noteindex.search.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldStatisticsTest {

    @Test
    void calculatesAverageFieldLength() {
        FieldStatistics statistics =
                new FieldStatistics(4, 100);

        assertEquals(25.0, statistics.averageFieldLength());
    }

    @Test
    void returnsZeroAverageForEmptyCollection() {
        FieldStatistics statistics =
                new FieldStatistics(0, 0);

        assertEquals(0.0, statistics.averageFieldLength());
    }

    @Test
    void rejectsTokensWithoutDocuments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FieldStatistics(0, 10)
        );
    }
}
