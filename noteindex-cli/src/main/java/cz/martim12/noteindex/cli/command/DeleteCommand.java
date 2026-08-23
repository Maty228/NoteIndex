package cz.martim12.noteindex.cli.command;

/**
 * Command for deleting a document by its identifier.
 */
public record DeleteCommand(long documentId) implements CliCommand {

    /**
     * Creates a validated delete command.
     *
     * @param documentId document identifier to delete
     * @throws IllegalArgumentException if the document ID is not positive
     */
    public DeleteCommand {
        if (documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be positive");
        }
    }
}
