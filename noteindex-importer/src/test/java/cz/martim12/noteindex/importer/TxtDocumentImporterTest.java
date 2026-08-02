package cz.martim12.noteindex.importer;

import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.importer.builtin.TxtDocumentImporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TxtDocumentImporterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void importsUtf8TextFile() throws Exception {
        Path source = temporaryDirectory.resolve("algorithms.txt");
        String text = "Binary trees and graphs.";
        Files.writeString(source, text, StandardCharsets.UTF_8);

        ImportedDocument document = new TxtDocumentImporter().importDocument(source);

        assertEquals("algorithms", document.title());
        assertEquals("text/plain", document.format());
        assertEquals(text, document.originalContent());
        assertEquals(document.originalContent(), document.searchableContent());
    }
}
