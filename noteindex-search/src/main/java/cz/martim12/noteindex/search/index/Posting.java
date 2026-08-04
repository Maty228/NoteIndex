package cz.martim12.noteindex.search.index;

import java.util.List;
import java.util.Objects;

/**
 * Describes the positions of one term inside one document field.
 *
 * Positions are ordered, unique and zero-based.
 */
public record Posting (
        long documentId,
        List<Integer> positions
){

    public Posting {
        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }

        Objects.requireNonNull(positions, "Positions must not be null");

        if (positions.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one position must be provided"
            );
        }

        int previousPosition = -1;

        for (Integer position : positions) {
            Objects.requireNonNull(position, "Position must not be null");

            if (position < 0) {
                throw new IllegalArgumentException(
                        "Position must not be negative"
                );
            }

            if (position <= previousPosition) {
                throw new IllegalArgumentException(
                        "Positions must be strictly increasing"
                );
            }

            previousPosition = position;
        }

        positions = List.copyOf(positions);
    }

    public int termFrequency() {
        return positions.size();
    }
}
