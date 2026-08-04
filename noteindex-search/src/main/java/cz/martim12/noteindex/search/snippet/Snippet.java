package cz.martim12.noteindex.search.snippet;

import java.util.Objects;

/**
 * A selected region of source text suitable for displaying
 * search-result context.
 *
 * The text is preserved exactly as it appeared in the source.
 *
 * @param text snippet text
 * @param sourceStartOffset inclusive start offset in source text
 * @param sourceEndOffset exclusive end offset in source text
 * @param truncatedAtStart whether source text exists before the snippet
 * @param truncatedAtEnd whether source text exists after the snippet
 */
public record Snippet (
        String text,
        int sourceStartOffset,
        int sourceEndOffset,
        boolean truncatedAtStart,
        boolean truncatedAtEnd
){
    public Snippet {
        Objects.requireNonNull(text, "Snippet text must not be null");

        if (sourceStartOffset < 0) {
            throw new IllegalArgumentException(
                    "Source start offset must not be negative"
            );
        }

        if (sourceEndOffset < sourceStartOffset) {
            throw new IllegalArgumentException(
                    "Source end offset must not precede start offset"
            );
        }

        if (text.length() != sourceEndOffset - sourceStartOffset) {
            throw new IllegalArgumentException(
                    "Snippet length must match its source offsets"
            );
        }
    }

    /**
     * Returns a user-facing representation with ellipses.
     */
    public String displayText() {
        StringBuilder displayed = new StringBuilder();

        if (truncatedAtStart) {
            displayed.append("...");
        }

        displayed.append(text);

        if (truncatedAtEnd) {
            displayed.append("...");
        }

        return displayed.toString();
    }
}
