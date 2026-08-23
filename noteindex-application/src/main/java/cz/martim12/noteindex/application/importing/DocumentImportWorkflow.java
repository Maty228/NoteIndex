package cz.martim12.noteindex.application.importing;

import cz.martim12.noteindex.application.index.SearchIndexSynchronizer;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.importer.api.DocumentImporter;
import cz.martim12.noteindex.importer.registry.ImporterRegistry;
import cz.martim12.noteindex.persistence.api.DocumentRepository;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * Coordinates importing, persistence and search indexing.
 * SQLite remains the authoritative data store. The persisted
 * document is indexed only after the repository successfully
 * saves it.
 */
public final class DocumentImportWorkflow {

    private final ImporterRegistry importerRegistry;
    private final DocumentRepository documentRepository;
    private final SearchIndexSynchronizer indexSynchronizer;

    /**
     * Creates an import workflow.
     *
     * @param importerRegistry registry of available document importers
     * @param documentRepository repository used to store imported documents
     * @param indexSynchronizer synchronizer for updating search index data
     */
    public DocumentImportWorkflow(
            ImporterRegistry importerRegistry,
            DocumentRepository documentRepository,
            SearchIndexSynchronizer indexSynchronizer
    ) {
        this.importerRegistry = Objects.requireNonNull(importerRegistry, "Importer registry must not be null");
        this.documentRepository = Objects.requireNonNull(documentRepository, "Document repository must not be null");
        this.indexSynchronizer = Objects.requireNonNull(indexSynchronizer, "Search index synchronizer must not be null");
    }

    /**
     * Imports a source file, stores it and adds it to the search index.
     *
     * @param source source file to import
     * @return persisted document
     */
    public Document importFile(Path source) {
        Objects.requireNonNull(source, "Source file must not be null");

        DocumentImporter importer = importerRegistry.resolve(source);

        ImportedDocument importedDocument = importer.importDocument(source);

        Document persistedDocument = documentRepository.save(importedDocument);

        indexSynchronizer.indexDocument(persistedDocument);

        return persistedDocument;
    }

    /**
     * Returns normalized extensions without leading dots.
     *
     * @return normalized supported extensions
     */
    public Set<String> supportedExtensions() {
        return Set.copyOf(importerRegistry.supportedExtensions());
    }
}
