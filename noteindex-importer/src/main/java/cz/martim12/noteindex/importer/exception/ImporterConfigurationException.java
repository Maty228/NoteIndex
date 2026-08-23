package cz.martim12.noteindex.importer.exception;


/**
 * Base exception for importer configuration failures.
 */
public class ImporterConfigurationException extends ImportException{
    /**
     * Creates an import configuration exception with a message.
     *
     * @param message error description
     */
    public ImporterConfigurationException(String message) {
        super(message);
    }
}
