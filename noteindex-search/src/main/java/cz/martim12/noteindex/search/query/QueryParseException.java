package cz.martim12.noteindex.search.query;

/**
 * Indicates that a user search query could not be parsed.
 */
public final class QueryParseException extends IllegalArgumentException {

    /**
     * Creates a query parsing exception.
     *
     * @param message description of the parsing error
     */
    public QueryParseException(String message) {
        super(message);
    }
}
