package cz.martim12.noteindex.importer.exception;

import cz.martim12.noteindex.core.exception.NoteIndexException;

public class ImportException extends NoteIndexException {
    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
