package cz.martim12.noteindex.persistence.exception;

import cz.martim12.noteindex.core.exception.NoteIndexException;

public class RepositoryException extends NoteIndexException {
    public RepositoryException(String message) {
        super(message);
    }
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
