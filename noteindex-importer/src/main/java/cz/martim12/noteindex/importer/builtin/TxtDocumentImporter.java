package cz.martim12.noteindex.importer.builtin;

import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.importer.api.DocumentImporter;
import cz.martim12.noteindex.importer.exception.ImportException;
import cz.martim12.noteindex.importer.annotation.ImporterPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Imports plain text files into NoteIndex documents.
 */
@ImporterPlugin(
        name= "Plain Text Importer",
        formatId = "text/plain",
        extensions = {"txt"}
)
public class TxtDocumentImporter implements DocumentImporter {

    /**
     * Reads a text file and creates an imported document.
     *
     * @param source text file to import
     * @return imported document data
     * @throws ImportException if the file cannot be read
     */
    @Override
    public ImportedDocument importDocument(Path source) throws ImportException {
        Objects.requireNonNull(source, "Source path must not be null");

        Path normalizedPath = source.toAbsolutePath().normalize();

        if (!Files.isRegularFile(normalizedPath)) {
            throw new ImportException(
                    "Source is not a regular file: " + normalizedPath
            );
        }

        if (!Files.isReadable(normalizedPath)) {
            throw new ImportException(
                    "Source file is not readable: " + normalizedPath
            );
        }

        try {
            String content = Files.readString(normalizedPath, StandardCharsets.UTF_8);

            return new ImportedDocument(
                    titleFrom(normalizedPath),
                    normalizedPath.toUri().toString(),
                    "text/plain",
                    content,
                    content
            );

        } catch (IOException exception) {
            throw new ImportException(
                    "Could not read file: " + normalizedPath,
                    exception
            );
        }
    }

    private static String titleFrom(Path source) {
        String fileName = source.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');

        return extensionStart > 0 ? fileName.substring(0, extensionStart) : fileName;
    }
}
