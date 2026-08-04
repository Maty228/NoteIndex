package cz.martim12.noteindex.application.importing;

import cz.martim12.noteindex.application.index.DocumentIndexMapper;
import cz.martim12.noteindex.application.index.SearchIndexSynchronizer;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.importer.annotation.ImporterPlugin;
import cz.martim12.noteindex.importer.api.DocumentImporter;
import cz.martim12.noteindex.importer.exception.UnsupportedFormatException;
import cz.martim12.noteindex.importer.registry.ImporterRegistry;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.search.analysis.UnicodeTextAnalyzer;
import cz.martim12.noteindex.search.index.FieldName;
import cz.martim12.noteindex.search.index.Posting;
import cz.martim12.noteindex.search.index.SearchIndex;
import cz.martim12.noteindex.search.index.SearchIndexes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentImportWorkflowTest {

    private SearchIndex searchIndex;
    private StubDocumentRepository repository;
    private StubTextImporter importer;
    private DocumentImportWorkflow workflow;

    @BeforeEach
    void setUp() {
        searchIndex = SearchIndexes.inMemory(
                new UnicodeTextAnalyzer()
        );

        ImportedDocument importedDocument =
                new ImportedDocument(
                        "Java Virtual Machine",
                        "file:///notes/jvm.txt",
                        "txt",
                        "Original Java content",
                        "Virtual machine bytecode execution"
                );

        Document persistedDocument =
                new Document(
                        42,
                        importedDocument.title(),
                        importedDocument.sourceUri(),
                        importedDocument.format(),
                        importedDocument.originalContent(),
                        importedDocument.searchableContent(),
                        Instant.parse(
                                "2026-08-04T20:00:00Z"
                        )
                );

        importer =
                new StubTextImporter(importedDocument);

        repository =
                new StubDocumentRepository(
                        persistedDocument
                );

        ImporterRegistry importerRegistry =
                new ImporterRegistry(
                        List.of(importer)
                );

        SearchIndexSynchronizer synchronizer =
                new SearchIndexSynchronizer(
                        repository,
                        searchIndex,
                        new DocumentIndexMapper()
                );

        workflow = new DocumentImportWorkflow(
                importerRegistry,
                repository,
                synchronizer
        );
    }

    @AfterEach
    void tearDown() {
        searchIndex.close();
    }

    @Test
    void importsPersistsAndIndexesDocument() {
        Path source = Path.of("notes", "jvm.txt");

        Document result =
                workflow.importFile(source);

        assertSame(
                repository.persistedDocument,
                result
        );

        assertEquals(
                source,
                importer.importedSource
        );

        assertEquals(
                importer.importedDocument,
                repository.savedDocument
        );

        assertEquals(1, searchIndex.documentCount());

        assertEquals(
                List.of(
                        new Posting(42, List.of(0))
                ),
                searchIndex.postings(
                        "java",
                        FieldName.TITLE
                )
        );

        assertEquals(
                List.of(
                        new Posting(42, List.of(0))
                ),
                searchIndex.postings(
                        "virtual",
                        FieldName.BODY
                )
        );
    }

    @Test
    void rejectsUnsupportedFileBeforePersistence() {
        assertThrows(
                UnsupportedFormatException.class,
                () -> workflow.importFile(
                        Path.of("notes", "jvm.md")
                )
        );

        assertFalse(repository.saveCalled);
        assertEquals(0, searchIndex.documentCount());
    }

    @Test
    void doesNotIndexWhenPersistenceFails() {
        IllegalStateException failure =
                new IllegalStateException(
                        "Persistence unavailable"
                );

        repository.saveFailure = failure;

        IllegalStateException thrown =
                assertThrows(
                        IllegalStateException.class,
                        () -> workflow.importFile(
                                Path.of("notes", "jvm.txt")
                        )
                );

        assertSame(failure, thrown);
        assertTrue(repository.saveCalled);
        assertEquals(0, searchIndex.documentCount());
    }

    @Test
    void returnsSupportedImporterExtensions() {
        Set<String> extensions =
                workflow.supportedExtensions();

        assertEquals(Set.of("txt"), extensions);

        assertThrows(
                UnsupportedOperationException.class,
                () -> extensions.add("md")
        );
    }

    @Test
    void rejectsNullSourcePath() {
        assertThrows(
                NullPointerException.class,
                () -> workflow.importFile(null)
        );

        assertFalse(repository.saveCalled);
        assertEquals(0, searchIndex.documentCount());
    }

    @ImporterPlugin(
            name = "Test text importer",
            formatId = "txt",
            extensions = {"txt"}
    )
    private static final class StubTextImporter
            implements DocumentImporter {

        private final ImportedDocument importedDocument;
        private Path importedSource;

        private StubTextImporter(
                ImportedDocument importedDocument
        ) {
            this.importedDocument = importedDocument;
        }

        @Override
        public ImportedDocument importDocument(Path source) {
            importedSource = source;
            return importedDocument;
        }
    }

    private static final class StubDocumentRepository
            implements DocumentRepository {

        private final Document persistedDocument;

        private ImportedDocument savedDocument;
        private RuntimeException saveFailure;
        private boolean saveCalled;

        private StubDocumentRepository(
                Document persistedDocument
        ) {
            this.persistedDocument =
                    persistedDocument;
        }

        @Override
        public Document save(
                ImportedDocument document
        ) {
            saveCalled = true;
            savedDocument = document;

            if (saveFailure != null) {
                throw saveFailure;
            }

            return persistedDocument;
        }

        @Override
        public Optional<Document> findById(long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Document> findAll() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DocumentSummary> findAllSummaries() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsBySourceUri(
                String sourceUri
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean deleteById(long id) {
            throw new UnsupportedOperationException();
        }
    }
}