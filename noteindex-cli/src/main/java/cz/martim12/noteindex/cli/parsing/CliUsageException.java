package cz.martim12.noteindex.cli.parsing;

/**
 * Indicates invalid command-line syntax or arguments.
 */
public class CliUsageException extends IllegalArgumentException {
    /**
     * Creates a CLI usage exception.
     *
     * @param message description of the invalid usage
     */
    public CliUsageException(String message) {
        super(message);
    }
}
