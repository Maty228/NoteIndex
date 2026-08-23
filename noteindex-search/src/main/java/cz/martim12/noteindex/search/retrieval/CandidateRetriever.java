package cz.martim12.noteindex.search.retrieval;

import cz.martim12.noteindex.search.query.ParsedQuery;

import java.util.List;

/**
 * Selects documents that may satisfy a parsed query.
 */
@FunctionalInterface
public interface CandidateRetriever {

    /**
     * Returns document IDs that may satisfy the query.
     *
     * @param query parsed query
     * @return candidate document IDs in ascending order
     */
    List<Long> retrieveCandidates(ParsedQuery query);
}
