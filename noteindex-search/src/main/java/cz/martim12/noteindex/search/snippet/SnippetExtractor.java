package cz.martim12.noteindex.search.snippet;

import cz.martim12.noteindex.search.query.ParsedQuery;

/**
 * Selects a relevant context region from document text.
 */
@FunctionalInterface
public interface SnippetExtractor {

    /**
     * Extracts a context snippet for a parsed query.
     *
     * @param source source document text
     * @param query parsed search query
     * @param maximumLength preferred maximum character length
     * @return extracted snippet
     */
    Snippet extract(CharSequence source, ParsedQuery query, int maximumLength);
}
