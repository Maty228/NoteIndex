package cz.martim12.noteindex.cli.output;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchResult;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Produces human-readable CLI output.
 */
public final class CliOutputFormatter {

    private CliOutputFormatter() {}

    /**
     * Prints general CLI usage information.
     *
     * @param output destination output stream
     */
    public static void printGeneralHelp(PrintStream output) {
        requireOutput(output);

        output.println("""
                NoteIndex command-line interface
                
                Usage:
                  noteindex [--database <file>] <command> [arguments]
                
                Commands:
                  import <file>              Import a document
                  search [--limit N] <query> Search indexed documents
                  list                       List stored documents
                  show <document-id>         Show a complete document
                  delete <document-id>       Delete a document
                  formats                    List supported import formats
                  help [command]             Show help
                  version                    Show the NoteIndex version
                
                Global options:
                  -d, --database <file>  Use a specific SQLite database
                  -h, --help             Show general help
                  -V, --version          Show the NoteIndex version
                """.stripTrailing());
    }

    /**
     * Prints help information for a specific command.
     *
     * @param output destination output stream
     * @param command command name
     */
    public static void printCommandHelp(PrintStream output, String command) {
        requireOutput(output);
        Objects.requireNonNull(command, "Command name must not be null");

        String help = switch (command) {
            case "import" -> """
                    Usage:
                      noteindex import <file>
                    
                    Imports one supported document file.
                    """;
            case "search" -> """
                    Usage:
                      noteindex search [--limit N] <query>
                    
                    Searches indexed documents. Quoted text is treated as an exact phrase.
                    
                    Options:
                      -n, --limit N  Maximum number of results
                    """;
            case "list" -> """
                    Usage:
                      noteindex list
                    
                    Lists all imported documents.
                    """;
            case "show" -> """
                    Usage:
                      noteindex show <document-id>
            
                    Displays one document.
                    """;
            case "formats" -> """
                    Usage:
                      noteindex formats
                    
                    Lists supported importer file extensions.
                    """;
            case "help" -> """
                    Usage:
                      noteindex help [command]
                    
                    Shows general or command-specific help.
                    """;
            case "version" -> """
                    Usage:
                      noteindex version
                    
                    Shows the NoteIndex version.
                    """;

            case "delete" -> """
                    Usage:
                      noteindex delete <document-id>
            
                    Deletes one document.
                    """;
            default -> throw new IllegalArgumentException("Unknown help command: " + command);
        };

        output.println(help.stripTrailing());
    }

    /**
     * Prints the application version.
     *
     * @param output destination output stream
     * @param version version string
     */
    public static void printVersion(PrintStream output, String version) {
        requireOutput(output);
        output.println("NoteIndex " + version);
    }

    /**
     * Prints supported document import formats.
     *
     * @param output destination output stream
     * @param extensions supported extensions
     */
    public static void printFormats(PrintStream output, Set<String> extensions) {
        requireOutput(output);
        Objects.requireNonNull(extensions, "Extensions must not be null");

        if (extensions.isEmpty()) {
            output.println("No supported import formats");
            return;
        }

        output.println("Supported import extensions:");

        extensions.stream()
                .sorted()
                .forEach(extension -> output.println("  " + extension));
    }

    /**
     * Prints information about an imported document.
     *
     * @param output destination output stream
     * @param document imported document
     */
    public static void printImportedDocument(PrintStream output, Document document) {
        requireOutput(output);
        Objects.requireNonNull(document, "Imported document must not be null");

        output.println("Imported document " + document.id() + ": " + document.title());

        output.println("Format: " + document.format());
        output.println("Source: " + document.sourceUri());
    }

    /**
     * Prints a list of stored document summaries.
     *
     * @param output destination output stream
     * @param documents documents to display
     */
    public static void printDocumentList(PrintStream output, List<DocumentSummary> documents) {
        requireOutput(output);
        Objects.requireNonNull(documents, "Documents must not be null");

        if (documents.isEmpty()) {
            output.println("No documents imported.");
            return;
        }

        output.printf(
                "%-8s %-14s %-25s %s%n",
                "ID", "FORMAT", "IMPORTED", "TITLE"
        );

        for (DocumentSummary document : documents) {
            output.printf(
                    "%-8d %-14s %-25s %s%n",
                    document.id(), document.format(), document.importedAt(), document.title()
            );
        }
    }

    /**
     * Prints complete information and content of a document.
     *
     * @param output destination output stream
     * @param document document to display
     */
    public static void printDocument(PrintStream output, Document document) {
        requireOutput(output);
        Objects.requireNonNull(document, "Document must not be null");

        output.println("ID:        " + document.id());
        output.println("Title:     " + document.title());
        output.println("Format:    " + document.format());
        output.println("Source:    " + document.sourceUri());
        output.println("Imported:  " + document.importedAt());
        output.println();
        output.println(document.originalContent());
    }

    /**
     * Prints ranked search results.
     *
     * @param output destination output stream
     * @param results search results to display
     */
    public static void printSearchResults(PrintStream output, List<SearchResult> results) {
        requireOutput(output);
        Objects.requireNonNull(results, "Search results must not be null");

        if (results.isEmpty()) {
            output.println("No matching documents.");
            return;
        }

        output.println(results.size() + " result(s)");

        for (int index = 0; index < results.size(); index++) {
            SearchResult result = Objects.requireNonNull(results.get(index), "Search result must not be null");

            output.println();

            output.println((index + 1) + ". " + result.document().title());

            output.printf(
                    Locale.ROOT,
                    "   ID: %d | Format: %s | Score: %.3f%n",
                    result.document().id(),
                    result.document().format(),
                    result.score()
            );

            if (!result.snippet().isBlank()) {
                output.println("   " + result.snippet());
            }
        }
    }

    /**
     * Prints confirmation of document deletion.
     *
     * @param output destination output stream
     * @param documentId deleted document identifier
     */
    public static void printDeletedDocument(PrintStream output, long documentId) {
        requireOutput(output);
        output.println("Deleted document " + documentId +".");
    }

    /**
     * Prints an error message for a missing document.
     *
     * @param errorOutput destination error stream
     * @param documentId missing document identifier
     */
    public static void printMissingDocument(PrintStream errorOutput, long documentId) {
        requireOutput(errorOutput);
        printOperationError(errorOutput, "Document " + documentId + " does not exist.");
    }


    /**
     * Prints a CLI usage error message and a hint for displaying help.
     *
     * @param output destination error stream
     * @param message error message
     */
    public static void printUsageError(PrintStream output, String message) {
        requireOutput(output);
        output.println("Error: " + message);
        output.println("Run 'noteindex help' for usage.");
    }

    /**
     * Prints a general CLI operation error message.
     *
     * @param output destination error stream
     * @param message error message
     */
    public static void printOperationError(PrintStream output, String message) {
        requireOutput(output);
        output.println("Error: " + message);
    }

    private static void requireOutput(PrintStream output) {
        Objects.requireNonNull(output, "Output stream must not be null");
    }
}
