package cz.martim12.noteindex.cli.runtime;

import cz.martim12.noteindex.application.api.NoteIndexService;

import java.nio.file.Path;

/**
 * Opens the application service used by service-backed commands.
 * The abstraction allows CLI tests to provide a stub service
 * instead of opening a real SQLite database.
 */
@FunctionalInterface
public interface NoteIndexServiceFactory {

    /**
     * Opens an application service for the selected database.
     *
     * @param databaseFile database file location
     * @return opened NoteIndex service
     */
    NoteIndexService open(Path databaseFile);
}
