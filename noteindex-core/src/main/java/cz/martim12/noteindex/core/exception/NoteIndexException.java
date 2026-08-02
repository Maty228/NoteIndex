package cz.martim12.noteindex.core.exception;

public class NoteIndexException extends RuntimeException {
    public NoteIndexException(String message) {
        super(message);
    }

    public NoteIndexException(String message, Throwable cause) {
        super(message, cause);
    }
}
