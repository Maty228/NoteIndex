package cz.martim12.noteindex.application.service;

import cz.martim12.noteindex.application.document.DocumentCatalogWorkflow;
import cz.martim12.noteindex.application.importing.DocumentImportWorkflow;
import cz.martim12.noteindex.application.index.DocumentIndexMapper;
import cz.martim12.noteindex.application.index.SearchIndexSynchronizer;
import cz.martim12.noteindex.application.search.DocumentSearchWorkflow;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.importer.registry.ImporterRegistry;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.search.engine.SearchEngine;
import cz.martim12.noteindex.search.engine.SearchRuntime;
import cz.martim12.noteindex.search.engine.SearchRuntimes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultNoteIndexServiceTest {

    @Test
    void deleteWaitsForCompleteSearchOperation() throws Exception {
        CountDownLatch searchStarted = new CountDownLatch(1);
        CountDownLatch releaseSearch = new CountDownLatch(1);

        SearchEngine blockingSearchEngine = (query, limit) -> {
            searchStarted.countDown();

            try {
                if (!releaseSearch.await(3, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting to release search"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Search was interrupted",
                        exception
                );
            }

            return List.of();
        };

        StubDocumentRepository repository =
                new StubDocumentRepository();

        DefaultNoteIndexService service = createService(
                repository,
                blockingSearchEngine
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch deleteAttempted = new CountDownLatch(1);

        try {
            Future<?> search = executor.submit(
                    () -> service.search(
                            new SearchQuery("java"),
                            10
                    )
            );

            assertTrue(
                    searchStarted.await(3, TimeUnit.SECONDS)
            );

            Future<Boolean> delete = executor.submit(() -> {
                deleteAttempted.countDown();
                return service.deleteDocument(1);
            });

            assertTrue(
                    deleteAttempted.await(3, TimeUnit.SECONDS)
            );

            assertFalse(
                    repository.deleteCalled.await(
                            200,
                            TimeUnit.MILLISECONDS
                    )
            );

            releaseSearch.countDown();

            search.get(3, TimeUnit.SECONDS);
            assertFalse(delete.get(3, TimeUnit.SECONDS));

            assertEquals(1, repository.deleteCount.get());
        } finally {
            releaseSearch.countDown();
            executor.shutdownNow();
            executor.awaitTermination(3, TimeUnit.SECONDS);
            service.close();
        }
    }

    private static DefaultNoteIndexService createService(
            DocumentRepository repository,
            SearchEngine searchEngine
    ) {
        SearchRuntime defaultRuntime = SearchRuntimes.inMemory();

        SearchRuntime runtime = new SearchRuntime(
                defaultRuntime.index(),
                defaultRuntime.queryParser(),
                searchEngine,
                defaultRuntime.snippetExtractor()
        );

        SearchIndexSynchronizer synchronizer =
                new SearchIndexSynchronizer(
                        repository,
                        runtime.index(),
                        new DocumentIndexMapper()
                );

        DocumentImportWorkflow importWorkflow =
                new DocumentImportWorkflow(
                        new ImporterRegistry(List.of()),
                        repository,
                        synchronizer
                );

        DocumentSearchWorkflow searchWorkflow =
                new DocumentSearchWorkflow(
                        repository,
                        runtime.searchEngine(),
                        runtime.queryParser(),
                        runtime.snippetExtractor(),
                        240
                );

        DocumentCatalogWorkflow catalogWorkflow =
                new DocumentCatalogWorkflow(
                        repository,
                        synchronizer
                );

        return new DefaultNoteIndexService(
                importWorkflow,
                searchWorkflow,
                catalogWorkflow,
                runtime
        );
    }

    private static final class StubDocumentRepository
            implements DocumentRepository {

        private final CountDownLatch deleteCalled =
                new CountDownLatch(1);

        private final AtomicInteger deleteCount =
                new AtomicInteger();

        @Override
        public boolean deleteById(long id) {
            deleteCount.incrementAndGet();
            deleteCalled.countDown();
            return false;
        }

        @Override
        public Document save(ImportedDocument document) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Document> findById(long id) {
            return Optional.empty();
        }

        @Override
        public List<Document> findAll() {
            return List.of();
        }

        @Override
        public List<DocumentSummary> findAllSummaries() {
            return List.of();
        }

        @Override
        public boolean existsBySourceUri(String sourceUri) {
            return false;
        }

        @Override
        public boolean updateDisplayTitle(
                long id,
                String displayTitle
        ) {
            return false;
        }
    }
}
