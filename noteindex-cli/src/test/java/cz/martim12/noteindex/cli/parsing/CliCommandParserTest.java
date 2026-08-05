package cz.martim12.noteindex.cli.parsing;

import cz.martim12.noteindex.cli.command.DeleteCommand;
import cz.martim12.noteindex.cli.command.FormatsCommand;
import cz.martim12.noteindex.cli.command.HelpCommand;
import cz.martim12.noteindex.cli.command.ImportCommand;
import cz.martim12.noteindex.cli.command.ListCommand;
import cz.martim12.noteindex.cli.command.SearchCommand;
import cz.martim12.noteindex.cli.command.ShowCommand;
import cz.martim12.noteindex.cli.command.VersionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CliCommandParserTest {

    private Path defaultDatabaseFile;
    private CliCommandParser parser;

    @BeforeEach
    void setUp() {
        defaultDatabaseFile = Path.of(
                "build",
                "default-noteindex.db"
        ).toAbsolutePath().normalize();

        parser = new CliCommandParser(
                defaultDatabaseFile
        );
    }

    @Test
    void treatsEmptyInvocationAsGeneralHelp() {
        CliArguments parsed =
                parser.parse(new String[0]);

        assertEquals(
                defaultDatabaseFile,
                parsed.databaseFile()
        );

        assertEquals(
                HelpCommand.general(),
                parsed.command()
        );
    }

    @Test
    void parsesCustomDatabaseBeforeCommand() {
        CliArguments parsed = parser.parse(
                new String[]{
                        "--database",
                        "./data/custom.db",
                        "list"
                }
        );

        assertEquals(
                Path.of("./data/custom.db")
                        .toAbsolutePath()
                        .normalize(),
                parsed.databaseFile()
        );

        assertInstanceOf(
                ListCommand.class,
                parsed.command()
        );
    }

    @Test
    void supportsShortDatabaseOption() {
        CliArguments parsed = parser.parse(
                new String[]{
                        "-d",
                        "custom.db",
                        "formats"
                }
        );

        assertInstanceOf(
                FormatsCommand.class,
                parsed.command()
        );
    }

    @Test
    void parsesImportCommand() {
        CliArguments parsed = parser.parse(
                new String[]{
                        "import",
                        "./notes/java.txt"
                }
        );

        ImportCommand command =
                assertInstanceOf(
                        ImportCommand.class,
                        parsed.command()
                );

        assertEquals(
                Path.of("./notes/java.txt"),
                command.source()
        );
    }

    @Test
    void parsesSearchWithDefaultLimitAndJoinedQuery() {
        CliArguments parsed = parser.parse(
                new String[]{
                        "search",
                        "java",
                        "virtual",
                        "machine"
                }
        );

        SearchCommand command =
                assertInstanceOf(
                        SearchCommand.class,
                        parsed.command()
                );

        assertEquals(
                "java virtual machine",
                command.query()
        );

        assertEquals(
                CliCommandParser.DEFAULT_SEARCH_LIMIT,
                command.limit()
        );
    }

    @Test
    void parsesExplicitSearchLimit() {
        CliArguments parsed = parser.parse(
                new String[]{
                        "search",
                        "java",
                        "--limit",
                        "5",
                        "\"virtual machine\""
                }
        );

        SearchCommand command =
                assertInstanceOf(
                        SearchCommand.class,
                        parsed.command()
                );

        assertEquals(
                "java \"virtual machine\"",
                command.query()
        );

        assertEquals(5, command.limit());
    }

    @Test
    void parsesDocumentIdCommands() {
        ShowCommand show =
                assertInstanceOf(
                        ShowCommand.class,
                        parser.parse(
                                new String[]{"show", "12"}
                        ).command()
                );

        DeleteCommand delete =
                assertInstanceOf(
                        DeleteCommand.class,
                        parser.parse(
                                new String[]{"delete", "25"}
                        ).command()
                );

        assertEquals(12, show.documentId());
        assertEquals(25, delete.documentId());
    }

    @Test
    void parsesGeneralAndCommandSpecificHelp() {
        assertEquals(
                HelpCommand.general(),
                parser.parse(
                        new String[]{"help"}
                ).command()
        );

        assertEquals(
                new HelpCommand(
                        Optional.of("search")
                ),
                parser.parse(
                        new String[]{"help", "search"}
                ).command()
        );

        assertEquals(
                new HelpCommand(
                        Optional.of("import")
                ),
                parser.parse(
                        new String[]{
                                "import",
                                "--help"
                        }
                ).command()
        );
    }

    @Test
    void parsesVersionAliases() {
        assertInstanceOf(
                VersionCommand.class,
                parser.parse(
                        new String[]{"version"}
                ).command()
        );

        assertInstanceOf(
                VersionCommand.class,
                parser.parse(
                        new String[]{"--version"}
                ).command()
        );
    }

    @Test
    void rejectsUnknownCommandAndHelpTopic() {
        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{"unknown"}
                )
        );

        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{"help", "unknown"}
                )
        );
    }

    @Test
    void rejectsMissingOrDuplicateDatabaseOptions() {
        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{"--database"}
                )
        );

        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{
                                "--database",
                                "first.db",
                                "-d",
                                "second.db",
                                "list"
                        }
                )
        );
    }

    @Test
    void rejectsInvalidDocumentIds() {
        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{"show", "abc"}
                )
        );

        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{"delete", "0"}
                )
        );

        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{"show", "-5"}
                )
        );
    }

    @Test
    void rejectsMissingAndInvalidSearchArguments() {
        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{"search"}
                )
        );

        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{
                                "search",
                                "--limit"
                        }
                )
        );

        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{
                                "search",
                                "--limit",
                                "zero",
                                "java"
                        }
                )
        );

        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{
                                "search",
                                "--limit",
                                "5",
                                "-n",
                                "10",
                                "java"
                        }
                )
        );
    }

    @Test
    void rejectsUnexpectedArgumentsAndOptions() {
        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{"list", "extra"}
                )
        );

        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{
                                "list",
                                "--database",
                                "other.db"
                        }
                )
        );

        assertThrows(
                CliUsageException.class,
                () -> parser.parse(
                        new String[]{"--unknown", "list"}
                )
        );
    }
}