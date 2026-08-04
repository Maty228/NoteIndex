package cz.martim12.noteindex.search.index;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostingTest {
    @Test
    void derivesTermFrequencyFromPositions() {
        Posting posting = new Posting(
                42,
                List.of(1, 5, 12)
        );

        assertEquals(3, posting.termFrequency());
    }

    @Test
    void createsDefensiveCopyOfPositions() {
        List<Integer> positions = new ArrayList<>(List.of(1, 3));

        Posting posting = new Posting(1, positions);
        positions.add(8);

        assertEquals(List.of(1, 3), posting.positions());

        assertThrows(
                UnsupportedOperationException.class,
                () -> posting.positions().add(10)
        );
    }

    @Test
    void rejectsEmptyPositions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Posting(1, List.of())
        );
    }

    @Test
    void rejectsUnorderedOrDuplicatePositions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Posting(1, List.of(2, 2))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Posting(1, List.of(5, 3))
        );
    }
}
