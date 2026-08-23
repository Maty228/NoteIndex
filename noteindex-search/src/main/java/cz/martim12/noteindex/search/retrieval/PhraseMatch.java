package cz.martim12.noteindex.search.retrieval;

import cz.martim12.noteindex.search.index.FieldName;

import java.util.List;
import java.util.Objects;

/**
 * Describes occurrences of a phrase in one document field.
 *
 * @param documentId   matching document
 * @param field        field containing the phrase
 * @param startPositions token positions where the phrase begins
 */
public record PhraseMatch (
        long documentId,
        FieldName field,
        List<Integer> startPositions
) {
    /**
     * Creates a validated phrase match.
     *
     * @param documentId matching document ID
     * @param field field containing the phrase
     * @param startPositions token positions where the phrase begins
     * @throws IllegalArgumentException if the document ID or positions are invalid
     */
    public PhraseMatch {
        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }

        Objects.requireNonNull(field, "Field must not be null");
        Objects.requireNonNull(
                startPositions,
                "Start positions must not be null"
        );

        if (startPositions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Phrase match must contain at least one position"
            );
        }

        int previous = -1;

        for (Integer position : startPositions) {
            Objects.requireNonNull(
                    position,
                    "Start position must not be null"
            );

            if (position < 0) {
                throw new IllegalArgumentException(
                        "Start position must not be negative"
                );
            }

            if (position <= previous) {
                throw new IllegalArgumentException(
                        "Start positions must be strictly increasing"
                );
            }

            previous = position;

        }

        startPositions = List.copyOf(startPositions);
    }

    /**
     * Returns the number of occurrences represented by this match.
     *
     * @return number of phrase occurrences
     */
    public int occurrenceCount() {
        return startPositions.size();
    }
}
