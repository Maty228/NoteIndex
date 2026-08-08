package cz.martim12.noteindex.gui.main;

import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MainViewModelTest {

    @Test
    void loadsSortsAndFiltersDocuments() throws Exception {
        StubService service = new StubService();

        service.summaries = List.of(
                summary(1, "Database", "text/plain", "2026-08-01T10:00:00Z"),
                summary(2, "Algorithms", "text/markdown", "2026-08-06T10:00:00Z"),
                summary(3, "Java", "text/markdown", "2026-08-04T10:00:00Z")
        );

        try (MainViewModel viewModel = createViewModel(service)) {
            viewModel.refresh().get(3, TimeUnit.SECONDS);

            assertEquals(3, viewModel.totalDocumentCountProperty().get());

            assertEquals(
                    List.of(2L, 3L, 1L),
                    viewModel.visibleDocuments().stream()
                            .map(DocumentSummary::id)
                            .toList()
            );

            viewModel.setLibraryView(MainViewModel.LibraryView.MARKDOWN);

            assertEquals(
                    List.of(2L, 3L),
                    viewModel.visibleDocuments().stream()
                            .map(DocumentSummary::id)
                            .toList()
            );

            viewModel.setDocumentSort(MainViewModel.DocumentSort.TITLE_ASCENDING);

            assertEquals(
                    List.of("Algorithms", "Java"),
                    viewModel.visibleDocuments().stream()
                            .map(DocumentSummary::title)
                            .toList()
            );

        }
    }

    @Test
    void loadsSelectedDocument() throws Exception {
        StubService service = new StubService();

        Document document = new Document(
                7,
                "Java Memory",
                "file:///notes/java.md",
                "text/markdown",
                "# Java Memory",
                "Java Memory",
                Instant.parse("2026-08-06T10:00:00Z")
        );

        service.document = Optional.of(document);

        try (MainViewModel viewModel = createViewModel(service)) {
            DocumentSummary summary = new DocumentSummary(
                    7,
                    "Java Memory",
                    "text/markdown",
                    document.importedAt()
            );

            viewModel.selectDocument(summary).get(3, TimeUnit.SECONDS);

            assertSame(
                    document,
                    viewModel.selectedDocumentProperty().get()
            );

            assertEquals(7, service.requestedDocumentId);

        }
    }

    @Test
    void exposesLibraryFailure() {
        StubService service = new StubService();
        service.listFailure = new IllegalStateException("Could not load documents");

        try (MainViewModel viewModel = createViewModel(service)) {
            try {
                viewModel.refresh().get(3, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }

            assertInstanceOf(IllegalStateException.class, viewModel.errorProperty().get());

            assertEquals(
                    "Could not load documents",
                    viewModel.errorProperty().get().getMessage()
            );

        }
    }

    @Test
    void renamesDocumentThroughApplicationService() throws Exception {
        StubService service = new StubService();

        MainViewModel viewModel = createViewModel(service);

        try {
            boolean renamed = viewModel.renameDocument(
                    7,
                    "Neural Networks"
            ).get(3, TimeUnit.SECONDS);

            assertTrue(renamed);

            assertEquals(
                    7,
                    service.renamedDocumentId
            );

            assertEquals(
                    "Neural Networks",
                    service.renamedTitle
            );

        } finally {
            viewModel.close();
        }
    }

    private MainViewModel createViewModel(StubService service) {
        return new MainViewModel(
                service,
                Executors.newSingleThreadExecutor(),
                Runnable::run
        );
    }

    private static DocumentSummary summary(
            long id,
            String title,
            String format,
            String importedAt
    ) {
        return new DocumentSummary(
                id,
                title,
                format,
                Instant.parse(importedAt)
        );
    }

    private static final class StubService implements NoteIndexService {

        private List<DocumentSummary> summaries = List.of();
        private Optional<Document> document = Optional.empty();

        private long deletedDocumentId;

        private RuntimeException listFailure;
        private long requestedDocumentId;

        private boolean renameResult = true;
        private long renamedDocumentId;
        private String renamedTitle;

        @Override
        public List<DocumentSummary> listDocuments() {
            if (listFailure != null) {
                throw listFailure;
            }

            return summaries;
        }

        @Override
        public Optional<Document> findDocument(long documentId) {
            requestedDocumentId = documentId;
            return document;
        }

        @Override
        public Document importFile(Path source) {
            throw new AssertionError("Import must not be called");
        }

        @Override
        public List<SearchResult> search(SearchQuery query, int limit) {
            throw new AssertionError("Search must not be called");
        }

        @Override
        public boolean renameDocument(
                long documentId,
                String newTitle
        ) {
            renamedDocumentId = documentId;
            renamedTitle = newTitle;

            return renameResult;
        }

        @Override
        public boolean deleteDocument(long documentId) {
            deletedDocumentId = documentId;
            boolean deleteResult = true;
            return deleteResult;
        }

        @Override
        public Set<String> supportedImportExtensions() {
            return Set.of("txt", "md", "markdown");
        }

        @Override
        public void close() {
        }
    }



    @Test
    void deletesSelectedDocumentAndClearsSelection() throws Exception {
        StubService service = new StubService();

        Document document = new Document(
                7,
                "Java Memory",
                "file:///notes/java.md",
                "text/markdown",
                "# Java Memory",
                "Java Memory",
                Instant.parse("2026-08-07T12:00:00Z")
        );

        service.document = Optional.of(document);

        try (MainViewModel viewModel = createViewModel(service)) {
            DocumentSummary summary = new DocumentSummary(
                    7,
                    "Java Memory",
                    "text/markdown",
                    document.importedAt()
            );

            viewModel.selectDocument(summary).get(3, TimeUnit.SECONDS);

            assertSame(
                    document,
                    viewModel.selectedDocumentProperty().get()
            );

            boolean deleted = viewModel.deleteDocument(7)
                    .get(3, TimeUnit.SECONDS);

            assertTrue(deleted);
            assertEquals(7, service.deletedDocumentId);

            assertNull(viewModel.selectedDocumentProperty().get());

        }
    }
}