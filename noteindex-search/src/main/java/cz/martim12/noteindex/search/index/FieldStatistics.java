package cz.martim12.noteindex.search.index;

/**
 * Collection statistics for one searchable field.
 *
 * @param documentsWithField number of documents containing the field
 * @param totalTokenCount total analyzed tokens in the field
 */
public record FieldStatistics (
      long documentsWithField,
      long totalTokenCount
) {

    /**
     * Creates validated field statistics.
     *
     * @param documentsWithField number of documents containing the field
     * @param totalTokenCount total analyzed tokens in the field
     * @throws IllegalArgumentException if statistics values are invalid
     */
    public FieldStatistics {
        if (documentsWithField < 0) {
            throw new IllegalArgumentException(
                    "Number of documents with field must be non-negative"
            );
        }

        if (totalTokenCount < 0) {
            throw new IllegalArgumentException(
                    "Total token count must be non-negative"
            );
        }

        if (documentsWithField == 0 && totalTokenCount != 0) {
            throw new IllegalArgumentException(
                    "An empty field collection cannot contain tokens"
            );
        }
    }

    /**
     * Calculates the average number of tokens per document containing this field.
     *
     * @return average field length, or zero when no documents contain the field
     */
    public double averageFieldLength() {
        if (documentsWithField == 0) {
            return 0.0;
        }
        return (double) totalTokenCount / documentsWithField;
    }
}
