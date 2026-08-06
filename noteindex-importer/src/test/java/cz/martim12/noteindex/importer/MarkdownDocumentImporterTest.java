package cz.martim12.noteindex.importer;

import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.importer.builtin.MarkdownDocumentImporter;
import cz.martim12.noteindex.importer.exception.ImportException;
import cz.martim12.noteindex.importer.registry.ImporterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownDocumentImporterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void importsRichMarkdownAndProducesSearchableText()
            throws Exception {

        Path source = temporaryDirectory.resolve(
                "java-memory.md"
        );

        String markdown = """
                # Java **Memory** Management

                ![Heap diagram](images/heap.png)

                <img src="object-graph.png" alt="Object graph">

                ![[memory-layout.png|Memory layout]]

                The **heap** stores [objects](https://example.com/objects).
                Inline code: `Object value`.

                $$
                E = mc^2
                $$

                \\[
                T(n) = \\Theta(n \\log n)
                \\]

                > Important runtime concept

                - [x] Review garbage collection

                ```java
                Object value = new Object();
                ```
                """.strip();

        Files.writeString(
                source,
                markdown,
                StandardCharsets.UTF_8
        );

        ImportedDocument document =
                new MarkdownDocumentImporter()
                        .importDocument(source);

        assertEquals(
                "Java Memory Management",
                document.title()
        );

        assertEquals(
                "text/markdown",
                document.format()
        );

        assertEquals(
                source.toAbsolutePath()
                        .normalize()
                        .toUri()
                        .toString(),
                document.sourceUri()
        );

        assertEquals(
                markdown,
                document.originalContent()
        );

        String searchable =
                document.searchableContent();

        assertTrue(
                searchable.contains(
                        "Java Memory Management"
                )
        );

        assertTrue(
                searchable.contains("Heap diagram")
        );

        assertTrue(
                searchable.contains("Object graph")
        );


        assertTrue(
                searchable.contains("Memory layout")
        );

        assertTrue(
                searchable.contains(
                        "The heap stores objects."
                )
        );

        assertTrue(
                searchable.contains(
                        "Inline code: Object value."
                )
        );

        assertTrue(
                searchable.contains("E = mc^2")
        );

        assertTrue(
                searchable.contains(
                        "T(n) = \\Theta(n \\log n)"
                )
        );

        assertTrue(
                searchable.contains(
                        "Important runtime concept"
                )
        );

        assertTrue(
                searchable.contains(
                        "Review garbage collection"
                )
        );

        assertTrue(
                searchable.contains(
                        "Object value = new Object();"
                )
        );

        assertFalse(
                searchable.contains(
                        "images/heap.png"
                )
        );

        assertFalse(
                searchable.contains(
                        "object-graph.png"
                )
        );

        assertFalse(
                searchable.contains(
                        "memory-layout.png"
                )
        );

        assertFalse(
                searchable.contains(
                        "https://example.com"
                )
        );

        assertFalse(searchable.contains("**"));
        assertFalse(searchable.contains("!["));
        assertFalse(searchable.contains("<img"));
        assertFalse(searchable.contains("```"));
        assertFalse(searchable.contains("$$"));
    }

    @Test
    void usesSetextLevelOneHeadingAsTitle()
            throws Exception {

        Path source = temporaryDirectory.resolve(
                "algorithms.markdown"
        );

        String markdown = """
                Algorithms and Data Structures
                ==============================

                Trees, graphs and hash tables.
                """.strip();

        Files.writeString(
                source,
                markdown,
                StandardCharsets.UTF_8
        );

        ImportedDocument document =
                new MarkdownDocumentImporter()
                        .importDocument(source);

        assertEquals(
                "Algorithms and Data Structures",
                document.title()
        );

        assertTrue(
                document.searchableContent().contains(
                        "Algorithms and Data Structures"
                )
        );

        assertFalse(
                document.searchableContent().contains(
                        "=============================="
                )
        );
    }

    @Test
    void fallsBackToFilenameWhenDocumentHasNoTitle()
            throws Exception {

        Path source = temporaryDirectory.resolve(
                "distributed-systems.md"
        );

        Files.writeString(
                source,
                "Consensus and replication.",
                StandardCharsets.UTF_8
        );

        ImportedDocument document =
                new MarkdownDocumentImporter()
                        .importDocument(source);

        assertEquals(
                "distributed-systems",
                document.title()
        );
    }

    @Test
    void discoversMarkdownExtensionsThroughServiceLoader() {
        ImporterRegistry registry =
                ImporterRegistry.discover();

        assertTrue(
                registry.supportedExtensions()
                        .containsAll(
                                Set.of(
                                        "txt",
                                        "md",
                                        "markdown"
                                )
                        )
        );

        assertInstanceOf(
                MarkdownDocumentImporter.class,
                registry.resolve(
                        Path.of("NOTES.MD")
                )
        );

        assertInstanceOf(
                MarkdownDocumentImporter.class,
                registry.resolve(
                        Path.of("notes.markdown")
                )
        );
    }

    @Test
    void rejectsMissingMarkdownFile() {
        Path missing =
                temporaryDirectory.resolve(
                        "missing.md"
                );

        assertThrows(
                ImportException.class,
                () -> new MarkdownDocumentImporter()
                        .importDocument(missing)
        );
    }
}