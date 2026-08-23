package cz.martim12.noteindex.persistence.exception;

import cz.martim12.noteindex.core.exception.NoteIndexException;

/**
 * Base exception for persistence-related failures.
 */
public class RepositoryException extends NoteIndexException {

    /**
     * Creates a repository exception with a message.
     *
     * @param message error description
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * Creates a repository exception with a message and cause.
     *
     * @param message error description
     * @param cause original cause
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
