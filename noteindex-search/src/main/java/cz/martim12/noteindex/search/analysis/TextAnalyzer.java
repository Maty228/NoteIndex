package cz.martim12.noteindex.search.analysis;

import java.util.List;

/**
 * Converts text into normalized searchable tokens.
 *
 * The same analyzer must be used for documents and search queries.
 */
@FunctionalInterface
public interface TextAnalyzer {

    /**
     * Analyzes the supplied text.
     *
     * @param text source text
     * @return analyzed tokens ordered by their position
     */
    List<AnalyzedToken> analyze(CharSequence text);
}
