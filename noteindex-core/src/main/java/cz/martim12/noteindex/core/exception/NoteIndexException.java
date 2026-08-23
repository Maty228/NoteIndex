package cz.martim12.noteindex.core.exception;

/**
 * Base unchecked exception for errors reported by NoteIndex components.
 *
 * <p>This exception may be used when an operation cannot be completed because
 * of an application-level failure that callers are not required to handle
 * immediately.</p>
 */
public class NoteIndexException extends RuntimeException {
    /**
     * Creates an exception with the specified detail message.
     *
     * @param message description of the failure
     */
    public NoteIndexException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified detail message and underlying cause.
     *
     * @param message description of the failure
     * @param cause underlying cause of the failure
     */
    public NoteIndexException(String message, Throwable cause) {
        super(message, cause);
    }
}
