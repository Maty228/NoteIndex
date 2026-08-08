package cz.martim12.noteindex.application.document;

import cz.martim12.noteindex.application.index.DocumentIndexMapper;
import cz.martim12.noteindex.application.index.SearchIndexSynchronizer;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.index.SearchIndex;
import cz.martim12.noteindex.search.index.SearchIndexes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentCatalogWorkflowTest {

    private SearchIndex searchIndex;
    private StubDocumentRepository repository;
    private DocumentCatalogWorkflow workflow;

    @BeforeEach
    void setUp() {
        searchIndex = SearchIndexes.inMemory(
                new UnicodeTextAnalyzer()
        );

        repository = new StubDocumentRepository(
                List.of(
                        document(
                                2,
                                "SQLite Notes",
                                "Embedded relational database"
                        ),
                        document(
                                1,
                                "Java Notes",
                                "Virtual machine and bytecode"
                        )
                )
        );

        SearchIndexSynchronizer synchronizer =
                new SearchIndexSynchronizer(
                        repository,
                        searchIndex,
                        new DocumentIndexMapper()
                );

        workflow = new DocumentCatalogWorkflow(
                repository,
                synchronizer
        );
    }

    @AfterEach
    void tearDown() {
        searchIndex.close();
    }

    @Test
    void listsDocumentSummariesInRepositoryOrder() {
        List<DocumentSummary> summaries =
                workflow.listDocuments();

        assertEquals(
                List.of(2L, 1L),
                summaries.stream()
                        .map(DocumentSummary::id)
                        .toList()
        );

        assertEquals(
                "SQLite Notes",
                summaries.getFirst().title()
        );

        assertEquals(
                "Java Notes",
                summaries.getLast().title()
        );
    }

    @Test
    void returnsImmutableSummaryList() {
        List<DocumentSummary> summaries =
                workflow.listDocuments();

        assertThrows(
                UnsupportedOperationException.class,
                () -> summaries.add(
                        new DocumentSummary(
                                99,
                                "Other",
                                "txt",
                                Instant.EPOCH
                        )
                )
        );
    }

    @Test
    void findsExistingAndMissingDocuments() {
        Optional<Document> existing =
                workflow.findDocument(1);

        assertTrue(existing.isPresent());
        assertEquals(
                "Java Notes",
                existing.orElseThrow().title()
        );

        assertTrue(
                workflow.findDocument(99).isEmpty()
        );

        assertEquals(
                List.of(1L, 99L),
                repository.requestedDocumentIds
        );
    }

    @Test
    void deletesDocumentFromRepositoryAndIndex() {
        Document indexedDocument =
                repository.findById(1).orElseThrow();

        searchIndex.indexDocument(
                new DocumentIndexMapper()
                        .map(indexedDocument)
        );

        assertFalse(
                searchIndex.postings(
                        "java",
                        FieldName.TITLE
                ).isEmpty()
        );

        assertTrue(workflow.deleteDocument(1));

        assertTrue(repository.findById(1).isEmpty());

        assertTrue(
                searchIndex.postings(
                        "java",
                        FieldName.TITLE
                ).isEmpty()
        );

        assertEquals(0, searchIndex.documentCount());
    }

    @Test
    void removesStaleIndexEntryWhenDocumentIsMissing() {
        searchIndex.indexDocument(
                new IndexDocument(
                        99,
                        Map.of(
                                FieldName.TITLE,
                                "Stale Document",
                                FieldName.BODY,
                                "Stale searchable content"
                        )
                )
        );

        assertFalse(workflow.deleteDocument(99));

        assertEquals(0, searchIndex.documentCount());

        assertTrue(
                searchIndex.postings(
                        "stale",
                        FieldName.TITLE
                ).isEmpty()
        );
    }

    @Test
    void keepsIndexUntouchedWhenRepositoryDeletionFails() {
        searchIndex.indexDocument(
                new DocumentIndexMapper().map(
                        repository.findById(1)
                                .orElseThrow()
                )
        );

        IllegalStateException failure =
                new IllegalStateException(
                        "Repository unavailable"
                );

        repository.deleteFailure = failure;

        IllegalStateException thrown =
                assertThrows(
                        IllegalStateException.class,
                        () -> workflow.deleteDocument(1)
                );

        assertSame(failure, thrown);

        assertEquals(1, searchIndex.documentCount());

        assertFalse(
                searchIndex.postings(
                        "java",
                        FieldName.TITLE
                ).isEmpty()
        );
    }

    @Test
    void rejectsNonPositiveDocumentIds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> workflow.findDocument(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> workflow.findDocument(-1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> workflow.deleteDocument(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> workflow.deleteDocument(-1)
        );

        assertTrue(repository.deletedDocumentIds.isEmpty());
    }

    @Test
    void renamesDocumentAndUpdatesSearchIndex() {
        Document original =
                repository.findById(1).orElseThrow();

        searchIndex.indexDocument(
                new DocumentIndexMapper().map(original)
        );

        assertFalse(
                searchIndex.postings(
                        "java",
                        FieldName.TITLE
                ).isEmpty()
        );

        assertTrue(
                workflow.renameDocument(
                        1,
                        "Concurrency Notes"
                )
        );

        Document renamed =
                repository.findById(1).orElseThrow();

        assertEquals(
                "Concurrency Notes",
                renamed.title()
        );

        assertTrue(
                searchIndex.postings(
                        "java",
                        FieldName.TITLE
                ).isEmpty()
        );

        assertFalse(
                searchIndex.postings(
                        "concurrency",
                        FieldName.TITLE
                ).isEmpty()
        );
    }

    private static Document document(
            long id,
            String title,
            String searchableContent
    ) {
        return new Document(
                id,
                title,
                "file:///notes/" + id + ".txt",
                "txt",
                searchableContent,
                searchableContent,
                Instant.parse(
                        "2026-08-04T20:00:00Z"
                )
        );
    }

    private static final class StubDocumentRepository
            implements DocumentRepository {

        private final Map<Long, Document> documents =
                new LinkedHashMap<>();

        private final List<Long> requestedDocumentIds =
                new ArrayList<>();

        private final List<Long> deletedDocumentIds =
                new ArrayList<>();

        private RuntimeException deleteFailure;

        private StubDocumentRepository(
                List<Document> documents
        ) {
            for (Document document : documents) {
                this.documents.put(
                        document.id(),
                        document
                );
            }
        }

        @Override
        public Optional<Document> findById(long id) {
            requestedDocumentIds.add(id);

            return Optional.ofNullable(
                    documents.get(id)
            );
        }

        @Override
        public List<DocumentSummary> findAllSummaries() {
            return documents.values()
                    .stream()
                    .map(document ->
                            new DocumentSummary(
                                    document.id(),
                                    document.title(),
                                    document.format(),
                                    document.importedAt()
                            )
                    )
                    .toList();
        }

        @Override
        public boolean deleteById(long id) {
            deletedDocumentIds.add(id);

            if (deleteFailure != null) {
                throw deleteFailure;
            }

            return documents.remove(id) != null;
        }

        @Override
        public List<Document> findAll() {
            return List.copyOf(documents.values());
        }

        @Override
        public Document save(
                ImportedDocument document
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsBySourceUri(
                String sourceUri
        ) {
            throw new UnsupportedOperationException();
        }
        @Override
        public boolean updateDisplayTitle(long id, String displayTitle) {
            Document existing = documents.get(id);

            if (existing == null) {
                return false;
            }

            documents.put(
                    id,
                    new Document(
                            existing.id(),
                            displayTitle,
                            existing.sourceUri(),
                            existing.format(),
                            existing.originalContent(),
                            existing.searchableContent(),
                            existing.importedAt()
                    )
            );

            return true;
        }
    }
}