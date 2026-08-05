package cz.martim12.noteindex.application.integration;

import cz.martim12.noteindex.application.api.NoteIndexApplications;
import cz.martim12.noteindex.application.api.NoteIndexService;
import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.SearchQuery;
import cz.martim12.noteindex.core.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteIndexApplicationIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsRebuildsSearchesAndDeletesAcrossRestarts()
            throws IOException {

        Path databaseFile =
                temporaryDirectory.resolve("noteindex.db");

        Path sourceFile =
                temporaryDirectory.resolve("java-runtime-notes.txt");

        String sourceContent = """
                Java virtual machine executes bytecode.
                Garbage collection manages runtime memory.
                SQLite stores persistent application data.
                """.strip();

        Files.writeString(
                sourceFile,
                sourceContent,
                StandardCharsets.UTF_8
        );

        long documentId;

        /*
         * First application run:
         * import the real TXT file, persist it and search it.
         */
        try (
                NoteIndexService service =
                        NoteIndexApplications.open(databaseFile)
        ) {
            assertTrue(
                    service.supportedImportExtensions()
                            .contains("txt")
            );

            assertTrue(service.listDocuments().isEmpty());

            Document imported =
                    service.importFile(sourceFile);

            documentId = imported.id();

            assertTrue(documentId > 0);
            assertEquals("text/plain", imported.format());

            assertEquals(
                    sourceContent,
                    imported.originalContent()
            );

            assertTrue(
                    imported.searchableContent()
                            .contains("virtual machine")
            );

            List<DocumentSummary> summaries =
                    service.listDocuments();

            assertEquals(1, summaries.size());
            assertEquals(
                    documentId,
                    summaries.getFirst().id()
            );

            Document loaded =
                    service.findDocument(documentId)
                            .orElseThrow();

            assertEquals(imported, loaded);

            List<SearchResult> results =
                    service.search(
                            new SearchQuery(
                                    "\"virtual machine\""
                            ),
                            10
                    );

            assertEquals(1, results.size());

            SearchResult result = results.getFirst();

            assertEquals(
                    documentId,
                    result.document().id()
            );

            assertTrue(result.score() > 0.0);

            assertTrue(
                    result.snippet()
                            .toLowerCase()
                            .contains("virtual machine")
            );
        }

        assertTrue(Files.exists(databaseFile));

        /*
         * Second application run:
         * no import occurs. The document must be loaded from
         * SQLite and placed into a newly created in-memory index.
         */
        try (
                NoteIndexService service =
                        NoteIndexApplications.open(databaseFile)
        ) {
            List<DocumentSummary> summaries =
                    service.listDocuments();

            assertEquals(1, summaries.size());
            assertEquals(
                    documentId,
                    summaries.getFirst().id()
            );

            Document restored =
                    service.findDocument(documentId)
                            .orElseThrow();

            assertEquals(
                    sourceContent,
                    restored.originalContent()
            );

            List<SearchResult> rebuiltIndexResults =
                    service.search(
                            new SearchQuery(
                                    "\"virtual machine\""
                            ),
                            10
                    );

            assertEquals(1, rebuiltIndexResults.size());

            assertEquals(
                    documentId,
                    rebuiltIndexResults
                            .getFirst()
                            .document()
                            .id()
            );

            assertTrue(
                    rebuiltIndexResults
                            .getFirst()
                            .snippet()
                            .toLowerCase()
                            .contains("virtual machine")
            );

            assertTrue(
                    service.deleteDocument(documentId)
            );

            assertFalse(
                    service.deleteDocument(documentId)
            );

            assertTrue(service.listDocuments().isEmpty());

            assertTrue(
                    service.findDocument(documentId)
                            .isEmpty()
            );

            assertTrue(
                    service.search(
                            new SearchQuery("java"),
                            10
                    ).isEmpty()
            );
        }

        /*
         * Third application run:
         * deletion must have persisted in SQLite, and rebuilding
         * the search index must not restore the document.
         */
        try (
                NoteIndexService service =
                        NoteIndexApplications.open(databaseFile)
        ) {
            assertTrue(service.listDocuments().isEmpty());

            assertTrue(
                    service.findDocument(documentId)
                            .isEmpty()
            );

            assertTrue(
                    service.search(
                            new SearchQuery(
                                    "\"virtual machine\""
                            ),
                            10
                    ).isEmpty()
            );
        }
    }
}