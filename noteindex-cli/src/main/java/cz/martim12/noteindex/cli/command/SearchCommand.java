package cz.martim12.noteindex.cli.command;

public record SearchCommand(String query, int limit) implements CliCommand{

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
