package cz.martim12.noteindex.persistence.jdbc;

import cz.martim12.noteindex.persistence.exception.RepositoryException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

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

    private static final String CREATE_TITLE_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_documents_title
            ON documents(title)
            """;

    private static final String CREATE_IMPORTED_AT_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_documents_imported_at
            ON documents(imported_at)
            """;

    private final SqliteConnectionFactory connectionFactory;

    public DatabaseInitializer(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "Connection factory must not be null");
    }

    public void initialize() {
        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()){
                statement.executeUpdate(CREATE_DOCUMENTS_TABLE);
                statement.executeUpdate(CREATE_TITLE_INDEX);
                statement.executeUpdate(CREATE_IMPORTED_AT_INDEX);

                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch(SQLException exception) {
            throw new RepositoryException(
                    "Could not initialize the database",
                    exception
            );
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
