package cz.martim12.noteindex.application.api;

import cz.martim12.noteindex.application.document.DocumentCatalogWorkflow;
import cz.martim12.noteindex.application.importing.DocumentImportWorkflow;
import cz.martim12.noteindex.application.index.DocumentIndexMapper;
import cz.martim12.noteindex.application.index.SearchIndexSynchronizer;
import cz.martim12.noteindex.application.search.DocumentSearchWorkflow;
import cz.martim12.noteindex.application.service.DefaultNoteIndexService;
import cz.martim12.noteindex.importer.registry.ImporterRegistry;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.persistence.jdbc.DatabaseInitializer;
import cz.martim12.noteindex.persistence.jdbc.JdbcDocumentRepository;
import cz.martim12.noteindex.persistence.jdbc.SqliteConnectionFactory;
import cz.martim12.noteindex.search.engine.SearchRuntime;
import cz.martim12.noteindex.search.engine.SearchRuntimes;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Entry point for creating complete NoteIndex application
 * services.
 */
public final class NoteIndexApplications {

    /**
     * Default maximum length of generated search snippets.
     */
    public static final int DEFAULT_MAXIMUM_SNIPPET_LENGTH = 240;

    private NoteIndexApplications() {}

    /**
     * Opens NoteIndex using a SQLite database file, discovers
     * importer plugins and rebuilds the in-memory search index.
     *
     * @param databaseFile SQLite database file to open
     * @return opened NoteIndex application service
     */
    public static NoteIndexService open(Path databaseFile) {
        Objects.requireNonNull(databaseFile, "Database file must not be null");

        Path normalizedDatabaseFile = databaseFile.toAbsolutePath().normalize();

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(normalizedDatabaseFile);

        DatabaseInitializer databaseInitializer = new DatabaseInitializer(connectionFactory);

        databaseInitializer.initialize();

        DocumentRepository documentRepository = new JdbcDocumentRepository(connectionFactory);

        ImporterRegistry importerRegistry = ImporterRegistry.discover();

        SearchRuntime searchRuntime = SearchRuntimes.inMemory();

        return create(
                documentRepository,
                importerRegistry,
                searchRuntime,
                DEFAULT_MAXIMUM_SNIPPET_LENGTH
        );
    }

    /**
     * Internal assembly method used by tests and alternative
     * runtime entry points.
     */
    static NoteIndexService create(
            DocumentRepository documentRepository,
            ImporterRegistry importerRegistry,
            SearchRuntime searchRuntime,
            int maximumSnippetLength
    ) {
        Objects.requireNonNull(documentRepository, "Document repository must not be null");

        Objects.requireNonNull(importerRegistry, "Importer registry must not be null");

        Objects.requireNonNull(searchRuntime, "Search runtime must not be null");

        if (maximumSnippetLength <= 0) {
            throw new IllegalArgumentException(
                    "Maximum snippet length must be positive"
            );
        }

        try {
            DocumentIndexMapper indexMapper = new DocumentIndexMapper();

            SearchIndexSynchronizer indexSynchronizer = new SearchIndexSynchronizer(documentRepository, searchRuntime.index(), indexMapper);

            /*
             * SQLite is authoritative. Reconstruct the derived
             * in-memory index before exposing the service.
             */
            indexSynchronizer.rebuild();

            DocumentImportWorkflow importWorkflow = new DocumentImportWorkflow(importerRegistry, documentRepository, indexSynchronizer);

            DocumentSearchWorkflow searchWorkflow = new DocumentSearchWorkflow(documentRepository, searchRuntime.searchEngine(), searchRuntime.queryParser(), searchRuntime.snippetExtractor(), maximumSnippetLength);

            DocumentCatalogWorkflow catalogWorkflow = new DocumentCatalogWorkflow(documentRepository, indexSynchronizer);

            return new DefaultNoteIndexService(importWorkflow, searchWorkflow, catalogWorkflow, searchRuntime);
        } catch (RuntimeException | Error failure) {
            closeAfterAssemblyFailure(searchRuntime, failure);
            throw failure;
        }
    }

    private static void closeAfterAssemblyFailure(SearchRuntime searchRuntime, Throwable failure) {
        try {
            searchRuntime.close();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
