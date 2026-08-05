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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliSearchCommandTest {

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
    void printsRankedSearchResults() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.results = List.of(
                new SearchResult(
                        new DocumentSummary(
                                12,
                                "Java Virtual Machine",
                                "text/plain",
                                Instant.parse(
                                        "2026-08-05T15:00:00Z"
                                )
                        ),
                        4.8274,
                        "...Java virtual machine "
                                + "executes bytecode..."
                ),
                new SearchResult(
                        new DocumentSummary(
                                7,
                                "Runtime Notes",
                                "text/plain",
                                Instant.parse(
                                        "2026-08-04T12:00:00Z"
                                )
                        ),
                        2.4126,
                        "...runtime memory is managed "
                                + "automatically..."
                )
        );

        int exitCode = cli(service).run(
                new String[]{
                        "search",
                        "java",
                        "virtual",
                        "machine"
                },
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(
                new SearchQuery(
                        "java virtual machine"
                ),
                service.receivedQuery
        );

        assertEquals(
                CliCommandParser.DEFAULT_SEARCH_LIMIT,
                service.receivedLimit
        );

        String displayed = standardOutput();

        assertTrue(
                displayed.contains("2 result(s)")
        );

        assertTrue(
                displayed.contains(
                        "1. Java Virtual Machine"
                )
        );

        assertTrue(
                displayed.contains(
                        "ID: 12 | Format: text/plain "
                                + "| Score: 4.827"
                )
        );

        assertTrue(
                displayed.contains(
                        "Java virtual machine "
                                + "executes bytecode"
                )
        );

        assertTrue(
                displayed.contains(
                        "2. Runtime Notes"
                )
        );

        assertTrue(
                displayed.contains(
                        "ID: 7 | Format: text/plain "
                                + "| Score: 2.413"
                )
        );

        assertTrue(
                displayed.indexOf(
                        "Java Virtual Machine"
                )
                        < displayed.indexOf(
                        "Runtime Notes"
                )
        );

        assertTrue(errorOutput().isEmpty());
        assertTrue(service.closed);
    }

    @Test
    void passesQuotedUnicodeQueryAndExplicitLimit() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        int exitCode = cli(service).run(
                new String[]{
                        "search",
                        "--limit",
                        "3",
                        "český",
                        "\"virtuální stroj\""
                },
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(
                new SearchQuery(
                        "český \"virtuální stroj\""
                ),
                service.receivedQuery
        );

        assertEquals(3, service.receivedLimit);

        assertEquals(
                "No matching documents.",
                standardOutput().strip()
        );

        assertTrue(errorOutput().isEmpty());
        assertTrue(service.closed);
    }

    @Test
    void treatsNoSearchResultsAsSuccess() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        int exitCode = cli(service).run(
                new String[]{
                        "search",
                        "nonexistent"
                },
                output,
                error
        );

        assertEquals(
                CliExitCode.SUCCESS,
                exitCode
        );

        assertEquals(
                "No matching documents.",
                standardOutput().strip()
        );

        assertTrue(errorOutput().isEmpty());
        assertTrue(service.closed);
    }

    @Test
    void reportsSearchFailureAndClosesService() {
        StubNoteIndexService service =
                new StubNoteIndexService();

        service.searchFailure =
                new IllegalArgumentException(
                        "Malformed search query"
                );

        int exitCode = cli(service).run(
                new String[]{
                        "search",
                        "\"unfinished phrase"
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
                        "Malformed search query"
                )
        );

        assertTrue(service.closed);
    }

    @Test
    void rejectsMissingQueryWithoutOpeningService() {
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
                new String[]{"search"},
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
                        "Usage: noteindex search "
                                + "[--limit N] <query>"
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

        private List<SearchResult> results =
                List.of();

        private RuntimeException searchFailure;
        private SearchQuery receivedQuery;
        private int receivedLimit;
        private boolean closed;

        @Override
        public List<SearchResult> search(
                SearchQuery query,
                int limit
        ) {
            receivedQuery = query;
            receivedLimit = limit;

            if (searchFailure != null) {
                throw searchFailure;
            }

            return results;
        }

        @Override
        public Document importFile(Path source) {
            throw new AssertionError(
                    "Import must not be called"
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