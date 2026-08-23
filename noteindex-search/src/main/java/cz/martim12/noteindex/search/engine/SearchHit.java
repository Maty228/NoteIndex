package cz.martim12.noteindex.search.engine;

/**
 * Lightweight ranked result produced by the search engine.
 * The search module returns document IDs only. Document content
 * remains owned by the persistence layer.
 *
 * @param documentId indexed document ID
 * @param lexicalScore score produced by the ranking strategy
 * @param phraseBoost additional score for exact phrase occurrences
 */
public record SearchHit (
        long documentId,
        double lexicalScore,
        double phraseBoost
) {

    /**
     * Creates a validated search hit.
     *
     * @param documentId indexed document ID
     * @param lexicalScore score produced by the ranking strategy
     * @param phraseBoost additional score for exact phrase occurrences
     * @throws IllegalArgumentException if the document ID is invalid or scores
     *         are not non-negative finite values
     */
    public SearchHit {
        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }

        requireNonNegativeFinite(lexicalScore, "Lexical score");
        requireNonNegativeFinite(phraseBoost, "Phrase boost");
    }

    /**
     * Returns the combined relevance score.
     *
     * @return lexical score plus phrase boost
     */
    public double score() {
        return lexicalScore + phraseBoost;
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be non-negative finite"
            );
        }
    }
}
