package cz.martim12.noteindex.search.snippet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnippetTest {

    @Test
    void addsEllipsesForTruncatedContext() {
        Snippet snippet = new Snippet(
                "binary tree",
                10,
                21,
                true,
                true
        );

        assertEquals(
                "...binary tree...",
                snippet.displayText()
        );
    }

    @Test
    void preservesTextWithoutTruncation() {
        Snippet snippet = new Snippet(
                "Java",
                0,
                4,
                false,
                false
        );

        assertEquals("Java", snippet.displayText());
    }

    @Test
    void rejectsOffsetsThatDoNotMatchTextLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Snippet(
                        "Java",
                        0,
                        10,
                        false,
                        false
                )
        );
    }
}