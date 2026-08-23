package cz.martim12.noteindex.gui.search;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javafx.collections.ListChangeListener;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class SearchCoordinatorTest {

    @Test
    void preservesRawQueryAndRankedResultOrder() throws Exception {
        StubService service = new StubService();

        service.results = List.of(
                result(2, "Neural Networks", 7.5),
                result(1, "Probability", 3.2)
        );

        try (SearchCoordinator coordinator = createCoordinator(service, 0)) {
            String query = "neural \"cross entropy\"";

            coordinator.search(query).get(3, TimeUnit.SECONDS);

            assertEquals(query, service.lastQuery);
            assertEquals(50, service.lastLimit);

            assertEquals(
                    List.of(2L, 1L),
                    coordinator.results().stream()
                            .map(result -> result.document().id())
                            .toList()
            );

            assertFalse(coordinator.searchingProperty().get());

        }
    }

    @Test
    void blankQueryClearsResultsWithoutExecutingSearch() throws Exception {
        StubService service = new StubService();

        service.results = List.of(
                result(1, "Java", 4.0)
        );

        try (SearchCoordinator coordinator = createCoordinator(service, 0)) {
            coordinator.search("java").get(3, TimeUnit.SECONDS);

            assertEquals(1, coordinator.results().size());
            assertEquals(1, service.searchCount);

            coordinator.search("   ").get(3, TimeUnit.SECONDS);

            assertTrue(coordinator.results().isEmpty());
            assertEquals(1, service.searchCount);

        }
    }

    @Test
    void debounceExecutesOnlyLatestPendingQuery() throws Exception {
        StubService service = new StubService();

        try (SearchCoordinator coordinator = createCoordinator(service, 100)) {
            coordinator.search("jav");
            coordinator.search("java");

            coordinator.search("java memory")
                    .get(3, TimeUnit.SECONDS);

            assertEquals(
                    List.of("java memory"),
                    service.executedQueries
            );

        }
    }

    @Test
    void ignoresResultsFromStaleInFlightSearch() throws Exception {
        StubService service = new StubService();

        service.resultsByQuery = Map.of(
                "old",
                List.of(result(1, "Old Result", 4.0)),
                "new",
                List.of(result(2, "New Result", 8.0))
        );

        service.blockedQuery = "old";
        service.searchStarted = new CountDownLatch(1);
        service.releaseSearch = new CountDownLatch(1);

        try (SearchCoordinator coordinator = createCoordinator(service, 0)) {
            List<List<Long>> publishedResults = new ArrayList<>();

            coordinator.results().addListener(
                    (ListChangeListener<SearchResult>) change ->
                            publishedResults.add(
                                    coordinator.results().stream()
                                            .map(result -> result.document().id())
                                            .toList()
                            )
            );

            coordinator.search("old");

            assertTrue(
                    service.searchStarted.await(
                            3,
                            TimeUnit.SECONDS
                    )
            );

            var newestSearch = coordinator.search("new");

            service.releaseSearch.countDown();

            newestSearch.get(3, TimeUnit.SECONDS);

            assertEquals(
                    List.of("old", "new"),
                    service.executedQueries
            );

            assertFalse(
                    publishedResults.contains(
                            List.of(1L)
                    )
            );

            assertEquals(
                    List.of(2L),
                    coordinator.results().stream()
                            .map(result -> result.document().id())
                            .toList()
            );
        }
    }

    @Test
    void exposesSearchFailure() {
        StubService service = new StubService();

        service.failure = new IllegalStateException(
                "Search engine unavailable"
        );

        try (SearchCoordinator coordinator = createCoordinator(service, 0)) {
            try {
                coordinator.search("java")
                        .get(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }

            assertInstanceOf(IllegalStateException.class, coordinator.errorProperty().get());

            assertEquals(
                    "Search engine unavailable",
                    coordinator.errorProperty().get().getMessage()
            );

            assertTrue(coordinator.results().isEmpty());

        }
    }

    @Test
    void usesConfiguredResultLimit() throws Exception {
        StubService service = new StubService();

        try (SearchCoordinator coordinator = createCoordinator(service, 0)) {
            coordinator.setResultLimit(100);

            coordinator.search("java")
                    .get(3, TimeUnit.SECONDS);

            assertEquals(
                    100,
                    service.lastLimit
            );

            assertEquals(
                    100,
                    coordinator.resultLimit()
            );

        }
    }
    @Test
    void rejectsInvalidResultLimit() {
        StubService service = new StubService();

        try (SearchCoordinator coordinator = createCoordinator(service, 0)) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> coordinator.setResultLimit(0)
            );

        }
    }

    @Test
    void waitsForClosingQuoteBeforeSearching()
            throws Exception {

        StubService service =
                new StubService();

        try (
                SearchCoordinator coordinator =
                        createCoordinator(
                                service,
                                0
                        )
        ) {
            coordinator.search("\"virtual")
                    .get(
                            3,
                            TimeUnit.SECONDS
                    );

            assertEquals(
                    0,
                    service.searchCount
            );

            assertTrue(
                    coordinator.results()
                            .isEmpty()
            );

            assertNull(
                    coordinator.errorProperty()
                            .get()
            );

            assertTrue(
                    coordinator
                            .unfinishedQuotedPhraseProperty()
                            .get()
            );

            coordinator.search("")
                    .get(3, TimeUnit.SECONDS);

            assertFalse(
                    coordinator
                            .unfinishedQuotedPhraseProperty()
                            .get()
            );

            coordinator.search(
                            "\"virtual machine\""
                    )
                    .get(
                            3,
                            TimeUnit.SECONDS
                    );

            assertEquals(
                    1,
                    service.searchCount
            );

            assertFalse(
                    coordinator
                            .unfinishedQuotedPhraseProperty()
                            .get()
            );
        }
    }

    private SearchCoordinator createCoordinator(
            StubService service,
            long debounceMillis
    ) {
        return new SearchCoordinator(
                service,
                Executors.newSingleThreadScheduledExecutor(),
                Runnable::run,
                debounceMillis,
                50
        );
    }

    private static SearchResult result(
            long id,
            String title,
            double score
    ) {
        DocumentSummary summary = new DocumentSummary(
                id,
                title,
                "text/markdown",
                Instant.parse("2026-08-07T12:00:00Z")
        );

        return new SearchResult(
                summary,
                score,
                "Relevant searchable text for " + title
        );
    }

    private static final class StubService implements NoteIndexService {

        private List<SearchResult> results = List.of();
        private Map<String, List<SearchResult>> resultsByQuery = Map.of();

        private String blockedQuery;
        private CountDownLatch searchStarted;
        private CountDownLatch releaseSearch;
        private RuntimeException failure;

        private final List<String> executedQueries = new ArrayList<>();

        private String lastQuery;
        private int lastLimit;
        private int searchCount;

        @Override
        public List<SearchResult> search(SearchQuery query, int limit) {
            searchCount++;

            String text = query.text();

            lastQuery = text;
            lastLimit = limit;

            executedQueries.add(text);

            if (failure != null) {
                throw failure;
            }

            if (text.equals(blockedQuery)) {
                searchStarted.countDown();

                try {
                    if (!releaseSearch.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Timed out waiting to release blocked search"
                        );
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "Blocked search was interrupted",
                            exception
                    );
                }
            }

            return resultsByQuery.getOrDefault(
                    text,
                    results
            );
        }

        @Override
        public Document importFile(Path source) {
            throw new AssertionError("Import must not be called");
        }

        @Override
        public List<DocumentSummary> listDocuments() {
            return List.of();
        }

        @Override
        public Optional<Document> findDocument(long documentId) {
            return Optional.empty();
        }

        @Override
        public boolean deleteDocument(long documentId) {
            throw new AssertionError("Delete must not be called");
        }

        @Override
        public Set<String> supportedImportExtensions() {
            return Set.of();
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
        public void close() {
        }
    }
}