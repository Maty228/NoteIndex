package cz.martim12.noteindex.search.engine;

/**
 * Lightweight ranked result produced by the search engine.
 *
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

    public SearchHit {
        if (documentId <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }

        requireNonNegativeFinite(lexicalScore, "Lexical score");
        requireNonNegativeFinite(phraseBoost, "Phrase boost");
    }

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
