package cz.martim12.noteindex.cli.parsing;

import cz.martim12.noteindex.cli.command.CliCommand;
import cz.martim12.noteindex.cli.command.DeleteCommand;
import cz.martim12.noteindex.cli.command.FormatsCommand;
import cz.martim12.noteindex.cli.command.HelpCommand;
import cz.martim12.noteindex.cli.command.ImportCommand;
import cz.martim12.noteindex.cli.command.ListCommand;
import cz.martim12.noteindex.cli.command.SearchCommand;
import cz.martim12.noteindex.cli.command.ShowCommand;
import cz.martim12.noteindex.cli.command.VersionCommand;

import java.nio.file.Path;
import java.util.*;

/**
 * Parses global options, command names and command-specific
 * arguments.
 */
public final class CliCommandParser {
    /**
     * Default maximum number of search results.
     */
    public static final int DEFAULT_SEARCH_LIMIT = 10;

    private static final Set<String> COMMAND_NAMES = Set.of(
            "import", "search", "list", "show", "delete", "formats", "version", "help"
    );

    private final Path defaultDatabaseFile;


    /**
     * Creates a CLI command parser.
     *
     * @param defaultDatabaseFile default database location
     */
    public CliCommandParser(Path defaultDatabaseFile) {
        this.defaultDatabaseFile = Objects.requireNonNull(defaultDatabaseFile, "Default database file must not be null");
    }

    /**
     * Parses command-line arguments into a CLI invocation model.
     *
     * @param args command-line arguments
     * @return parsed CLI arguments
     * @throws CliUsageException if arguments contain invalid syntax
     */
    public CliArguments parse(String[] args) {
        Objects.requireNonNull(args, "Arguments must not be null");

        validateArgumentElements(args);

        if (args.length == 0) {
            return result(defaultDatabaseFile, HelpCommand.general());
        }

        Path databaseFile = defaultDatabaseFile;
        boolean databaseSpecified = false;
        int position = 0;

        while (position < args.length) {
            String arg = args[position];

            switch (arg) {
                case "--database", "-d" -> {
                    if (databaseSpecified) {
                        throw new CliUsageException("Database file specified more than once");
                    }

                    position++;

                    if (position >= args.length) {
                        throw new CliUsageException("Missing path after " + arg);
                    }

                    databaseFile = Path.of(args[position]);
                    databaseSpecified = true;
                    position++;
                    continue;
                }
                case "--help", "-h" -> {
                    requireNoRemainingArguments(args, position + 1, arg);

                    return result(databaseFile, HelpCommand.general());
                }
                case "--version", "-V" -> {
                    requireNoRemainingArguments(args, position + 1, arg);

                    return result(databaseFile, new VersionCommand());
                }
            }

            if (arg.startsWith("-")) {
                throw new CliUsageException("Unknown global option: " + arg);
            }

            break;
        }

        if (position >= args.length) {
            return result(databaseFile, HelpCommand.general());
        }

        String commandName = args[position];
        position++;

        List<String> commandArgs = copyRemaining(args, position);

        CliCommand command = parseCommand(commandName, commandArgs);

        return result(databaseFile, command);
    }

    private CliCommand parseCommand(String commandName, List<String> args) {
        if (args.size() == 1 && isHelpOption(args.getFirst()) && !commandName.equals("help")) {
            requireKnownCommand(commandName);

            return HelpCommand.forCommand(commandName);
        }

        return switch (commandName) {
            case "import" -> parseImport(args);
            case "search" -> parseSearch(args);
            case "list" -> {
                requireNoArguments("list", args);
                yield new ListCommand();
            }
            case "show" -> new ShowCommand(parseDocumentId("show", args));
            case "delete" -> new DeleteCommand(parseDocumentId("delete", args));
            case "formats" -> {
                requireNoArguments("formats", args);
                yield new FormatsCommand();
            }
            case "help" -> parseHelp(args);
            case "version" -> {
                requireNoArguments("version", args);
                yield new VersionCommand();
            }
            default -> throw new CliUsageException("Unknown command: " + commandName);
        };
    }

