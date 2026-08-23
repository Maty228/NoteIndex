package cz.martim12.noteindex.persistence.exception;

/**
 * Indicates that a document with the same source already exists.
 */
public class DuplicateDocumentException extends RepositoryException {

    /**
     * Creates an exception for a duplicate document source.
     *
     * @param sourceUri source URI of the duplicate document
     * @param cause database failure causing the duplicate detection
     */
    public DuplicateDocumentException(String sourceUri, Throwable cause) {
        super("A document from this source has already been imported: " + sourceUri, cause);
    }
}
