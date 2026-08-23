package cz.martim12.noteindex.importer.builtin;

import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.importer.annotation.ImporterPlugin;
import cz.martim12.noteindex.importer.api.DocumentImporter;
import cz.martim12.noteindex.importer.exception.ImportException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Imports Markdown files into NoteIndex documents.
 */
@ImporterPlugin(
        name = "Markdown Importer",
        formatId = "text/markdown",
        extensions = {"md", "markdown"}
)
public final class MarkdownDocumentImporter implements DocumentImporter {

    /**
     * Reads and converts a Markdown file into an imported document.
     *
     * @param source Markdown file to import
     * @return imported document data
     * @throws ImportException if the file cannot be read or processed
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
            String originalContent = Files.readString(normalizedPath, StandardCharsets.UTF_8);

            MarkdownContentProcessor.ProcessedMarkdown processed =
                    MarkdownContentProcessor.process(originalContent);

            String title = processed.title()
                    .orElseGet(
                            () -> titleFrom(normalizedPath)
                    );

            return new ImportedDocument(title, normalizedPath.toUri().toString(), "text/markdown", originalContent, processed.searchableContent());

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
