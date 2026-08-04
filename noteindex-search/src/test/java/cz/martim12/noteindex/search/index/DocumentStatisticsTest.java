package cz.martim12.noteindex.search.index;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentStatisticsTest {

    @Test
    void returnsFieldAndTotalLengths() {
        DocumentStatistics statistics = new DocumentStatistics(
                5,
                Map.of(
                        FieldName.TITLE, 3,
                        FieldName.BODY, 120
                )
        );

        assertEquals(3, statistics.fieldLength(FieldName.TITLE));
        assertEquals(120, statistics.fieldLength(FieldName.BODY));
        assertEquals(123, statistics.totalLength());
    }

    @Test
    void returnsZeroForMissingField() {
        DocumentStatistics statistics = new DocumentStatistics(
                1,
                Map.of(FieldName.TITLE, 2)
        );

        assertEquals(
                0,
                statistics.fieldLength(new FieldName("formula"))
        );
    }

    @Test
    void createsDefensiveCopy() {
        Map<FieldName, Integer> lengths = new LinkedHashMap<>();
        lengths.put(FieldName.TITLE, 2);

        DocumentStatistics statistics =
                new DocumentStatistics(1, lengths);

        lengths.put(FieldName.BODY, 10);

        assertEquals(
                Map.of(FieldName.TITLE, 2),
                statistics.fieldLengths()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> statistics.fieldLengths()
                        .put(FieldName.BODY, 5)
        );
    }

    @Test
    void rejectsNegativeFieldLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DocumentStatistics(
                        1,
                        Map.of(FieldName.BODY, -1)
                )
        );
    }
}
