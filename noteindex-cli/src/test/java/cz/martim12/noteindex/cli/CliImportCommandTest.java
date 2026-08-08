package cz.martim12.noteindex.cli;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.cli.parsing.CliCommandParser;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliImportCommandTest {

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
    void importsDocumentAndPrintsResult() {
        Path source =
                temporaryDirectory.resolve(
                        "java-runtime-notes.txt"
                );

        StubNoteIndexService service =
                new StubNoteIndexService();

        service.importedDocument = new Document(
                12,
                "java-runtime-notes.txt",
                source.toAbsolutePath()
                        .normalize()
                        .toUri()
                        .toString(),
                "text/plain",
                "Java virtual machine.",
                "Java virtual machine.",
                Instant.parse(
                        "2026-08-05T15:00:00Z"
                )
        );

        int exitCode = cli(service).run(
                new String[]{
                        "import",
                        source.toString()
                },
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(
                source,
                service.importedSource
        );

        assertTrue(
                standardOutput().contains(
                        "Imported document 12: "
                                + "java-runtime-notes.txt"
                )
        );

        assertTrue(
                standardOutput().contains(
                        "Format: text/plain"
                )
        );

        assertTrue(
                standardOutput().contains(
                        "Source: "
                                + service.importedDocument
                                .sourceUri()
                )
        );

        assertTrue(errorOutput().isEmpty());
        assertTrue(service.closed);
    }

    @Test
    void usesSelectedDatabaseAndCreatesItsDirectory() {
        Path source =
                temporaryDirectory.resolve("notes.txt");

        Path databaseFile =
                temporaryDirectory
                        .resolve("nested")
                        .resolve("database")
                        .resolve("noteindex.db");

        StubNoteIndexService service =
                successfulService(source);

        Path[] openedDatabase = new Path[1];

        CliApplication cli = new CliApplication(
                databasePath -> {
                    openedDatabase[0] = databasePath;
                    return service;
                },
                "1.0-test",
                new CliCommandParser(
                        temporaryDirectory.resolve(
                                "default.db"
                        )
                )
        );

        int exitCode = cli.run(
                new String[]{
                        "--database",
                        databaseFile.toString(),
                        "import",
                        source.toString()
                },
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(
                databaseFile.toAbsolutePath().normalize(),
                openedDatabase[0]
        );

        assertTrue(
                Files.isDirectory(
                        databaseFile.getParent()
                )
        );
    }

    @Test
    void reportsImportFailureAndClosesService() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.importFailure =
                new IllegalStateException(
                        "Could not import document"
                );

        int exitCode = cli(service).run(
                new String[]{
                        "import",
                        "notes.txt"
                },
                output,
                error
        );

        assertEquals(
                CliExitCode.FAILURE,
                exitCode
        );

        assertTrue(standardOutput().isEmpty());

        assertTrue(
                errorOutput().contains(
                        "Could not import document"
                )
        );

        assertTrue(service.closed);
    }

    @Test
    void rejectsMissingImportPathWithoutOpeningService() {
        AtomicInteger openingCount =
                new AtomicInteger();

        CliApplication cli = new CliApplication(
                databaseFile -> {
                    openingCount.incrementAndGet();

                    throw new AssertionError(
                            "Invalid arguments must not open service"
                    );
                },
                "1.0-test",
                new CliCommandParser(
                        temporaryDirectory.resolve(
                                "default.db"
                        )
                )
        );

        int exitCode = cli.run(
                new String[]{"import"},
                output,
                error
        );

        assertEquals(
                CliExitCode.USAGE_ERROR,
                exitCode
        );

        assertEquals(0, openingCount.get());

        assertTrue(
                errorOutput().contains(
                        "Usage: noteindex import <file>"
                )
        );
    }

    @Test
    void importHelpDoesNotOpenService() {
        AtomicInteger openingCount =
                new AtomicInteger();

        CliApplication cli = new CliApplication(
                databaseFile -> {
                    openingCount.incrementAndGet();

                    throw new AssertionError(
                            "Help must not open service"
                    );
                },
                "1.0-test",
                new CliCommandParser(
                        temporaryDirectory.resolve(
                                "default.db"
                        )
                )
        );

        int exitCode = cli.run(
                new String[]{
                        "import",
                        "--help"
                },
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(0, openingCount.get());

        assertTrue(
                standardOutput().contains(
                        "noteindex import <file>"
                )
        );
    }

    private CliApplication cli(
            StubNoteIndexService service
    ) {
        return new CliApplication(
                databaseFile -> service,
                "1.0-test",
                new CliCommandParser(
                        temporaryDirectory.resolve(
                                "default.db"
                        )
                )
        );
    }

    private StubNoteIndexService successfulService(
            Path source
    ) {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.importedDocument = new Document(
                1,
                source.getFileName().toString(),
                source.toAbsolutePath()
                        .normalize()
                        .toUri()
                        .toString(),
                "text/plain",
                "Content",
                "Content",
                Instant.parse(
                        "2026-08-05T15:00:00Z"
                )
        );

        return service;
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

    private static final class StubNoteIndexService
            implements NoteIndexService {

        private Document importedDocument;
        private RuntimeException importFailure;
        private Path importedSource;
        private boolean closed;

        @Override
        public Document importFile(Path source) {
            importedSource = source;

            if (importFailure != null) {
                throw importFailure;
            }

            return importedDocument;
        }

        @Override
        public List<SearchResult> search(
                SearchQuery query,
                int limit
        ) {
            throw new AssertionError(
                    "Search must not be called"
            );
        }

        @Override
        public List<DocumentSummary> listDocuments() {
            throw new AssertionError(
                    "List must not be called"
            );
        }

        @Override
        public Optional<Document> findDocument(
                long documentId
        ) {
            throw new AssertionError(
                    "Find must not be called"
            );
        }

        @Override
        public boolean deleteDocument(long documentId) {
            throw new AssertionError(
                    "Delete must not be called"
            );
        }

        @Override
        public boolean renameDocument(
                long documentId,
                String newTitle
        ) {
            throw new AssertionError(
                    "Rename must not be called"
            );
        }

        @Override
        public Set<String> supportedImportExtensions() {
            throw new AssertionError(
                    "Formats must not be called"
            );
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}