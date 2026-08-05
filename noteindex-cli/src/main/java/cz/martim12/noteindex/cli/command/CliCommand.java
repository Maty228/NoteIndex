package cz.martim12.noteindex.cli.command;

/**
 * Parsed command accepted by the NoteIndex CLI.
 */
public sealed interface CliCommand
        permits ImportCommand,
                SearchCommand,
                ListCommand,
                ShowCommand,
                DeleteCommand,
                FormatsCommand,
                HelpCommand,
                VersionCommand {
}
