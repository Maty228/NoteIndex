package cz.martim12.noteindex.application.index;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.IndexDocument;
import cz.martim12.noteindex.search.index.Posting;
import cz.martim12.noteindex.search.index.SearchIndex;
import cz.martim12.noteindex.search.index.SearchIndexes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchIndexSynchronizerTest {

    private SearchIndex index;

    @BeforeEach
    void setUp() {
        index = SearchIndexes.inMemory(
                new UnicodeTextAnalyzer()
        );
    }

    @AfterEach
    void tearDown() {
        index.close();
    }

    @Test
    void rebuildsIndexFromRepositoryAndRemovesStaleData() {
        index.indexDocument(
                new IndexDocument(
                        99,
                        Map.of(
                                FieldName.BODY,
                                "stale content"
                        )
                )
        );

        Document first = document(
                1,
                "Java Notes",
                "Virtual machine internals"
        );

        Document second = document(
                2,
                "SQLite Guide",
                "Embedded relational database"
        );

        SearchIndexSynchronizer synchronizer =
                synchronizer(
                        new StubDocumentRepository(
                                List.of(first, second)
                        )
                );

        int indexedCount = synchronizer.rebuild();

        assertEquals(2, indexedCount);
        assertEquals(2, index.documentCount());

        assertTrue(
                index.postings(
                        "stale",
                        FieldName.BODY
                ).isEmpty()
        );

        assertEquals(
                List.of(
                        new Posting(1, List.of(0))
                ),
                index.postings(
                        "java",
                        FieldName.TITLE
                )
        );

        assertEquals(
                List.of(
                        new Posting(2, List.of(0))
                ),
                index.postings(
                        "sqlite",
                        FieldName.TITLE
                )
        );
    }

    @Test
    void clearsIndexWhenRepositoryIsEmpty() {
        index.indexDocument(
                new IndexDocument(
                        1,
                        Map.of(
                                FieldName.BODY,
                                "existing content"
                        )
                )
        );

        SearchIndexSynchronizer synchronizer =
                synchronizer(
                        new StubDocumentRepository(List.of())
                );

        assertEquals(0, synchronizer.rebuild());
        assertEquals(0, index.documentCount());

        assertTrue(
                index.postings(
                        "existing",
                        FieldName.BODY
                ).isEmpty()
        );
    }

    @Test
    void indexesAndRemovesIndividualDocument() {
        SearchIndexSynchronizer synchronizer =
                synchronizer(
                        new StubDocumentRepository(List.of())
                );

        synchronizer.indexDocument(
                document(
                        1,
                        "Java Notes",
                        "Virtual machine"
                )
        );

        assertEquals(1, index.documentCount());

        assertFalse(
                index.postings(
                        "java",
                        FieldName.TITLE
                ).isEmpty()
        );

        assertTrue(synchronizer.removeDocument(1));
        assertEquals(0, index.documentCount());
        assertFalse(synchronizer.removeDocument(1));
    }

    @Test
    void keepsExistingIndexWhenRepositoryLoadingFails() {
        index.indexDocument(
                new IndexDocument(
                        50,
                        Map.of(
                                FieldName.BODY,
                                "existing searchable data"
                        )
                )
        );

        IllegalStateException failure =
                new IllegalStateException(
                        "Repository unavailable"
                );

        SearchIndexSynchronizer synchronizer =
                synchronizer(
                        StubDocumentRepository.failing(failure)
                );

        IllegalStateException thrown =
                assertThrows(
                        IllegalStateException.class,
                        synchronizer::rebuild
                );

        assertSame(failure, thrown);
        assertEquals(1, index.documentCount());

        assertFalse(
                index.postings(
                        "existing",
                        FieldName.BODY
                ).isEmpty()
        );
    }

    private SearchIndexSynchronizer synchronizer(
            DocumentRepository repository
    ) {
        return new SearchIndexSynchronizer(
                repository,
                index,
                new DocumentIndexMapper()
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
                Instant.parse("2026-08-04T18:00:00Z")
        );
    }

    private static final class StubDocumentRepository
            implements DocumentRepository {

        private final List<Document> documents;
        private final RuntimeException findAllFailure;

        private StubDocumentRepository(
                List<Document> documents
        ) {
            this(documents, null);
        }

        private StubDocumentRepository(
                List<Document> documents,
                RuntimeException findAllFailure
        ) {
            this.documents = List.copyOf(documents);
            this.findAllFailure = findAllFailure;
        }

        private static StubDocumentRepository failing(
                RuntimeException failure
        ) {
            return new StubDocumentRepository(
                    List.of(),
                    failure
            );
        }

        @Override
        public List<Document> findAll() {
            if (findAllFailure != null) {
                throw findAllFailure;
            }

            return documents;
        }

        @Override
        public Document save(ImportedDocument document) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Document> findById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DocumentSummary> findAllSummaries() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsBySourceUri(String sourceUri) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean updateDisplayTitle(long id, String displayTitle) {
            throw new UnsupportedOperationException();
        }
    }
}