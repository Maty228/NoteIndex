package cz.martim12.noteindex.application.api;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import cz.martim12.noteindex.importer.annotation.ImporterPlugin;
import cz.martim12.noteindex.importer.api.DocumentImporter;
import cz.martim12.noteindex.importer.registry.ImporterRegistry;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.search.engine.SearchRuntime;
import cz.martim12.noteindex.search.engine.SearchRuntimes;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteIndexApplicationsTest {

    @Test
    void rebuildsSearchIndexDuringAssembly() {
        StubDocumentRepository repository =
                new StubDocumentRepository(
                        List.of(
                                document(
                                        1,
                                        "Java Notes",
                                        "Virtual machine bytecode"
                                )
                        )
                );

        try (
                NoteIndexService service =
                        createService(repository)
        ) {
            List<SearchResult> results =
                    service.search(
                            new SearchQuery("java"),
                            10
                    );

            assertEquals(1, results.size());
            assertEquals(
                    1,
                    results.getFirst().document().id()
            );

            assertTrue(
                    results.getFirst()
                            .snippet()
                            .toLowerCase()
                            .contains("virtual")
            );
        }
    }

    @Test
    void exposesCompleteApplicationWorkflow() {
        StubDocumentRepository repository =
                new StubDocumentRepository(List.of());

        try (
                NoteIndexService service =
                        createService(repository)
        ) {
            assertEquals(
                    Set.of("txt"),
                    service.supportedImportExtensions()
            );

            Document imported =
                    service.importFile(
                            Path.of("sqlite.txt")
                    );

            assertEquals(
                    "SQLite Guide",
                    imported.title()
            );

            assertEquals(
                    List.of(imported.id()),
                    service.listDocuments()
                            .stream()
                            .map(DocumentSummary::id)
                            .toList()
            );

            assertTrue(
                    service.findDocument(imported.id())
                            .isPresent()
            );

            List<SearchResult> searchResults =
                    service.search(
                            new SearchQuery(
                                    "embedded database"
                            ),
                            10
                    );

            assertEquals(1, searchResults.size());

            assertEquals(
                    imported.id(),
                    searchResults.getFirst()
                            .document()
                            .id()
            );

            assertTrue(
                    service.deleteDocument(imported.id())
            );

            assertTrue(service.listDocuments().isEmpty());

            assertTrue(
                    service.search(
                            new SearchQuery("database"),
                            10
                    ).isEmpty()
            );

            assertFalse(
                    service.deleteDocument(imported.id())
            );
        }
    }

    @Test
    void closesIdempotentlyAndRejectsFurtherOperations() {
        StubDocumentRepository repository =
                new StubDocumentRepository(List.of());

        NoteIndexService service =
                createService(repository);

        service.close();
        service.close();

        assertThrows(
                IllegalStateException.class,
                service::listDocuments
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.search(
                        new SearchQuery("java"),
                        10
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.importFile(
                        Path.of("notes.txt")
                )
        );
    }

    @Test
    void rejectsInvalidAssemblyArguments() {
        StubDocumentRepository repository =
                new StubDocumentRepository(List.of());

        ImporterRegistry registry =
                importerRegistry();

        SearchRuntime searchRuntime =
                SearchRuntimes.inMemory();

        try {
            assertThrows(
                    NullPointerException.class,
                    () -> NoteIndexApplications.create(
                            null,
                            registry,
                            searchRuntime,
                            100
                    )
            );

            assertThrows(
                    NullPointerException.class,
                    () -> NoteIndexApplications.create(
                            repository,
                            null,
                            searchRuntime,
                            100
                    )
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> NoteIndexApplications.create(
                            repository,
                            registry,
                            searchRuntime,
                            0
                    )
            );
        } finally {
            searchRuntime.close();
        }
    }

    private static NoteIndexService createService(
            DocumentRepository repository
    ) {
        return NoteIndexApplications.create(
                repository,
                importerRegistry(),
                SearchRuntimes.inMemory(),
                80
        );
    }

    private static ImporterRegistry importerRegistry() {
        return new ImporterRegistry(
                List.of(new StubTextImporter())
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

    @ImporterPlugin(
            name = "Test text importer",
            formatId = "txt",
            extensions = {"txt"}
    )
    private static final class StubTextImporter
            implements DocumentImporter {

        @Override
        public ImportedDocument importDocument(
                Path source
        ) {
            return new ImportedDocument(
                    "SQLite Guide",
                    source.toAbsolutePath()
                            .normalize()
                            .toUri()
                            .toString(),
                    "txt",
                    "SQLite is an embedded database.",
                    "SQLite is an embedded relational database."
            );
        }
    }

    private static final class StubDocumentRepository
            implements DocumentRepository {

        private final Map<Long, Document> documents =
                new LinkedHashMap<>();

        private long nextId = 1;

        private StubDocumentRepository(
                List<Document> initialDocuments
        ) {
            for (Document document : initialDocuments) {
                documents.put(document.id(), document);
                nextId = Math.max(
                        nextId,
                        document.id() + 1
                );
            }
        }

        @Override
        public Document save(
                ImportedDocument importedDocument
        ) {
            Document document = new Document(
                    nextId++,
                    importedDocument.title(),
                    importedDocument.sourceUri(),
                    importedDocument.format(),
                    importedDocument.originalContent(),
                    importedDocument.searchableContent(),
                    Instant.parse(
                            "2026-08-04T20:00:00Z"
                    )
            );

            documents.put(document.id(), document);

            return document;
        }

        @Override
        public Optional<Document> findById(long id) {
            return Optional.ofNullable(documents.get(id));
        }

        @Override
        public List<Document> findAll() {
            return List.copyOf(documents.values());
        }

        @Override
        public List<DocumentSummary> findAllSummaries() {
            List<DocumentSummary> summaries =
                    new ArrayList<>();

            for (Document document : documents.values()) {
                summaries.add(
                        new DocumentSummary(
                                document.id(),
                                document.title(),
                                document.format(),
                                document.importedAt()
                        )
                );
            }

            return List.copyOf(summaries);
        }

        @Override
        public boolean existsBySourceUri(
                String sourceUri
        ) {
            return documents.values()
                    .stream()
                    .anyMatch(document ->
                            document.sourceUri()
                                    .equals(sourceUri)
                    );
        }

        @Override
        public boolean deleteById(long id) {
            return documents.remove(id) != null;
        }

        @Override
        public boolean updateDisplayTitle(long id, String displayTitle) {
            Document existing = documents.get(id);

            if (existing == null) {
                return false;
            }

            Document renamed = new Document(
                    existing.id(),
                    displayTitle,
                    existing.sourceUri(),
                    existing.format(),
                    existing.originalContent(),
                    existing.searchableContent(),
                    existing.importedAt()
            );

            documents.put(id, renamed);

            return true;
        }

    }
}