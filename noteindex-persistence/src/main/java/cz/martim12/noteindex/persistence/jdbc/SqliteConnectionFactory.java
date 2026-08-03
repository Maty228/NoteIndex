package cz.martim12.noteindex.persistence.jdbc;

import cz.martim12.noteindex.persistence.exception.RepositoryException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class SqliteConnectionFactory {
    private final Path databaseFile;

    public SqliteConnectionFactory(Path databaseFile) {
        this.databaseFile = Objects.requireNonNull(databaseFile,"Database file must not be null")
                .toAbsolutePath().normalize();

    }

    public Connection openConnection(){
        createParentDirectory();

        Connection connection = null;

        try {
            String jdbcUrl = "jdbc:sqlite:" + databaseFile;
            connection = DriverManager.getConnection(jdbcUrl);

            configureConnection(connection);

            return connection;
        } catch(SQLException exception) {
            closeAfterFailure(connection, exception);

            throw new RepositoryException(
                    "Could not open SQLite database: " + databaseFile,
                    exception
            );
        }
    }

    public Path databaseFile() {
        return databaseFile;
    }

    private void createParentDirectory() {
        Path parent = databaseFile.getParent();

        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException exception) {
            throw new RepositoryException(
                    "Could not create database directory: " + parent,
                    exception
            );
        }
    }

    private static void configureConnection(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");

        }
    }

    private static void closeAfterFailure(Connection connection, SQLException exception) {
        if (connection == null) {
            return;
        }


        try {
            connection.close();
        } catch (SQLException closeException) {
            exception.addSuppressed(closeException);
        }
    }
}
