package cz.martim12.noteindex.importer.exception;

import cz.martim12.noteindex.core.exception.NoteIndexException;

/**
 * Base exception for document import failures.
 */
public class ImportException extends NoteIndexException {
    /**
     * Creates an import exception with a message.
     *
     * @param message error description
     */
    public ImportException(String message) {
        super(message);
    }

    /**
     * Creates an import exception with a message and cause.
     *
     * @param message error description
     * @param cause original cause
     */
    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
