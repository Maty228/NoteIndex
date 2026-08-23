package cz.martim12.noteindex.search.index;


import java.util.*;

/**
 * Stores analyzed token counts for one indexed document.
 */
public record DocumentStatistics (
        long documentId,
        Map<FieldName, Integer> fieldLengths
) {
    /**
     * Creates validated document statistics.
     *
     * @param documentId indexed document ID
     * @param fieldLengths analyzed token counts by field
     * @throws IllegalArgumentException if the document ID or field lengths are invalid
     */
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

    /**
     * Returns the token count for a field.
     *
     * @param field field to inspect
     * @return field length, or zero when the field is not present
     */
    public int fieldLength(FieldName field) {
        Objects.requireNonNull(field, "Field name must not be null");
        return fieldLengths.getOrDefault(field, 0);
    }

    /**
     * Returns the total number of tokens across all fields.
     *
     * @return total token count
     */
    public long totalLength() {
        return fieldLengths.values()
                .stream()
                .mapToLong(Integer::longValue)
                .sum();
    }
}
