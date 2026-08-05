package cz.martim12.noteindex.cli.command;

public record ShowCommand(long documentId) implements CliCommand {

    public ShowCommand {
        if (documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be positive");
        }
    }
}
