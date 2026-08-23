package cz.martim12.noteindex.cli.parsing;

import cz.martim12.noteindex.cli.command.CliCommand;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Complete result of parsing one CLI invocation.
 *
 * @param databaseFile SQLite database selected for the invocation
 * @param command parsed command
 */
public record CliArguments(Path databaseFile, CliCommand command){

    /**
     * Creates validated CLI arguments.
     *
     * @param databaseFile SQLite database file to use
     * @param command parsed CLI command
     * @throws NullPointerException if the database file or command is null
     */
    public CliArguments {
        databaseFile = Objects.requireNonNull(databaseFile, "Database file must not be null").toAbsolutePath().normalize();

        Objects.requireNonNull(command, "CLI command must not be null");
    }
}
