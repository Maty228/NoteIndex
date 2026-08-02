package cz.martim12.noteindex.importer.api;

import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.importer.exception.ImportException;

import java.nio.file.Path;

public interface DocumentImporter {
    ImportedDocument importDocument(Path source) throws ImportException;
}
