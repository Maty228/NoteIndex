package cz.martim12.noteindex.cli;


import cz.martim12.noteindex.application.api.NoteIndexApplications;
import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.cli.command.*;

import cz.martim12.noteindex.cli.error.CliErrorHandler;
import cz.martim12.noteindex.cli.output.CliOutputFormatter;
import cz.martim12.noteindex.cli.parsing.CliArguments;
import cz.martim12.noteindex.cli.parsing.CliCommandParser;
import cz.martim12.noteindex.cli.parsing.CliUsageException;
import cz.martim12.noteindex.cli.runtime.CliDatabasePaths;
import cz.martim12.noteindex.cli.runtime.NoteIndexServiceFactory;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.SearchQuery;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Testable command-line application.

 * The main method delegates here so commands can be tested without
 * starting another JVM or replacing System.out and System.err.
 */
public final class CliApplication {

    public static final String VERSION = "1.0";

    private final NoteIndexServiceFactory serviceFactory;
    private final CliCommandParser commandParser;
    private final CliErrorHandler errorHandler;
    private final String version;

    public CliApplication() {
        this(NoteIndexApplications::open, VERSION, new CliCommandParser(CliDatabasePaths.defaultDatabaseFile()));
    }

    public CliApplication(NoteIndexServiceFactory serviceFactory, String version) {
        this(serviceFactory, version, new CliCommandParser(CliDatabasePaths.defaultDatabaseFile()));
    }

    public CliApplication(NoteIndexServiceFactory serviceFactory, String version, CliCommandParser commandParser) {
        this(serviceFactory, version, commandParser, new CliErrorHandler());
    }

    /*
     * Package-private constructor for deterministic tests with a
     * temporary default database path.
     */
    public CliApplication(NoteIndexServiceFactory serviceFactory, String version, CliCommandParser commandParser, CliErrorHandler errorHandler) {
        this.serviceFactory = Objects.requireNonNull(serviceFactory, "Service factory must not be null");
        this.version = requireNonBlank(version, "CLI version");
        this.commandParser = Objects.requireNonNull(commandParser, "Command parser must not be null");
        this.errorHandler = Objects.requireNonNull(errorHandler, "Error handler must not be null");
    }

    /**
     * Executes one CLI invocation.
     *
     * @return process exit code
     */
    public int run(String[] args, PrintStream standardOutput, PrintStream errorOutput) {
        Objects.requireNonNull(args, "Arguments must not be null");
        Objects.requireNonNull(standardOutput, "Standard output must not be null");
        Objects.requireNonNull(errorOutput, "Error output must not be null");

        final CliArguments parsedArgs;

        try {
            parsedArgs = commandParser.parse(args);
        } catch (CliUsageException exception) {
            return errorHandler.handleUsageError(exception, errorOutput);
        }

        return execute(parsedArgs, standardOutput, errorOutput);
    }

    private int execute(CliArguments args, PrintStream standardOutput, PrintStream errorOutput) {
        return switch (args.command()) {
            case HelpCommand helpCommand -> {
                printHelp(standardOutput, helpCommand);
                yield CliExitCode.SUCCESS;
            }

            case VersionCommand _ -> {
                CliOutputFormatter.printVersion(standardOutput, version);
                yield CliExitCode.SUCCESS;
            }

            case FormatsCommand formatsCommand -> executeServiceCommand(args.databaseFile(), formatsCommand, standardOutput, errorOutput);

            case ListCommand listCommand -> executeServiceCommand(args.databaseFile(), listCommand, standardOutput, errorOutput);

            case ShowCommand showCommand -> executeServiceCommand(args.databaseFile(), showCommand, standardOutput, errorOutput);

            case ImportCommand importCommand -> executeServiceCommand(args.databaseFile(), importCommand, standardOutput, errorOutput);

            case SearchCommand searchCommand -> executeServiceCommand(args.databaseFile(), searchCommand, standardOutput, errorOutput);

            case DeleteCommand deleteCommand -> executeServiceCommand(args.databaseFile(), deleteCommand, standardOutput, errorOutput);
        };
    }

    private int executeServiceCommand(Path databaseFile, CliCommand command, PrintStream standardOutput, PrintStream errorOutput) {
        try {
            createDatabaseParentDirectory(databaseFile);

            try (NoteIndexService service = Objects.requireNonNull(serviceFactory.open(databaseFile), "Service factory returned null")) {
                return executeWithService(service, command, standardOutput, errorOutput);
            }
        } catch (IOException | RuntimeException exception) {
            return errorHandler.handleOperationError(exception, errorOutput);
        }
    }

    private int executeWithService(NoteIndexService service, CliCommand command, PrintStream standardOutput, PrintStream errorOutput) {
        return switch (command) {
            case FormatsCommand _ -> {
                CliOutputFormatter.printFormats(standardOutput, service.supportedImportExtensions());
                yield CliExitCode.SUCCESS;
            }

            case ListCommand _ -> {
                CliOutputFormatter.printDocumentList(standardOutput, service.listDocuments());
                yield CliExitCode.SUCCESS;
            }

            case ShowCommand showCommand ->
                service.findDocument(showCommand.documentId())
                        .map(document -> {
                            CliOutputFormatter.printDocument(standardOutput, document);
                            return CliExitCode.SUCCESS;
                        })
                        .orElseGet(() -> {
                            CliOutputFormatter.printMissingDocument(errorOutput, showCommand.documentId());
                            return CliExitCode.FAILURE;
                        });

            case ImportCommand importCommand -> {
                Document importedDocument = service.importFile(importCommand.source());

                CliOutputFormatter.printImportedDocument(standardOutput, importedDocument);

                yield CliExitCode.SUCCESS;
            }

            case SearchCommand searchCommand -> {
                var results = service.search(new SearchQuery(searchCommand.query()), searchCommand.limit());
                CliOutputFormatter.printSearchResults(standardOutput, results);
                yield CliExitCode.SUCCESS;
            }

            case DeleteCommand deleteCommand -> {
                boolean deleted = service.deleteDocument(deleteCommand.documentId());

                if (deleted) {
                    CliOutputFormatter.printDeletedDocument(standardOutput, deleteCommand.documentId());
                    yield CliExitCode.SUCCESS;
                }

                CliOutputFormatter.printMissingDocument(errorOutput, deleteCommand.documentId());
                yield CliExitCode.FAILURE;
            }


            default -> throw new IllegalStateException("Command does not use the application service: " + command.getClass().getSimpleName());
        };
    }

    private void printHelp(PrintStream output, HelpCommand command) {
        command.topic().ifPresentOrElse(
                topic -> CliOutputFormatter.printCommandHelp(output, topic),
                () -> CliOutputFormatter.printGeneralHelp(output)
        );
    }


    private static void createDatabaseParentDirectory(Path databaseFile) throws IOException {
        Path parent = databaseFile.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }
    }


    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
