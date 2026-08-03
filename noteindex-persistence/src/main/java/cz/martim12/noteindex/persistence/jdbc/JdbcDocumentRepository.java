package cz.martim12.noteindex.persistence.jdbc;

import cz.martim12.noteindex.core.model.Document;
import cz.martim12.noteindex.core.model.DocumentSummary;
import cz.martim12.noteindex.core.model.ImportedDocument;
import cz.martim12.noteindex.persistence.api.DocumentRepository;
import cz.martim12.noteindex.persistence.exception.DuplicateDocumentException;
import cz.martim12.noteindex.persistence.exception.RepositoryException;

import java.sql.*;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JdbcDocumentRepository implements DocumentRepository {
    private static final String INSERT_DOCUMENT = """
            INSERT INTO documents (
                title,
                source_uri,
                format_id,
                original_content,
                searchable_content,
                imported_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT
                id,
                title,
                source_uri,
                format_id,
                original_content,
                searchable_content,
                imported_at
            FROM documents
            WHERE id = ?
            """;

    private static final String SELECT_ALL = """
            SELECT
                id,
                title,
                source_uri,
                format_id,
                original_content,
                searchable_content,
                imported_at
            FROM documents
            ORDER BY imported_at DESC, id DESC
            """;

    private static final String SELECT_ALL_SUMMARIES = """
            SELECT
                id,
                title,
                format_id,
                imported_at
            FROM documents
            ORDER BY imported_at DESC, id DESC
            """;

    private static final String EXISTS_BY_SOURCE_URI = """
            SELECT 1
            FROM documents
            WHERE source_uri = ?
            LIMIT 1
            """;

    private static final String DELETE_BY_ID = """
            DELETE FROM documents
            WHERE id = ?
            """;

    private final SqliteConnectionFactory connectionFactory;
    private final Clock clock;

    public JdbcDocumentRepository(SqliteConnectionFactory connectionFactory) {
        this(connectionFactory, Clock.systemUTC());
    }
    public JdbcDocumentRepository(SqliteConnectionFactory connectionFactory, Clock clock) {
        this.connectionFactory = Objects.requireNonNull(
                connectionFactory,
                "Connection factory must not be null"
        );

        this.clock = Objects.requireNonNull(
                clock,
                "Clock must not be null"
        );
    }

    @Override
    public Document save(ImportedDocument document) {
        validateDocument(document);

        Instant importedAt = clock.instant();

        try (Connection connection = connectionFactory.openConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(INSERT_DOCUMENT, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, document.title());
                statement.setString(2, document.sourceUri());
                statement.setString(3, document.format());
                statement.setString(4, document.originalContent());
                statement.setString(5, document.searchableContent());
                statement.setString(6, importedAt.toString());

                int affectedRows = statement.executeUpdate();

                if (affectedRows != 1) {
                    throw new SQLException(
                            "Expected one inserted row, but got "
                                    + affectedRows
                    );
                }

                long generatedId = readGeneratedId(statement);

                connection.commit();

                return new Document(
                        generatedId,
                        document.title(),
                        document.sourceUri(),
                        document.format(),
                        document.originalContent(),
                        document.searchableContent(),
                        importedAt
                );
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            if (isDuplicateSourceException(exception)) {
                throw new DuplicateDocumentException(
                        document.sourceUri(),
                        exception
                );
            }

            throw new RepositoryException(
                    "Could not save document: " + document.title(),
                    exception
            );
        }
    }

    @Override
    public Optional<Document> findById(long id){
        requirePositiveId(id);

        try (
                Connection connection = connectionFactory.openConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)
        ) {
            statement.setLong(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapDocument(resultSet));
            }
        } catch (SQLException exception) {
            throw new RepositoryException(
                    "Could not load document with ID " + id,
                    exception
            );
        }
    }

    @Override
    public List<Document> findAll() {
        List<Document> documents = new ArrayList<>();

        try (
                Connection connection = connectionFactory.openConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
                ResultSet resultSet = statement.executeQuery()
                ) {
            while (resultSet.next()) {
                documents.add(mapDocument(resultSet));
            }

            return List.copyOf(documents);
        } catch (SQLException exception) {
            throw new RepositoryException(
                    "Could not load documents",
                    exception
            );
        }
    }

    @Override
    public List<DocumentSummary> findAllSummaries() {
        List<DocumentSummary> summaries = new ArrayList<>();

        try (
                Connection connection = connectionFactory.openConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_ALL_SUMMARIES);
                ResultSet resultSet = statement.executeQuery()
                ) {
            while (resultSet.next()) {
                summaries.add(mapSummary(resultSet));
            }

            return List.copyOf(summaries);
        } catch (SQLException exception) {
            throw new RepositoryException(
                    "Could not load document summaries",
                    exception
            );
        }
    }

    @Override
    public boolean existsBySourceUri(String sourceUri) {
        requireNonBlank(sourceUri, "Source URI");

        try (
                Connection connection = connectionFactory.openConnection();
                PreparedStatement statement = connection.prepareStatement(EXISTS_BY_SOURCE_URI)
        ) {
            statement.setString(1, sourceUri);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException exception) {
            throw new RepositoryException(
                    "Could not check document source: " + sourceUri,
                    exception
            );
        }
    }

    @Override
    public boolean deleteById(long id) {
        requirePositiveId(id);

        try (
                Connection connection = connectionFactory.openConnection();
                PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID)
                ) {
            statement.setLong(1, id);

            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new RepositoryException(
                    "Could not delete document with ID " + id,
                    exception
            );
        }
    }

    private static long readGeneratedId(PreparedStatement statement) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            if (!generatedKeys.next()) {
                throw new SQLException(
                        "Database did not return a generated document ID"
                );
            }
            return generatedKeys.getLong(1);
        }
    }

    private static Document mapDocument(ResultSet resultSet) throws SQLException {
        return new Document(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("source_uri"),
                resultSet.getString("format_id"),
                resultSet.getString("original_content"),
                resultSet.getString("searchable_content"),
                Instant.parse(resultSet.getString("imported_at"))
        );
    }

    private static DocumentSummary mapSummary(ResultSet resultSet) throws SQLException {
        return new DocumentSummary(
                resultSet.getLong("id"),
                resultSet.getString("title"),
                resultSet.getString("format_id"),
                Instant.parse(resultSet.getString("imported_at"))
        );
    }

    private static void validateDocument(ImportedDocument document) {
        Objects.requireNonNull(document, "Imported document must not be null");

        requireNonBlank(document.title(), "Title");
        requireNonBlank(document.sourceUri(), "Source URI");
        requireNonBlank(document.format(), "Format");

        Objects.requireNonNull(document.originalContent(), "Original content must not be null");

        Objects.requireNonNull(document.searchableContent(), "Searchable content must not be null");
    }

    private static void requirePositiveId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Document ID must be positive"
            );
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
    }

    private static boolean isDuplicateSourceException(SQLException exception) {
        for (SQLException current = exception; current != null; current = current.getNextException()) {
            String message = current.getMessage();
            if (current.getErrorCode() == 19 && message != null && message.contains("documents.source_uri")) {
                return true;
            }

        }
        return false;
    }

    private static void rollback(Connection connection, SQLException exception) {
        try {
            connection.rollback();
        } catch(SQLException rollbackException) {
            exception.addSuppressed(rollbackException);
        }
    }
}