    private static ImportCommand parseImport(List<String> args) {
        if (args.size() != 1) {
            throw new CliUsageException("Usage: noteindex import <file>");
        }

        return new ImportCommand(Path.of(args.getFirst()));
    }

    private static SearchCommand parseSearch(List<String> args) {
        int limit = DEFAULT_SEARCH_LIMIT;
        boolean limitSpecified = false;
        List<String> queryParts = new ArrayList<>();

        for (int pos = 0; pos < args.size(); pos++) {
            String arg = args.get(pos);

            if (arg.equals("--limit") || arg.equals("-n")) {
                if (limitSpecified) {
                    throw new CliUsageException("Limit specified more than once");
                }

                pos++;


                if (pos >= args.size()) {
                    throw new CliUsageException("Missing limit after " + arg);
                }

                limit = parsePositiveInteger(args.get(pos), "Search limit");
                limitSpecified = true;
                continue;
            }

            if (arg.startsWith("-")) {
                throw new CliUsageException("Unknown option: " + arg);
            }

            queryParts.add(arg);
        }

        String query = String.join(" ", queryParts).trim();

        if (query.isBlank()) {
            throw new CliUsageException("Usage: noteindex search [--limit N] <query>");
        }

        return new SearchCommand(query, limit);
    }

    private static long parseDocumentId(String commandName, List<String> args) {
        if (args.size() != 1) {
            throw new CliUsageException("Usage: noteindex " + commandName + " <document id>");
        }
        String value = args.getFirst();

        try {
            long documentId = Long.parseLong(value);

            if (documentId <= 0) {
                throw new CliUsageException("Document ID must be positive: " + value);
            }

            return documentId;
        } catch (NumberFormatException exception) {
            throw new CliUsageException("Invalid document ID: " + value);
        }
    }

    private static HelpCommand parseHelp(List<String> args) {
        if (args.isEmpty()) {
            return HelpCommand.general();
        }

        if (args.size() != 1) {
            throw new CliUsageException("Usage: noteindex help [command]");
        }

        String topic = args.getFirst();
        requireKnownCommand(topic);

        return HelpCommand.forCommand(topic);
    }

    private static void requireKnownCommand(String commandName) {
        if (!COMMAND_NAMES.contains(commandName)) {
            throw new CliUsageException("Unknown help topic: " + commandName);
        }
    }

    private static void requireNoArguments(String commandName, List<String> args) {
        if (!args.isEmpty()) {
            throw new CliUsageException("Command '" + commandName + "' does not accept arguments");
        }
    }

    private static int parsePositiveInteger(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);

            if (parsed <= 0) {
                throw new CliUsageException(name + " must be positive: " + value);
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new CliUsageException("Invalid " + name.toLowerCase() + ": " + value);
        }
    }

    private static boolean isHelpOption(String value) {
        return value.equals("--help") || value.equals("-h");
    }

    private static List<String> copyRemaining(String[] args, int startingPosition) {
        List<String> remaining = new ArrayList<>(args.length - startingPosition);

        remaining.addAll(Arrays.asList(args).subList(startingPosition, args.length));

        return List.copyOf(remaining);
    }

    private static void validateArgumentElements(String[] args) {
        for (int pos = 0; pos < args.length; pos++) {
            if (args[pos] == null) {
                throw new CliUsageException("Argument at position " + pos + " must not be null");
            }
        }
    }

    private static void requireNoRemainingArguments(String[] args, int startingPosition, String option) {
        if (startingPosition < args.length) {
            throw new CliUsageException(option + " does not accept arguments");
        }
    }

    private static CliArguments result(Path databaseFile, CliCommand command) {
        return new CliArguments(databaseFile, command);
    }
}
