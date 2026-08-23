package cz.martim12.noteindex.importer.exception;

import java.nio.file.Path;

/**
 * Base exception for unsupported format failures.
 */
public class UnsupportedFormatException extends ImportException{
    /**
     * Creates an unsupported format exception from a path.
     *
     * @param source path of the source file
     */
    public UnsupportedFormatException(Path source) {
        super("Unsupported document format: " + source);
    }
}
