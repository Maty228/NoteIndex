package cz.martim12.noteindex.importer;

import cz.martim12.noteindex.importer.builtin.TxtDocumentImporter;
import cz.martim12.noteindex.importer.exception.UnsupportedFormatException;
import cz.martim12.noteindex.importer.registry.ImporterRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ImporterRegistryTest {

    private final ImporterRegistry registry = new ImporterRegistry(List.of(new TxtDocumentImporter()));

    @Test
    void resolvesImporterCaseInsensitively() {
        assertInstanceOf(TxtDocumentImporter.class, registry.resolve(Path.of("lexture.TXT")));
    }

    @Test
    void exposesSupportedExtensions() {
        assertTrue(registry.supportedExtensions().contains("txt"));
    }

    @Test
    void rejectsUnsupportedFormat() {
        assertThrows(UnsupportedFormatException.class, () -> registry.resolve(Path.of("lexture.pdf")));
    }
}
