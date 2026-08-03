package cz.martim12.noteindex.persistence;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.persistence.exception.DuplicateDocumentException;
import cz.martim12.noteindex.persistence.jdbc.DatabaseInitializer;
import cz.martim12.noteindex.persistence.jdbc.JdbcDocumentRepository;
import cz.martim12.noteindex.persistence.jdbc.SqliteConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JdbcDocumentRepositoryTest {

    private static final Instant IMPORT_TIME = Instant.parse("2026-08-03T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(IMPORT_TIME, ZoneId.of("UTC"));

    @TempDir
    Path temporaryDirectory;

    private JdbcDocumentRepository repository;

    @BeforeEach
    void setUp() {
        Path databaseFile = temporaryDirectory.resolve("noteindex-test.db");

        SqliteConnectionFactory connectionFactory = new SqliteConnectionFactory(databaseFile);
        new DatabaseInitializer(connectionFactory).initialize();

        repository = new JdbcDocumentRepository(connectionFactory, FIXED_CLOCK);
    }

    @Test
    void savesAndLoadsDocument() {
        ImportedDocument imported = createDocument(
                "file:///notes/algorithms.txt",
                "Algorithms"
        );

        Document saved = repository.save(imported);

        assertTrue(saved.id() > 0);
        assertEquals(IMPORT_TIME, saved.importedAt());
        assertEquals(imported.title(), saved.title());
        assertEquals(imported.sourceUri(), saved.sourceUri());

        Document loaded = repository.findById(saved.id()).orElseThrow();

        assertEquals(saved, loaded);
    }

    @Test
    void listsDocumentSummaries() {
        Document first = repository.save(
                createDocument(
                        "file:///notes/first.txt",
                        "First"
                )
        );

        Document second = repository.save(
                createDocument(
                        "file:///notes/second.txt",
                        "Second"
                )
        );

        List<DocumentSummary> summaries =
                repository.findAllSummaries();

        assertEquals(2, summaries.size());

        // Same timestamp, so the document with the newer ID is first.
        assertEquals(second.id(), summaries.getFirst().id());
        assertEquals(first.id(), summaries.getLast().id());
    }

    @Test
    void rejectsDuplicateSourceUri() {
        ImportedDocument document = createDocument(
                "file:///notes/java.txt",
                "Java"
        );

        repository.save(document);

        assertThrows(
                DuplicateDocumentException.class,
                () -> repository.save(document)
        );
    }

    @Test
    void checksWhetherSourceExists() {
        String sourceUri = "file:///notes/databases.txt";

        assertFalse(repository.existsBySourceUri(sourceUri));

        repository.save(createDocument(sourceUri, "Databases"));

        assertTrue(repository.existsBySourceUri(sourceUri));
    }

    @Test
    void deletesDocument() {
        Document saved = repository.save(createDocument("file://notes/networks.text", "Networks"));

        assertTrue(repository.deleteById(saved.id()));
        assertTrue(repository.findById(saved.id()).isEmpty());

        assertFalse(repository.deleteById(saved.id()));
    }

    private static ImportedDocument createDocument(String sourceUri, String title) {
        return new ImportedDocument(
                title,
                sourceUri,
                "text/plain",
                "Original content for " + title,
                "Original content for " + title
        );
    }
}
