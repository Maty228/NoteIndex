package cz.martim12.noteindex.search.snippet;

/**
 * One query match in the original source text.
 * Offsets use the same half-open convention as String.substring().
 */
public record SnippetMatch(
        int sourceStartOffset,
        int sourceEndOffset
) {

    /**
     * Creates a validated snippet match.
     *
     * @param sourceStartOffset inclusive start offset in source text
     * @param sourceEndOffset exclusive end offset in source text
     * @throws IllegalArgumentException if offsets are invalid
     */
    public SnippetMatch {
        if (sourceStartOffset < 0) {
            throw new IllegalArgumentException(
                    "Match start offset must not be negative"
            );
        }

        if (sourceEndOffset <= sourceStartOffset) {
            throw new IllegalArgumentException(
                    "Match end offset must follow start offset"
            );
        }
    }
}
