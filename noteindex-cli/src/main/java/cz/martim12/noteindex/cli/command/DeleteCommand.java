package cz.martim12.noteindex.cli.command;

public record DeleteCommand(long documentId) implements CliCommand {

    public DeleteCommand {
        if (documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be positive");
        }
    }
}
