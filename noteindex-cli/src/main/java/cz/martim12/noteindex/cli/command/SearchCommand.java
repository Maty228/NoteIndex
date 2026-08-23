package cz.martim12.noteindex.cli.command;

/**
 * Command for searching indexed documents.
 *
 * @param query search query text
 * @param limit maximum number of results
 */
public record SearchCommand(String query, int limit) implements CliCommand{

    /**
     * Creates a validated search command.
     *
     * @param query search query text
     * @param limit maximum number of results
     * @throws IllegalArgumentException if the query is blank or the limit is not positive
     */
    public SearchCommand {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }

        query = query.trim();
    }
}
