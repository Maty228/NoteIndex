package cz.martim12.noteindex.core.model;

/**
 * Half-open character range used for presentation highlighting.
 *
 * @param startOffset inclusive start offset
 * @param endOffset exclusive end offset
 */
public record HighlightRange(
        int startOffset,
        int endOffset
) {

    public HighlightRange {
        if (startOffset < 0) {
            throw new IllegalArgumentException(
                    "Highlight start offset must not be negative"
            );
        }

        if (endOffset <= startOffset) {
            throw new IllegalArgumentException(
                    "Highlight end offset must follow start offset"
            );
        }
    }
}