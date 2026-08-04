package cz.martim12.noteindex.application.api;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Public application-facing operations provided by NoteIndex.

 * Implementations coordinate importing, persistence and search.
 * User-interface modules should use this interface rather than
 * accessing the individual infrastructure modules directly.
 */
public interface NoteIndexService extends AutoCloseable{

    /**
     * Imports a file, stores the resulting document and makes it
     * available to the search index.
     *
     * @param source path of the source file
     * @return persisted document with its generated ID
     */
    Document importFile(Path source);

    /**
     * Searches indexed documents and returns ranked results with
     * display snippets.
     *
     * @param query user search query
     * @param limit maximum number of returned results
     * @return results ordered by descending relevance
     */
    List<SearchResult> search(SearchQuery query, int limit);

    /**
     * Lists stored documents without loading their full contents.
     */
    List<DocumentSummary> listDocuments();

    /**
     * Loads one complete document.
     */
    Optional<Document> findDocument(long documentId);

    /**
     * Deletes a stored document and removes it from the index.
     *
     * @return true when a document was deleted
     */
    boolean deleteDocument(long documentId);

    /**
     * Returns normalized extensions accepted by the available
     * importer plugins, without leading dots.
     */
    Set<String> supportedImportExtensions();

    /**
     * Releases resources owned by the application service.
     */
    @Override
    void close();

}

