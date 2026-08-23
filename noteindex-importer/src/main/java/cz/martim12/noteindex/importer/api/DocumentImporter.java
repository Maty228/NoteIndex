package cz.martim12.noteindex.importer.api;

import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.importer.exception.ImportException;

import java.nio.file.Path;

/**
 * Imports source files into NoteIndex document data.
 *
 * <p>Implementations handle a specific document format and convert it into
 * an {@link ImportedDocument}.</p>
 */
public interface DocumentImporter {

    /**
     * Imports a document from the given source file.
     *
     * @param source source file to import
     * @return imported document data
     * @throws ImportException if the document cannot be imported
     */
    ImportedDocument importDocument(Path source) throws ImportException;
}
