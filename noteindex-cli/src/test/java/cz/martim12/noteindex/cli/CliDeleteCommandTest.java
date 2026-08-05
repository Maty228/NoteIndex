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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliDeleteCommandTest {

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
    void deletesExistingDocument() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.deleteResult = true;

        int exitCode = cli(service).run(
                new String[]{"delete", "12"},
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(
                12,
                service.deletedDocumentId
        );

        assertEquals(
                "Deleted document 12.",
                standardOutput().strip()
        );

        assertTrue(errorOutput().isEmpty());
        assertTrue(service.closed);
    }

    @Test
    void reportsMissingDocument() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.deleteResult = false;

        int exitCode = cli(service).run(
                new String[]{"delete", "99"},
                output,
                error
        );

        assertEquals(
                CliExitCode.FAILURE,
                exitCode
        );

        assertEquals(
                99,
                service.deletedDocumentId
        );

        assertTrue(standardOutput().isEmpty());

        assertEquals(
                "Error: Document 99 does not exist.",
                errorOutput().strip()
        );

        assertTrue(service.closed);
    }

    @Test
    void reportsDeletionFailureAndClosesService() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.deleteFailure =
                new IllegalStateException(
                        "Could not delete document"
                );

        int exitCode = cli(service).run(
                new String[]{"delete", "12"},
                output,
                error
        );

        assertEquals(
                CliExitCode.FAILURE,
                exitCode
        );

        assertTrue(standardOutput().isEmpty());

        assertEquals(
                "Error: Could not delete document",
                errorOutput().strip()
        );

        assertTrue(service.closed);
    }

    @Test
    void rejectsInvalidDocumentIdWithoutOpeningService() {
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
                new String[]{"delete", "0"},
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
                        "Document ID must be positive"
                )
        );
    }

    @Test
    void deleteHelpDoesNotOpenService() {
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
                        "delete",
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
                        "noteindex delete <document-id>"
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

        private boolean deleteResult;
        private RuntimeException deleteFailure;
        private long deletedDocumentId;
        private boolean closed;

        @Override
        public boolean deleteDocument(long documentId) {
            deletedDocumentId = documentId;

            if (deleteFailure != null) {
                throw deleteFailure;
            }

            return deleteResult;
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