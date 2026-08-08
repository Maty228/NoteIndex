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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliBrowsingCommandsTest {

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
    void listsSupportedFormatsAlphabetically() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.extensions = Set.of("txt", "md");

        int exitCode = cli(service).run(
                new String[]{"formats"},
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(
                List.of(
                        "Supported import extensions:",
                        "  md",
                        "  txt"
                ),
                standardOutput().lines().toList()
        );

        assertTrue(errorOutput().isEmpty());
        assertTrue(service.closed);
    }

    @Test
    void printsEmptyDocumentList() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        int exitCode = cli(service).run(
                new String[]{"list"},
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(
                "No documents imported.",
                standardOutput().strip()
        );

        assertTrue(service.closed);
    }

    @Test
    void printsDocumentSummaryTable() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.summaries = List.of(
                new DocumentSummary(
                        12,
                        "Java Virtual Machine",
                        "text/plain",
                        Instant.parse(
                                "2026-08-05T12:00:00Z"
                        )
                ),
                new DocumentSummary(
                        7,
                        "SQLite Notes",
                        "text/plain",
                        Instant.parse(
                                "2026-08-04T18:00:00Z"
                        )
                )
        );

        int exitCode = cli(service).run(
                new String[]{"list"},
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        String displayed = standardOutput();

        assertTrue(displayed.contains("ID"));
        assertTrue(displayed.contains("FORMAT"));
        assertTrue(displayed.contains("IMPORTED"));
        assertTrue(displayed.contains("TITLE"));

        assertTrue(
                displayed.contains(
                        "Java Virtual Machine"
                )
        );

        assertTrue(
                displayed.contains("SQLite Notes")
        );

        assertTrue(
                displayed.indexOf("Java Virtual Machine")
                        < displayed.indexOf("SQLite Notes")
        );
    }

    @Test
    void showsOriginalDocumentContent() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.document = Optional.of(
                new Document(
                        12,
                        "Java Virtual Machine",
                        "file:///notes/jvm.txt",
                        "text/plain",
                        "Original Java document\nSecond line",
                        "normalized searchable representation",
                        Instant.parse(
                                "2026-08-05T12:00:00Z"
                        )
                )
        );

        int exitCode = cli(service).run(
                new String[]{"show", "12"},
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        String displayed = standardOutput();

        System.out.println("OUTPUT:\n" + displayed);
        assertTrue(displayed.contains("ID:        12"));

        assertTrue(
                displayed.contains(
                        "Title:     Java Virtual Machine"
                )
        );

        assertTrue(
                displayed.contains(
                        "Source:    file:///notes/jvm.txt"
                )
        );

        assertTrue(
                displayed.contains(
                        "Original Java document"
                )
        );

        assertFalse(
                displayed.contains(
                        "normalized searchable representation"
                )
        );

        assertEquals(12, service.requestedDocumentId);
        assertTrue(errorOutput().isEmpty());
    }

    @Test
    void reportsMissingDocument() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        int exitCode = cli(service).run(
                new String[]{"show", "99"},
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
                        "Document 99 does not exist."
                )
        );

        assertEquals(99, service.requestedDocumentId);
        assertTrue(service.closed);
    }

    @Test
    void createsDatabaseDirectoryAndUsesSelectedPath() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        AtomicReference<Path> openedDatabase =
                new AtomicReference<>();

        Path databaseFile = temporaryDirectory
                .resolve("nested")
                .resolve("data")
                .resolve("notes.db");

        CliApplication cli = new CliApplication(
                path -> {
                    openedDatabase.set(path);
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
                        "formats"
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
                openedDatabase.get()
        );

        assertTrue(
                Files.isDirectory(
                        databaseFile.getParent()
                )
        );

        assertTrue(service.closed);
    }

    @Test
    void returnsFailureWhenServiceCannotBeOpened() {
        CliApplication cli = new CliApplication(
                databaseFile -> {
                    throw new IllegalStateException(
                            "Database unavailable"
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
                new String[]{"list"},
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
                        "Database unavailable"
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

        private Set<String> extensions = Set.of();
        private List<DocumentSummary> summaries =
                List.of();

        private Optional<Document> document =
                Optional.empty();

        private long requestedDocumentId;
        private boolean closed;

        @Override
        public Set<String> supportedImportExtensions() {
            return extensions;
        }

        @Override
        public List<DocumentSummary> listDocuments() {
            return summaries;
        }

        @Override
        public Optional<Document> findDocument(
                long documentId
        ) {
            requestedDocumentId = documentId;
            return document;
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
        public Document importFile(Path source) {
            throw new AssertionError(
                    "Import must not be called"
            );
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
        public boolean deleteDocument(long documentId) {
            throw new AssertionError(
                    "Delete must not be called"
            );
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}