package cz.martim12.noteindex.search.ranking;

/**
 * Configuration parameters for BM25 ranking.
 *
 * @param k1 controls term-frequency saturation
 * @param b controls document-length normalization
 */
public record Bm25Parameters (double k1, double b) {
    public static final Bm25Parameters DEFAULT = new Bm25Parameters(1.2, 0.75);

    /**
     * Creates validated BM25 parameters.
     *
     * @param k1 term-frequency saturation parameter
     * @param b document-length normalization parameter
     * @throws IllegalArgumentException if parameters are outside supported ranges
     */
    public Bm25Parameters {
        if (!Double.isFinite(k1) || k1 <= 0.0) {
            throw new IllegalArgumentException(
                    "BM25 k1 must be finite and positive"
            );
        }

        if (!Double.isFinite(b) || b < 0.0 || b > 1.0) {
            throw new IllegalArgumentException(
                    "BM25 b must be between 0 and 1"
            );
        }
    }
}
