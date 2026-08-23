package cz.martim12.noteindex.cli.command;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Command for importing a document from a source file.
 *
 * @param source source file to import
 */
public record ImportCommand(Path source) implements CliCommand {

    /**
     * Creates an import command.
     *
     * @param source source file to import
     * @throws NullPointerException if the source path is null
     */
    public ImportCommand {
        Objects.requireNonNull(source);
    }
}
