package cz.martim12.noteindex.persistence.exception;

public class DuplicateDocumentException extends RepositoryException {
    public DuplicateDocumentException(String sourceUri, Throwable cause) {
        super("A document from this source has already been imported: " + sourceUri, cause);
    }
}
