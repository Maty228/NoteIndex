package cz.martim12.noteindex.search.analysis;

import java.util.Objects;

/**
 * A normalized token produced by text analysis.
 *
 * @param term        normalized searchable term
 * @param position    sequential token position, starting at zero
 * @param startOffset start position in the original text, inclusive
 * @param endOffset   end position in the original text, exclusive
 */
public record AnalyzedToken (
        String term,
        int position,
        int startOffset,
        int endOffset
) {
    /**
     * Creates a validated analyzed token.
     *
     * @param term normalized searchable term
     * @param position sequential token position, starting at zero
     * @param startOffset start position in the original text, inclusive
     * @param endOffset end position in the original text, exclusive
     * @throws NullPointerException if the term is null
     * @throws IllegalArgumentException if the term is blank, the position is negative,
     *         or the offsets are invalid
     */
    public AnalyzedToken {
        Objects.requireNonNull(term, "Term must not be null");

        if (term.isBlank()) {
            throw new IllegalArgumentException(
                    "Term must not be blank"
            );
        }

        if (position < 0) {
            throw new IllegalArgumentException(
                    "Token position must not be negative"
            );
        }

        if (startOffset < 0) {
            throw new IllegalArgumentException(
                    "Start offset must not be negative"
            );
        }

        if (endOffset <= startOffset) {
            throw new IllegalArgumentException(
                    "End offset must be greater than start offset"
            );
        }
    }
}
