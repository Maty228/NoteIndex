package cz.martim12.noteindex.cli;

import cz.martim12.noteindex.application.api.NoteIndexApplications;
import cz.martim12.noteindex.cli.parsing.CliCommandParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliLifecycleIntegrationTest {

    private static final Pattern IMPORTED_DOCUMENT_PATTERN =
            Pattern.compile(
                    "Imported document (\\d+):"
            );

    @TempDir
    Path temporaryDirectory;

    private ByteArrayOutputStream outputBytes;
    private ByteArrayOutputStream errorBytes;
    private PrintStream output;
    private PrintStream error;

    @BeforeEach
    void setUp() {
        outputBytes = new ByteArrayOutputStream();
        errorBytes = new ByteArrayOutputStream();

        output = new PrintStream(
                outputBytes,
                true,
                StandardCharsets.UTF_8
        );

        error = new PrintStream(
                errorBytes,
                true,
                StandardCharsets.UTF_8
        );
    }

    @Test
    void executesCompleteLifecycleAcrossRestarts()
            throws IOException {

        Path databaseFile =
                temporaryDirectory.resolve(
                        "database"
                ).resolve(
                        "noteindex.db"
                );

        Path sourceFile =
                temporaryDirectory.resolve(
                        "java-runtime-notes.txt"
                );

        String sourceContent = """
                Java virtual machine executes bytecode.
                Garbage collection manages runtime memory.
                SQLite stores persistent application data.
                """.strip();

        Files.writeString(
                sourceFile,
                sourceContent,
                StandardCharsets.UTF_8
        );

        /*
         * First CLI/application lifecycle.
         */
        CliApplication firstCli =
                createCli(databaseFile);

        assertSuccessful(
                firstCli,
                "formats"
        );

        assertTrue(
                standardOutput().contains(
                        "Supported import extensions:"
                )
        );

        assertTrue(
                standardOutput().lines()
                        .anyMatch(line ->
                                line.strip().equals("txt")
                        )
        );

        assertSuccessful(
                firstCli,
                "import",
                sourceFile.toString()
        );

        String importOutput = standardOutput();

        assertTrue(
                importOutput.contains(
                        "Format: text/plain"
                )
        );

        long documentId =
                extractImportedDocumentId(
                        importOutput
                );

        assertTrue(documentId > 0);
        assertTrue(Files.exists(databaseFile));

        assertSuccessful(
                firstCli,
                "list"
        );

        String listOutput = standardOutput();

        assertTrue(listOutput.contains("ID"));
        assertTrue(listOutput.contains("FORMAT"));
        assertTrue(listOutput.contains("TITLE"));

        assertTrue(
                listOutput.contains(
                        Long.toString(documentId)
                )
        );

        assertTrue(
                listOutput.contains("text/plain")
        );

        assertSuccessful(
                firstCli,
                "show",
                Long.toString(documentId)
        );

        String showOutput = standardOutput();

        assertTrue(
                showOutput.contains(
                        "ID:        " + documentId
                )
        );

        assertTrue(
                showOutput.contains(sourceContent)
        );

        assertSuccessful(
                firstCli,
                "search",
                "--limit",
                "5",
                "\"virtual machine\""
        );

        String initialSearchOutput =
                standardOutput();

        assertTrue(
                initialSearchOutput.contains(
                        "1 result(s)"
                )
        );

        assertTrue(
                initialSearchOutput.contains(
                        "ID: " + documentId
                )
        );

        assertTrue(
                initialSearchOutput
                        .toLowerCase()
                        .contains("virtual machine")
        );

        /*
         * New CLI instance and new application service.
         *
         * The search index is in memory, so successful search here
         * proves that documents were loaded from SQLite and indexed
         * again during application startup.
         */
        CliApplication restartedCli =
                createCli(databaseFile);

        assertSuccessful(
                restartedCli,
                "search",
                "\"virtual machine\""
        );

        String rebuiltSearchOutput =
                standardOutput();

        assertTrue(
                rebuiltSearchOutput.contains(
                        "1 result(s)"
                )
        );

        assertTrue(
                rebuiltSearchOutput.contains(
                        "ID: " + documentId
                )
        );

        assertSuccessful(
                restartedCli,
                "delete",
                Long.toString(documentId)
        );

        assertEquals(
                "Deleted document "
                        + documentId
                        + ".",
                standardOutput().strip()
        );

        assertSuccessful(
                restartedCli,
                "list"
        );

        assertEquals(
                "No documents imported.",
                standardOutput().strip()
        );

        assertSuccessful(
                restartedCli,
                "search",
                "java"
        );

        assertEquals(
                "No matching documents.",
                standardOutput().strip()
        );

        /*
         * Third lifecycle verifies that deletion was persisted in
         * SQLite and the document is not restored during rebuilding.
         */
        CliApplication afterDeletionCli =
                createCli(databaseFile);

        assertSuccessful(
                afterDeletionCli,
                "list"
        );

        assertEquals(
                "No documents imported.",
                standardOutput().strip()
        );

        assertSuccessful(
                afterDeletionCli,
                "search",
                "\"virtual machine\""
        );

        assertEquals(
                "No matching documents.",
                standardOutput().strip()
        );
    }

    private CliApplication createCli(
            Path databaseFile
    ) {
        return new CliApplication(
                NoteIndexApplications::open,
                "1.0-test",
                new CliCommandParser(databaseFile)
        );
    }

    private void assertSuccessful(
            CliApplication cli,
            String... arguments
    ) {
        resetOutput();

        int exitCode = cli.run(
                arguments,
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode,
                () -> """
                        CLI command failed.
                        Standard output:
                        %s
                        Error output:
                        %s
                        """.formatted(
                        standardOutput(),
                        errorOutput()
                )
        );

        assertTrue(
                errorOutput().isEmpty(),
                () -> "Unexpected CLI error output: "
                        + errorOutput()
        );
    }

    private long extractImportedDocumentId(
            String importOutput
    ) {
        Matcher matcher =
                IMPORTED_DOCUMENT_PATTERN.matcher(
                        importOutput
                );

        assertTrue(
                matcher.find(),
                () -> "Could not find imported document ID in:\n"
                        + importOutput
        );

        return Long.parseLong(
                matcher.group(1)
        );
    }

    private void resetOutput() {
        outputBytes.reset();
        errorBytes.reset();
    }

    private String standardOutput() {
        return outputBytes.toString(
                StandardCharsets.UTF_8
        );
    }

    private String errorOutput() {
        return errorBytes.toString(
                StandardCharsets.UTF_8
        );
    }
}