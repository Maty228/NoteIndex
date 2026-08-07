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
        private RuntimeException failure;

        private final List<String> executedQueries = new ArrayList<>();

        private String lastQuery;
        private int lastLimit;
        private int searchCount;

        @Override
        public List<SearchResult> search(SearchQuery query, int limit) {
            searchCount++;

            lastQuery = query.text();
            lastLimit = limit;

            executedQueries.add(query.text());

            if (failure != null) {
                throw failure;
            }

            return results;
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
        public void close() {
        }
    }
}