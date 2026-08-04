package cz.martim12.noteindex.search.index;


import java.util.*;

/**
 * Stores analyzed token counts for one indexed document.
 */
public record DocumentStatistics (
        long documentId,
        Map<FieldName, Integer> fieldLengths
) {
    public DocumentStatistics {
        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }

        Objects.requireNonNull(fieldLengths, "Field lengths must not be null");

        if (fieldLengths.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one field length must be provided"
            );
        }

        Map<FieldName, Integer> copy = new LinkedHashMap<>();

        fieldLengths.forEach((field, length) -> {
            Objects.requireNonNull(field, "Field name must not be null");
            Objects.requireNonNull(length, "Field length must not be null");
            if (length < 0) {
                throw new IllegalArgumentException(
                        "Field length must not be negative"
                );
            }
            copy.put(field, length);
        });

        fieldLengths = Collections.unmodifiableMap(copy);
    }

    public int fieldLength(FieldName field) {
        Objects.requireNonNull(field, "Field name must not be null");
        return fieldLengths.getOrDefault(field, 0);
    }

    public long totalLength() {
        return fieldLengths.values()
                .stream()
                .mapToLong(Integer::longValue)
                .sum();
    }
}
