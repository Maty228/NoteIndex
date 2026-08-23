package cz.martim12.noteindex.cli.command;

/**
 * Command for displaying one document.
 *
 * @param documentId identifier of the document to display
 */
public record ShowCommand(long documentId) implements CliCommand {

    /**
     * Creates a validated show command.
     *
     * @param documentId document identifier to display
     * @throws IllegalArgumentException if the document ID is not positive
     */
    public ShowCommand {
        if (documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be positive");
        }
    }
}
