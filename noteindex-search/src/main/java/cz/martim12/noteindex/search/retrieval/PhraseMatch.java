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

    public int occurrenceCount() {
        return startPositions.size();
    }
}
