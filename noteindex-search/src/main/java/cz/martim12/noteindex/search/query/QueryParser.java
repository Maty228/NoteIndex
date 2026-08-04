package cz.martim12.noteindex.search.query;

/**
 * Converts raw user input into a normalized query model.
 */
@FunctionalInterface
public interface QueryParser {
    ParsedQuery parse(CharSequence query);
}
