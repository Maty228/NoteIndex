package cz.martim12.noteindex.gui.importflow;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportCoordinatorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void importsMultipleDocumentsSequentiallyAndReportsProgress() throws Exception {
        StubService service = new StubService();

        ImportCoordinator coordinator = createCoordinator(service);

        Path first = temporaryDirectory.resolve("first.txt");
        Path second = temporaryDirectory.resolve("second.md");

        List<ImportProgress> progress = new ArrayList<>();

        try {
            ImportBatchResult result = coordinator.importFiles(
                    List.of(first, second),
                    progress::add
            ).get(3, TimeUnit.SECONDS);

            assertEquals(2, result.importedDocuments().size());
            assertTrue(result.failures().isEmpty());

            assertEquals(
                    List.of(
                            first.toAbsolutePath().normalize(),
                            second.toAbsolutePath().normalize()
                    ),
                    service.importedSources
            );

            assertEquals(2, progress.size());

            assertEquals(1, progress.getFirst().current());
            assertEquals(2, progress.getFirst().total());

            assertEquals(2, progress.getLast().current());
            assertEquals(2, progress.getLast().total());

        } finally {
            coordinator.close();
        }
    }

    @Test
    void continuesImportingAfterIndividualFailure() throws Exception {
        StubService service = new StubService();

        Path failing = temporaryDirectory.resolve("broken.md");

        service.failingSource = failing.toAbsolutePath().normalize();

        ImportCoordinator coordinator = createCoordinator(service);

        try {
            ImportBatchResult result = coordinator.importFiles(
                    List.of(
                            temporaryDirectory.resolve("first.txt"),
                            failing,
                            temporaryDirectory.resolve("last.md")
                    ),
                    progress -> {
                    }
            ).get(3, TimeUnit.SECONDS);

            assertEquals(2, result.importedDocuments().size());
            assertEquals(1, result.failures().size());

            assertEquals(
                    failing.toAbsolutePath().normalize(),
                    result.failures().getFirst().source()
            );

            assertEquals(
                    "Could not import test document",
                    result.failures().getFirst().message()
            );

        } finally {
            coordinator.close();
        }
    }

    @Test
    void removesDuplicatePathsFromOneBatch() throws Exception {
        StubService service = new StubService();

        ImportCoordinator coordinator = createCoordinator(service);

        Path source = temporaryDirectory.resolve("same.md");

        try {
            ImportBatchResult result = coordinator.importFiles(
                    List.of(source, source, source),
                    progress -> {
                    }
            ).get(3, TimeUnit.SECONDS);

            assertEquals(1, result.importedDocuments().size());
            assertEquals(1, service.importedSources.size());

        } finally {
            coordinator.close();
        }
    }

    @Test
    void exposesSupportedExtensions() {
        StubService service = new StubService();

        ImportCoordinator coordinator = createCoordinator(service);

        try {
            assertEquals(
                    Set.of("txt", "md", "markdown"),
                    coordinator.supportedExtensions()
            );

            assertFalse(coordinator.isImporting());

        } finally {
            coordinator.close();
        }
    }

    private ImportCoordinator createCoordinator(StubService service) {
        return new ImportCoordinator(
                service,
                Executors.newSingleThreadExecutor(),
                Runnable::run
        );
    }

    private static final class StubService implements NoteIndexService {

        private final List<Path> importedSources = new ArrayList<>();

        private Path failingSource;
        private long nextId = 1;

        @Override
        public Document importFile(Path source) {
            Path normalized = source.toAbsolutePath().normalize();

            importedSources.add(normalized);

            if (normalized.equals(failingSource)) {
                throw new IllegalStateException("Could not import test document");
            }

            long id = nextId++;

            return new Document(
                    id,
                    source.getFileName().toString(),
                    normalized.toUri().toString(),
                    source.toString().endsWith(".txt")
                            ? "text/plain"
                            : "text/markdown",
                    "Original content",
                    "Searchable content",
                    Instant.parse("2026-08-07T12:00:00Z")
            );
        }

        @Override
        public List<SearchResult> search(SearchQuery query, int limit) {
            throw new AssertionError("Search must not be called");
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
            return Set.of("txt", "md", "markdown");
        }

        @Override
        public void close() {
        }
    }
}