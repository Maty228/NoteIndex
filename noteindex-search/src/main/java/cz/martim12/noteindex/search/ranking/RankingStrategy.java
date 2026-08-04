package cz.martim12.noteindex.search.ranking;


import cz.martim12.noteindex.search.query.ParsedQuery;

/**
 * Calculates a relevance score for an indexed document.
 */
@FunctionalInterface
public interface RankingStrategy {

    /**
     * Calculates a relevance score for one document.
     *
     * @param documentId indexed document ID
     * @param query parsed and normalized query
     * @return non-negative relevance score
     */
    double score(long documentId, ParsedQuery query);
}
