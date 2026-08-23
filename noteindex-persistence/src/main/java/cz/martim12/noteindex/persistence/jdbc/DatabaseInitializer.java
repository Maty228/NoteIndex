package cz.martim12.noteindex.persistence.jdbc;

import cz.martim12.noteindex.persistence.exception.RepositoryException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.sql.ResultSet;

/**
 * Initializes and migrates the SQLite database schema used by NoteIndex.
 *
 * <p>The initializer creates required tables and indexes and applies
 * supported schema migrations when necessary.</p>
 */
public final class DatabaseInitializer {
    private static final String CREATE_DOCUMENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS documents (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            title TEXT NOT NULL,
                            source_uri TEXT NOT NULL UNIQUE,
                            format_id TEXT NOT NULL,
                            original_content TEXT NOT NULL,
                            searchable_content TEXT NOT NULL,
                            imported_at TEXT NOT NULL
                        )
            """;

    private static final String ADD_DISPLAY_TITLE_COLUMN = """
        ALTER TABLE documents
        ADD COLUMN display_title TEXT
        """;

    private static final String CREATE_TITLE_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_documents_title
            ON documents(title)
            """;

    private static final String CREATE_IMPORTED_AT_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_documents_imported_at
            ON documents(imported_at)
            """;

    private final SqliteConnectionFactory connectionFactory;

    /**
     * Creates a database initializer.
     *
     * @param connectionFactory factory used to obtain database connections
     */
    public DatabaseInitializer(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "Connection factory must not be null");
    }

    /**
     * Creates or updates the database schema.
     *
     * @throws RepositoryException if initialization fails
     */
    public void initialize() {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);

            try {
                createDocumentsTable(connection);
                migrateDocumentsTable(connection);
                createIndexes(connection);

                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }

        } catch (SQLException exception) {
            throw new RepositoryException(
                    "Could not initialize the database",
                    exception
            );
        }
    }

    private void createDocumentsTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_DOCUMENTS_TABLE);
        }
    }

    private void migrateDocumentsTable(Connection connection) throws SQLException {
        if (hasColumn(connection, "documents", "display_title")) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ADD_DISPLAY_TITLE_COLUMN);
        }
    }

    private void createIndexes(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_TITLE_INDEX);
            statement.executeUpdate(CREATE_IMPORTED_AT_INDEX);
        }
    }

    private static boolean hasColumn(
            Connection connection,
            String table,
            String column
    ) throws SQLException {

        try (
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "PRAGMA table_info(" + table + ")"
                )
        ) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }

            return false;
        }
    }

    private static void rollback(Connection connection, SQLException exception) {
        try {
            connection.rollback();
        } catch(SQLException rollbackException) {
            exception.addSuppressed(rollbackException);
        }
    }
}
