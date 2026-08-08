package cz.martim12.noteindex.application.api;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.SearchQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentRenameIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void renamePersistsAndUpdatesSearchIndexAcrossRestart() throws Exception {
        Path databaseFile =
                temporaryDirectory.resolve("noteindex.db");

        Path source =
                temporaryDirectory.resolve("lecture.txt");

        Files.writeString(
                source,
                """
                Ordinary study notes.

                This body deliberately does not contain the
                custom title search term.
                """
        );

        long documentId;

        try (NoteIndexService service =
                     NoteIndexApplications.open(databaseFile)) {

            Document imported =
                    service.importFile(source);

            documentId = imported.id();

            assertTrue(
                    service.renameDocument(
                            documentId,
                            "Quantum Rename Probe"
                    )
            );

            Document renamed =
                    service.findDocument(documentId)
                            .orElseThrow();

            assertEquals(
                    "Quantum Rename Probe",
                    renamed.title()
            );

            assertFalse(
                    service.search(
                            new SearchQuery("quantum"),
                            10
                    ).isEmpty()
            );
        }

        try (NoteIndexService service =
                     NoteIndexApplications.open(databaseFile)) {

            Document renamed =
                    service.findDocument(documentId)
                            .orElseThrow();

            assertEquals(
                    "Quantum Rename Probe",
                    renamed.title()
            );

            assertFalse(
                    service.search(
                            new SearchQuery("quantum"),
                            10
                    ).isEmpty()
            );
        }
    }
}