package cz.martim12.noteindex.importer.exception;

import java.nio.file.Path;

public class UnsupportedFormatException extends ImportException{
    public UnsupportedFormatException(Path source) {
        super("Unsupported document format: " + source);
    }
}
