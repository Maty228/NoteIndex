package cz.martim12.noteindex.search.query;

/**
 * Converts raw user input into a normalized query model.
 */
@FunctionalInterface
public interface QueryParser {
    /**
     * Parses raw query input into a normalized query model.
     *
     * @param query raw user query
     * @return parsed query
     */
    ParsedQuery parse(CharSequence query);
}
