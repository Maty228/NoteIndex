package cz.martim12.noteindex.persistence;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.persistence.jdbc.DatabaseInitializer;
import cz.martim12.noteindex.persistence.jdbc.JdbcDocumentRepository;
import cz.martim12.noteindex.persistence.jdbc.SqliteConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitializerMigrationTest {

    private static final Instant IMPORT_TIME =
            Instant.parse("2026-08-03T00:00:00Z");

    private static final Clock FIXED_CLOCK =
            Clock.fixed(
                    IMPORT_TIME,
                    ZoneId.of("UTC")
            );

    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesExistingDatabaseAndPreservesDocuments() throws Exception {
        Path databaseFile =
                temporaryDirectory.resolve(
                        "noteindex-old.db"
                );

        SqliteConnectionFactory connectionFactory =
                new SqliteConnectionFactory(
                        databaseFile
                );

        createOldDatabase(connectionFactory);

        DatabaseInitializer initializer =
                new DatabaseInitializer(
                        connectionFactory
                );

        initializer.initialize();

        JdbcDocumentRepository repository =
                new JdbcDocumentRepository(
                        connectionFactory,
                        FIXED_CLOCK
                );

        Document existing =
                repository.findById(1)
                        .orElseThrow();

        assertEquals(
                "Legacy Note",
                existing.title()
        );

        assertEquals(
                "Legacy searchable content",
                existing.searchableContent()
        );

        assertTrue(
                repository.updateDisplayTitle(
                        existing.id(),
                        "Renamed Legacy Note"
                )
        );

        assertEquals(
                "Renamed Legacy Note",
                repository.findById(
                                existing.id()
                        )
                        .orElseThrow()
                        .title()
        );

        initializer.initialize();

        assertEquals(
                "Renamed Legacy Note",
                repository.findById(
                                existing.id()
                        )
                        .orElseThrow()
                        .title()
        );
    }

    private static void createOldDatabase(
            SqliteConnectionFactory connectionFactory
    ) throws Exception {
        try (
                Connection connection =
                        connectionFactory.openConnection();

                Statement statement =
                        connection.createStatement()
        ) {
            statement.executeUpdate(
                    """
                    CREATE TABLE documents (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        source_uri TEXT NOT NULL UNIQUE,
                        format_id TEXT NOT NULL,
                        original_content TEXT NOT NULL,
                        searchable_content TEXT NOT NULL,
                        imported_at TEXT NOT NULL
                    )
                    """
            );

            statement.executeUpdate(
                    """
                    INSERT INTO documents (
                        title,
                        source_uri,
                        format_id,
                        original_content,
                        searchable_content,
                        imported_at
                    )
                    VALUES (
                        'Legacy Note',
                        'file:///notes/legacy.txt',
                        'text/plain',
                        'Legacy original content',
                        'Legacy searchable content',
                        '2026-08-03T00:00:00Z'
                    )
                    """
            );
        }
    }
}