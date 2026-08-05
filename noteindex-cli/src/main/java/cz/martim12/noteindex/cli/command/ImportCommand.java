package cz.martim12.noteindex.cli.command;

import java.nio.file.Path;
import java.util.Objects;

public record ImportCommand(Path source) implements CliCommand {

    public ImportCommand {
        source = Objects.requireNonNull(source);
    }
}
