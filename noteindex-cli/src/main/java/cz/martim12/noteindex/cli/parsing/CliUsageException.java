package cz.martim12.noteindex.cli.parsing;

/**
 * Indicates invalid command-line syntax or arguments.
 */
public class CliUsageException extends IllegalArgumentException {
    public CliUsageException(String message) {
        super(message);
    }
}
